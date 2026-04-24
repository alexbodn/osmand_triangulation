pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        ivy {
            name = "OsmAndBinariesIvy"
            url = uri("https://builder.osmand.net")
            patternLayout {
                artifact("ivy/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]")
            }
        }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "GeolocationTriangulation"
include(":app")
