import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val preparePlayerLegalResources by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.file("LICENSE"))
    from(rootProject.layout.projectDirectory.file("THIRD_PARTY_NOTICES.md"))
    into(layout.buildDirectory.dir("generated/player-legal-assets"))
}

android {
    namespace = "com.digihori.pgp.player.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.digihori.pgp.player"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0-alpha.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.getByName("main").assets.srcDir(preparePlayerLegalResources)
}

tasks.named("preBuild").configure {
    dependsOn(preparePlayerLegalResources)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":playerShared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}
