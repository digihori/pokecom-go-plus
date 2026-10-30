import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.gradle.process.ExecOperations
import javax.inject.Inject

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":playerShared"))
    implementation(project(":hostInput"))
    implementation(project(":controllerUi"))
    implementation(platform("org.lwjgl:lwjgl-bom:3.4.1"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-sdl")
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch") in listOf("aarch64", "arm64")
    val lwjglNatives = when {
        os.contains("mac") -> if (arm) "natives-macos-arm64" else "natives-macos"
        os.contains("win") -> if (arm) "natives-windows-arm64" else "natives-windows"
        else -> if (arm) "natives-linux-arm64" else "natives-linux"
    }
    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-sdl::$lwjglNatives")
    implementation(libs.kotlinx.serialization.json)
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    testImplementation(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
}

val prepareDistributionResources by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.file("LICENSE"))
    from(rootProject.layout.projectDirectory.file("THIRD_PARTY_NOTICES.md"))
    from(rootProject.layout.projectDirectory.dir("docs/licenses")) { into("licenses") }
    into(layout.buildDirectory.dir("generated/distribution-resources/common"))
}

tasks.matching { it.name == "prepareAppResources" }.configureEach {
    dependsOn(prepareDistributionResources)
}

compose.desktop {
    application {
        mainClass = "com.digihori.pgp.desktop.MainKt"

        nativeDistributions {
            // LWJGL uses sun.misc.Unsafe; jdeps cannot see this reflective dependency.
            // The local MCP server needs HttpServer in the packaged runtime too.
            modules("jdk.unsupported", "jdk.httpserver")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PokecomGOStudio"
            // jpackage requires 1-3 numeric components and macOS rejects a zero major version.
            // The public project/Git version is 1.0.0; native versions increase independently.
            packageVersion = "1.6.0"
            description = "A multiplatform pocket-computer development environment"
            vendor = "Y Horiuchi"
            copyright = "Copyright (c) 2026 Y Horiuchi"
            appResourcesRootDir.set(layout.buildDirectory.dir("generated/distribution-resources"))
            macOS {
                iconFile.set(project.file("icons/pokecom-go.icns"))
                // Keep the existing bundle identity. Signing is opt-in on the build host;
                // certificates and account details do not belong in the repository.
                bundleID = "com.digihori.pgp.desktop"
                appCategory = "public.app-category.developer-tools"
                signing {
                    providers.environmentVariable("PGP_MAC_SIGNING_IDENTITY").orNull?.let {
                        sign.set(true)
                        identity.set(it)
                    }
                    providers.environmentVariable("PGP_MAC_SIGNING_KEYCHAIN").orNull?.let {
                        keychain.set(it)
                    }
                }
            }
            linux {
                menuGroup = "Development"
                iconFile.set(project.file("icons/pokecom-go.png"))
            }
            windows {
                iconFile.set(project.file("icons/pokecom-go.ico"))
                // Preserve the UpgradeCode from the published 0.4 MSI (ProductVersion 1.3.0).
                upgradeUuid = "D173EFD1-57EF-3753-A618-92481F262D95"
                menu = true
                menuGroup = "Pokecom GO Studio"
            }
        }
    }
}

abstract class DmgIconOperations @Inject constructor(val execOperations: ExecOperations)
val dmgIconOperations = objects.newInstance<DmgIconOperations>()
tasks.withType<AbstractJPackageTask>().matching { it.targetFormat == TargetFormat.Dmg }.configureEach {
    val execOperations = dmgIconOperations.execOperations
    val volumeIcon = project.layout.projectDirectory.file("icons/pokecom-go-dmg.icns")
    val iconScript = rootProject.layout.projectDirectory.file("scripts/set-dmg-volume-icon.sh")
    val macSigning = compose.desktop.application.nativeDistributions.macOS.signing
    val signFinalDmg = macSigning.sign.getOrElse(false)
    val finalDmgIdentity = macSigning.identity.orNull
    inputs.files(volumeIcon, iconScript).withPropertyName("studioDmgVolumeIcon")
    inputs.property("studioDmgSigned", signFinalDmg)
    inputs.property("studioDmgSigningIdentity", finalDmgIdentity.orEmpty())
    doLast {
        val image = destinationDir.file("${packageName.get()}-${packageVersion.get()}.dmg").get().asFile
        execOperations.exec {
            commandLine("/bin/sh", iconScript.asFile, image, volumeIcon.asFile)
        }
        // The icon operation rebuilds the image, so sign the final DMG afterwards.
        if (signFinalDmg) {
            execOperations.exec {
                commandLine("/usr/bin/codesign", "--force", "--timestamp", "--sign", requireNotNull(finalDmgIdentity), image)
            }
            execOperations.exec {
                commandLine("/usr/bin/codesign", "--verify", "--strict", "--verbose=2", image)
            }
        }
    }
}

// Isolated visual verification; never starts Studio preferences, ROM library, or SDL.
val verifyDistributionRuntime by tasks.registering {
    group = "verification"
    description = "Verify required Java modules in the actual distributable runtime"
    dependsOn("createDistributable")
    val runtimeReleases = fileTree(layout.buildDirectory.dir("compose/binaries/main/app")) {
        include("**/runtime/**/release")
    }
    inputs.files(runtimeReleases)
    doLast {
        val releases = runtimeReleases.files
        check(releases.size == 1) { "Expected one packaged Java runtime, found ${releases.size}" }
        val modulesLine = releases.single().readLines().single { it.startsWith("MODULES=") }
        val modules = modulesLine.substringAfter('=').trim('"').split(' ').toSet()
        val required = setOf("jdk.unsupported", "jdk.httpserver")
        check(modules.containsAll(required)) { "Packaged runtime is missing Java modules: ${required - modules}" }
        logger.lifecycle("Packaged Java runtime includes required modules: $required")
    }
}

tasks.register<JavaExec>("localizationPreview") {
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.digihori.pgp.desktop.localization.LocalizationPreviewKt")
}
