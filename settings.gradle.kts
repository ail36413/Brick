pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Brick"

include(":app")
include(":brick-net")
include(":brick-utils")
include(":brick-ui")
include(":brick-image")
include(":brick-arch")
include(":brick-store")
include(":brick-log")
include(":brick-data")
include(":brick-permission")
include(":brick-startup")
