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

rootProject.name = "WIFIAP"
//include(":app")
//include(":usbp2p")
include(":screen")
//include(":scrcpy")
include(":AServer")
include(":BClient")
include(":CClient")
