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

rootProject.name = "A2A"
//include(":app")
//include(":usbp2p")
//include(":screen")
//include(":scrcpy")
//include(":AServer")
//include(":BClient")
//include(":Simplesshd")
//include(":wenxiaomao1023_scrcpy")
include(":web_screen")
include(":webrtc_lib")
include(":webrtc_android")
include(":webrtc_web")


include(":SystemLib")
project(":SystemLib").projectDir = File("D:\\repository\\SystemFunction\\SystemFunction_src\\SystemLib")

include(":Android12")
project(":Android12").projectDir = File("D:\\repository\\SystemFunction\\SystemFunction_src\\Android12")

include(":Android13")
project(":Android13").projectDir = File("D:\\repository\\SystemFunction\\SystemFunction_src\\Android13")

include(":Android14")
project(":Android14").projectDir = File("D:\\repository\\SystemFunction\\SystemFunction_src\\Android14")