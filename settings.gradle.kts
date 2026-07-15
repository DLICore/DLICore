pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://jitpack.io") }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.papermc.paper.plugin") {
                useModule("io.papermc:paper-gradle-plugin:2.1.1")
            }
        }
    }
}

rootProject.name = "DLIAssets"