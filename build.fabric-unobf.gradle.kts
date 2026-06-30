plugins {
    id("dev.architectury.loom-no-remap") version "1.14-SNAPSHOT"
}

val minecraft = property("deps.minecraft") as String

loom {
    //accessWidenerPath = rootProject.file("src/main/resources/${property("mod.id")}.unobf.accesswidener")
}

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["mod_group"] =     prop("mod.group")
        this["mod_id"] =        prop("mod.id")
        this["mod_name"] =      prop("mod.name") + "Refabricated"
        this["mod_version"] =   prop("mod.version")
        this["mod_description"]=prop("mod.description")
        this["mod_author"] =    prop("mod.author")
        this["mod_contributor"]=prop("mod.contributor")
        this["mod_sources"] =   prop("mod.sources")
        this["mod_issues"] =    prop("mod.issues")
        this["mod_homepage"] =  prop("mod.homepage")
        this["mod_modrinth"] =  prop("mod.modrinth")
        this["mod_mcmod"] =     prop("mod.mcmod")
        this["mod_license"] =   prop("mod.license")
        this["mod_icon"] =      prop("mod.icon") + "fabric"

        this["version_range"] = prop("version_range")
        this["cloth"] =         prop("deps.cloth")
        this["distanthorizons_min_version"] = prop("distanthorizons_min_version")
        this["particlerain_min_version"] = prop("particlerain_min_version")

        //this["access_widener"] = "${prop("mod.id")}.unobf.accesswidener"

        // insert version-specific mixins
        this["particlerain_mixin"] = "\"particlerain.ParticleSpawnerMixin\","
        this["ServerLevelAccessor"] = ""
        this["clientlevel_mixin"] = if (sc.current.parsed < "26.2") "" else "\"extra.ClientLevelMixin\","

        // insert deps
        this["particlerain_deps"] = "\"particlerain\": \">=${prop("particlerain_min_version")}\","
        this["sereneseasons_deps"] = ""
    }

    filesMatching(listOf("fabric.mod.json", "${prop("mod.id")}.mixins.json")) {
        expand(props)
    }
}

version = "${property("mod.version")}+${minecraft}-fabric"
base.archivesName = property("mod.id") as String

repositories {
    mavenLocal()
    maven("https://api.modrinth.com/maven")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.shedaniel.me/")
    maven("https://jitpack.io")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    implementation("net.fabricmc:fabric-loader:${property("deps.fabric")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")

    //modmenu
    implementation("com.terraformersmc:modmenu:${property("deps.modmenu")}")
    // cloth
    api("me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    //distant horizons
    api("maven.modrinth:DistantHorizonsApi:${property("deps.distanthorizons-api")}")
//    runtimeOnly("maven.modrinth:DistantHorizons:${property("deps.distanthorizons")}")
    //particle rain
    compileOnly("maven.modrinth:particle-rain:${property("deps.particlerain")}")
    //serene seasons
    compileOnly("maven.modrinth:serene-seasons:${property("deps.sereneseasons")}")
    //Iris
    compileOnly("maven.modrinth:iris:${property("deps.iris")}-fabric")
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml", "**/icon-neoforge.png", "**/*.accesswidener")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
