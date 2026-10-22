import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            modules("jdk.unsupported")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PokecomGOStudio"
            // jpackage requires 1-3 numeric components and macOS rejects a zero major version.
            // The project/Git version remains 0.6.0-alpha.2.
            packageVersion = "1.4.1"
            description = "A multiplatform pocket-computer development environment"
            vendor = "Y Horiuchi"
            copyright = "Copyright (c) 2026 Y Horiuchi"
            appResourcesRootDir.set(layout.buildDirectory.dir("generated/distribution-resources"))
            macOS {
                iconFile.set(project.file("icons/pokecom-go.icns"))
            }
            linux {
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

// Isolated visual verification; never starts Studio preferences, ROM library, or SDL.
tasks.register<JavaExec>("localizationPreview") {
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.digihori.pgp.desktop.localization.LocalizationPreviewKt")
}
