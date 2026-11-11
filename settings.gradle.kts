pluginManagement {
    // AGP 8.11 bundles an R8 version that cannot parse Kotlin 2.4 metadata.
    // Keep AGP unchanged and use a Kotlin 2.4-compatible stable R8.
    buildscript {
        repositories {
            google()
            mavenCentral()
        }
        dependencies {
            classpath("com.android.tools:r8:9.1.57")
        }
    }
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pokecom-go-plus"

include(":core")
include(":playerShared")
include(":desktopApp")
include(":androidApp")
include(":hostInput")
include(":controllerUi")
