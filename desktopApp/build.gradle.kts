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
    implementation(libs.kotlinx.serialization.json)
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    testImplementation(kotlin("test"))
}

val prepareDistributionResources by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.file("LICENSE"))
    from(rootProject.layout.projectDirectory.file("THIRD_PARTY_NOTICES.md"))
    into(layout.buildDirectory.dir("generated/distribution-resources/common"))
}

tasks.matching { it.name == "prepareAppResources" }.configureEach {
    dependsOn(prepareDistributionResources)
}

compose.desktop {
    application {
        mainClass = "com.digihori.pgp.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PokecomGOStudio"
            // jpackage requires 1-3 numeric components and macOS rejects a zero major version.
            // The project/Git version remains 0.3.0-alpha.1.
            packageVersion = "1.2.0"
            description = "A multiplatform pocket-computer development environment"
            vendor = "Y Horiuchi"
            copyright = "Copyright (c) 2026 Y Horiuchi"
            appResourcesRootDir.set(layout.buildDirectory.dir("generated/distribution-resources"))
        }
    }
}
