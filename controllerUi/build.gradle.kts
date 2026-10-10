import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }
    sourceSets {
        named("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
        }
        commonMain.dependencies {
            api(project(":hostInput"))
            // Keep Android-compatible common artifacts for compileSdk 36 / AGP 8.11.
            // Desktop overrides these with the current Compose desktop runtime.
            implementation("org.jetbrains.compose.runtime:runtime:1.8.2")
            implementation("org.jetbrains.compose.foundation:foundation:1.8.2")
            implementation("org.jetbrains.compose.material3:material3:1.8.2")
        }
    }
}

android {
    namespace = "com.digihori.pgp.controller.ui"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}
