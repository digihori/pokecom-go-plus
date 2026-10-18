import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        named("desktopTest").dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

android {
    namespace = "com.digihori.pgp.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}

// Verify the common runtime version against the actual Gradle release version.
tasks.withType<Test>().configureEach {
    systemProperty("pgp.projectVersion", rootProject.version.toString())
}
