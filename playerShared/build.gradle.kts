import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
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

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "PgpPlayer"
            isStatic = true
            export(project(":core"))
            export(project(":hostInput"))
            binaryOption("bundleId", "com.digihori.pgp.player.shared")
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            api(project(":hostInput"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.digihori.pgp.player.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}
