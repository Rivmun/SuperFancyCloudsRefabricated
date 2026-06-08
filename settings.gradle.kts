pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.architectury.dev/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9-beta.1"
}

stonecutter {
    create(rootProject) {
        fun mc(version: String, vararg loaders: String) = loaders
            .forEach {
                version("$version-$it", version).buildscript = "build.$it.gradle.kts"
            }

        mc("1.21.1", "fabric", "neoforge")
        mc("1.20.1", "fabric", "forge")
        mc("1.19.2", "fabric", "forge")
        mc("1.18.2", "fabric", "forge")
        mc("1.16.5", "fabric", "forge")

        vcsVersion = "1.21.1-fabric"
    }
}
