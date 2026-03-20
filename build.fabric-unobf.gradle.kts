plugins {
}

val minecraft = property("deps.minecraft") as String;

loom {
    silentMojangMappingsLicense()
    accessWidenerPath = file("src/main/resources/sfcr.uobf.accesswidener")
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
        this["arch-api"] =      prop("deps.arch-api")
        this["cloth"] =         prop("deps.cloth")
        this["distanthorizons_min_version"] = prop("distanthorizons_min_version")
        this["particlerain_min_version"] = prop("particlerain_min_version")

        this["access_widener"] = "${prop("mod.id")}.unobf.accesswidener"

        // insert version-specific mixins
//        this["RegistrySyncManagerMixin" ] = if (sc.current.parsed  > "1.20.1") "\"fabric.RegistrySyncManagerMixin\"," else ""
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
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")

    implementation("net.fabricmc:fabric-loader:${property("deps.fabric")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")
//    implementation("com.terraformersmc:modmenu:${property("deps.modmenu")}")
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml", "**/mods.toml", "**/${project.property("mod.id")}.accesswidener")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
