plugins {
    id("dev.architectury.loom")
}

val minecraft = property("deps.minecraft") as String

loom {
    silentMojangMappingsLicense()
    if (sc.current.parsed > "1.21.1") accessWidenerPath = rootProject.file("src/main/resources/sfcr.accesswidener")
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
        this["fabricseasons_min_version"] = prop("fabricseasons_min_version")

        this["access_widener"] = "${prop("mod.id")}.accesswidener"

        // insert version-specific mixins
        this["particlerain_mixin"] = if (sc.current.parsed > "1.20") "\"particlerain.ParticleSpawnerMixin\"," else
            if (sc.current.parsed > "1.19") "\"particlerain.RainDropParticleMixin\",\n    \"particlerain.WeatherParticleSpawnerMixin\"," else ""

        // insert deps
        this["particlerain_deps"] = if (! sc.current.parsed.eq("1.18.2")) "\"particlerain\": \">=${prop("particlerain_min_version")}\"," else ""
        this["sereneseasons_deps"] = if (sc.current.parsed > "1.20") "\"sereneseasons\": \">=${prop("deps.sereneseasons")}\"," else ""
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
    if (sc.current.parsed < "1.20") {
        maven("https://cursemaven.com")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.officialMojangMappings())
    implementation("com.google.code.gson:gson:2.10.1")
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
    if (sc.current.parsed > "1.20") {
        modCompileOnly("maven.modrinth:particle-rain:${property("deps.particlerain")}")
    } else if (! sc.current.parsed.eq("1.18.2")){
        modCompileOnly("curse.maven:particle-rain-421897:${property("deps.particlerain")}")
    }
    //serene seasons
    if (sc.current.parsed > "1.20") {
        modCompileOnly("maven.modrinth:serene-seasons:${property("deps.sereneseasons")}")
    }
    //fabric seasons
    if (stonecutter.current.parsed <= "1.21.1") {
        modCompileOnly("maven.modrinth:fabric-seasons:${property("deps.fabric_seasons")}")
    }
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml", "**/mods.toml", "**/${project.property("mod.id")}.unobf.accesswidener")
        if (sc.current.parsed <= "1.21.1") {
            exclude("**/*.vsh", "**/${project.property("mod.id")}.accesswidener")
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
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
