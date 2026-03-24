plugins {
    id("dev.architectury.loom")
}

val minecraft = property("deps.minecraft") as String

loom {
    silentMojangMappingsLicense()
    accessWidenerPath = rootProject.file("src/main/resources/sfcr.accesswidener")
}

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["mod_group"] =     prop("mod.group")
        this["mod_id"] =        prop("mod.id")
        this["mod_name"] =      prop("mod.name")
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
        this["mod_icon"] =      prop("mod.icon")

        this["version_range"] = prop("version_range")
        this["arch_api"] =      prop("deps.arch-api")
        this["cloth"] =         prop("deps.cloth")
        this["distanthorizons_min_version"] = prop("distanthorizons_min_version")

        this["access_widener"] = "${prop("mod.id")}.accesswidener"

        // insert version-specific mixins
        this["particlerain_mixin"] = "\"particlerain.ParticleSpawnerMixin\","

        // insert deps
        this["particlerain_deps"] = "\"particlerain\": \">=${prop("particlerain_min_version")}\","
        this["sereneseasons_deps"] = "\"sereneseasons\": \">=${prop("deps.sereneseasons")}\","
    }

    filesMatching(listOf("fabric.mod.json", "${prop("mod.id")}.mixins.json")) {
        expand(props)
    }
}

version = "${property("mod.version")}+${minecraft}-fabric"
base.archivesName = property("mod.id") as String

repositories {
    mavenLocal()
    maven("https://maven.architectury.dev/")
    maven("https://maven.shedaniel.me/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.terraformersmc.com/")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")

    // Arch-api
    modApi("dev.architectury:architectury-fabric:${property("deps.arch-api")}")
    // cloth
    modApi("me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    //modmenu
    modImplementation("com.terraformersmc:modmenu:${property("deps.modmenu")}")

    //distant horizons
    modApi("maven.modrinth:DistantHorizonsApi:${property("deps.distanthorizons-api")}")
    modRuntimeOnly("maven.modrinth:DistantHorizons:${property("deps.distanthorizons")}")

    //particle rain
    modCompileOnly("maven.modrinth:particle-rain:${property("deps.particlerain")}")
    //serene seasons
    modCompileOnly("maven.modrinth:serene-seasons:${property("deps.sereneseasons")}")
    //Iris
    modCompileOnly("maven.modrinth:iris:${property("deps.iris")}")
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml", "**/mods.toml", "**/${project.property("mod.id")}.unobf.accesswidener", "**/*.mcmeta")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs"))
        dependsOn("build")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
