plugins {
    id("dev.architectury.loom")
}

val minecraft = property("deps.minecraft") as String

loom {
    silentMojangMappingsLicense()
//    accessWidenerPath = rootProject.file("src/main/resources/sfcr.accesswidener")
}

sourceSets.main {
    resources {
        // Oh, my dear arch-loom, why you change src dir only for forge-like platform...
        setSrcDirs(listOf(rootProject.file("src/main/resources")))
    }
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
        this["neoforge_min_version"] = prop("neoforge_min_version")
        this["arch_api"] =      prop("deps.arch-api")
        this["cloth"] =         prop("deps.cloth")
        this["distanthorizons_min_version"] = prop("distanthorizons_min_version")
        this["particlerain_min_version"] = if (sc.current.parsed <= "1.21.1") prop("particlerain_min_version") else ""
        this["sereneseasons"] = prop("deps.sereneseasons")

        // insert version-specific mixins
        this["particlerain_mixin"] = "\"particlerain.ParticleSpawnerMixin\","
    }

    filesMatching(listOf("META-INF/neoforge.mods.toml", "${prop("mod.id")}.mixins.json")) {
        expand(props)
    }
}

version = "${property("mod.version")}+${minecraft}-neoforge"
base.archivesName = property("mod.id") as String

repositories {
    mavenLocal()
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.architectury.dev/")
    maven("https://maven.shedaniel.me/")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.officialMojangMappings())
    implementation("com.google.code.gson:gson:2.10.1")
    neoForge("net.neoforged:neoforge:${property("deps.neoforge")}")

    // Arch-api
    modApi("dev.architectury:architectury-neoforge:${property("deps.arch-api")}")
    // cloth
    modApi("me.shedaniel.cloth:cloth-config-neoforge:${property("deps.cloth")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    //distant horizons
    modApi("maven.modrinth:DistantHorizonsApi:${property("deps.distanthorizons-api")}")
    modRuntimeOnly("maven.modrinth:DistantHorizons:${property("deps.distanthorizons")}")

    //particle rain
    if (sc.current.parsed eq "1.21.1") {
        modCompileOnly("maven.modrinth:particle-rain:${property("deps.particlerain")}")
    }

    //serene seasons
    modCompileOnly("maven.modrinth:serene-seasons:${property("deps.sereneseasons")}")
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/mods.toml", "**/${project.property("mod.id")}.unobf.accesswidener")
        if (sc.current.parsed <= "1.21.1") {
            exclude("**/*.vsh", "**/${project.property("mod.id")}.accesswidener")
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs"))
        dependsOn("build")
    }

    jar {
        manifest.attributes["MixinConfigs"] = "${project.property("mod.id")}.mixins.json"
    }
}

java {
    val javaCompat = if (stonecutter.eval(stonecutter.current.version, ">=1.20")) {
        JavaVersion.VERSION_21
    } else if (stonecutter.eval(stonecutter.current.version, ">=1.18")) {
        JavaVersion.VERSION_17
    } else {
        JavaVersion.VERSION_1_8
    }
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}
