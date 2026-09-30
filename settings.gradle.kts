pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mapbox Maps SDK (Remote-Config-gated alternate delivery-tracking map
        // renderer — see app/build.gradle.kts). Requires a secret downloads
        // token (scope DOWNLOADS:READ) in the developer's own
        // ~/.gradle/gradle.properties as MAPBOX_DOWNLOADS_TOKEN — never commit
        // this token. Without it the dependency simply fails to resolve.
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication { create<BasicAuthentication>("basic") }
            credentials {
                username = "mapbox"
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").getOrElse("")
            }
        }
    }
}

rootProject.name = "unsealed"
include(":app")
 