import java.util.zip.ZipFile

plugins {
    id("dev.architectury.loom")
}

val minecraft = property("deps.minecraft") as String

loom {
    silentMojangMappingsLicense()
//    accessWidenerPath = rootProject.file("src/main/resources/sfcr.accesswidener")

    forge {
//        convertAccessWideners = true
//        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)

        mixinConfig("${property("mod.id")}.mixins.json")
    }

    if (sc.current.parsed < "1.20") {
        mixin.useLegacyMixinAp = false
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
        this["forge_min_version"] = prop("forge_min_version")
        this["arch_api"] =      prop("deps.arch-api")
        this["cloth_id"] =      if (sc.current.parsed > "1.18") "cloth_config" else "cloth-config"
        this["cloth"] =         prop("deps.cloth")
        this["distanthorizons_min_version"] = prop("distanthorizons_min_version")
        this["sereneseasons"] = if (sc.current.parsed > "1.18") prop("deps.sereneseasons") else "1.16.5-${prop("deps.sereneseasons")}"

        // insert version-specific mixins
        this["particlerain_mixin"] = if (sc.current.parsed > "1.20") "\"particlerain.ParticleSpawnerMixin\"," else ""

        // insert deps
        this["particlerain_deps"] = if (sc.current.parsed > "1.20")
            "[[dependencies.${prop("mod.id")}]]\nmodId = \"particlerain\"\nmandatory = false\nversionRange = \"[${prop("particlerain_min_version")},)\"\nordering = \"NONE\"\nside = \"CLIENT\"\n" else ""
    }

    filesMatching(listOf("META-INF/mods.toml", "${prop("mod.id")}.mixins.json")) {
        expand(props)
    }
}

version = "${property("mod.version")}+${minecraft}-forge"
base.archivesName = property("mod.id") as String

repositories {
    mavenLocal()
    maven("https://maven.architectury.dev/")
    maven("https://maven.shedaniel.me/")
    maven("https://api.modrinth.com/maven")
    maven("https://repo.spongepowered.org/maven/")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.officialMojangMappings())
    forge("net.minecraftforge:forge:${property("deps.forge")}")

    // disable mixin processor to let tiny mapper remapped mixin class reference automatically, and no refmap file created.
//    if (sc.current.parsed < "1.20") {
//        annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
//    }
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")!!)
    if (sc.current.parsed > "1.17") {
        implementation(include("io.github.llamalad7:mixinextras-forge:${property("deps.mixinextras")}")) {}
    } else {
        // mixinextras JIJ on 1.16.5 is unsupported
        implementation("io.github.llamalad7:mixinextras-forge:${property("deps.mixinextras")}") {}
    }

    // Arch-api
    if (sc.current.parsed > "1.18") {
        modApi("dev.architectury:architectury-forge:${property("deps.arch-api")}")
    } else {
        modApi("me.shedaniel:architectury-forge:${property("deps.arch-api")}")
    }
    // cloth
    modApi("me.shedaniel.cloth:cloth-config-forge:${property("deps.cloth")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    //distant horizons
    modApi("maven.modrinth:DistantHorizonsApi:${property("deps.distanthorizons-api")}")
//    modRuntimeOnly("maven.modrinth:DistantHorizons:${property("deps.distanthorizons")}${minecraft}")

    //particle rain
    if (sc.current.parsed > "1.20") {
        modCompileOnly("maven.modrinth:particle-rain:${property("deps.particlerain")}")
    }
    //serene seasons
    modCompileOnly("maven.modrinth:serene-seasons:${property("deps.sereneseasons")}")
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/*.accesswidener", "**/neoforge.mods.toml")
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

    // resolve 1.16.5 mixinextras classDefNotFound / JIJ load failure issue.
    if (sc.current.parsed < "1.17") {
        val mixinExtrasVersion = property("deps.mixinextras").toString()

        // 专用可解析配置：制品由 Gradle 缓存保证存在，不依赖任何任务输出
        val mixinExtrasArtifacts = configurations.create("mixinExtrasArtifacts") {
            isCanBeConsumed = false
            isCanBeResolved = true
            description = "MixinExtras artifacts unpacked into our own jar on 1.16.5"
        }
        project.dependencies.add(mixinExtrasArtifacts.name, "io.github.llamalad7:mixinextras-common:$mixinExtrasVersion")
        project.dependencies.add(mixinExtrasArtifacts.name, "io.github.llamalad7:mixinextras-forge:$mixinExtrasVersion")

        val mixinExtrasClassesDir = layout.buildDirectory.dir("classes/java/main").get().asFile

        // 用 Property<File> 传递制品路径：
        // - 不能在配置期立即解析（会撞上 Loom 的 remapped_mods 仓库初始化）
        // - 也不能在任务动作里捕获 Configuration（配置缓存不支持序列化）
        val mixinExtrasCoreJar = objects.fileProperty()
        val mixinExtrasWrapperJar = objects.fileProperty()
        project.afterEvaluate {
            val files = mixinExtrasArtifacts.files
            mixinExtrasCoreJar.set(files.first { it.name.startsWith("mixinextras-common-") })
            mixinExtrasWrapperJar.set(files.first { it.name.startsWith("mixinextras-forge-") })
        }

        // 把核心类摊进 main classes 目录（Mixin 运行时能加载到，也会被 jar 任务打进产物）
        val copyMixinExtras = register("copyMixinExtras") {
            group = "build"
            description = "Unpack MixinExtras core classes into the main classes directory"
            inputs.file(mixinExtrasCoreJar).withPropertyName("mixinExtrasCoreJar")
            outputs.dir(mixinExtrasClassesDir.resolve("com/llamalad7/mixinextras"))
            doLast {
                ZipFile(mixinExtrasCoreJar.get().asFile).use { zip ->
                    for (entry in zip.entries()) {
                        val name = entry.name
                        if (entry.isDirectory || !name.startsWith("com/") || name == "module-info.class") continue
                        val out = mixinExtrasClassesDir.resolve(name)
                        out.parentFile.mkdirs()
                        zip.getInputStream(entry).use { input -> out.outputStream().use { input.copyTo(it) } }
                    }
                }
            }
        }

        // MixinExtras 的 LICENSE 只在 mixinextras-forge 包装 jar 里，一并复制进产物
        val copyMixinExtrasLicense = register("copyMixinExtrasLicense") {
            group = "build"
            description = "Copy the MixinExtras license into the mod jar"
            val licenseDir = mixinExtrasClassesDir.resolve("META-INF/licenses/mixinextras")
            inputs.file(mixinExtrasWrapperJar).withPropertyName("mixinExtrasWrapperJar")
            outputs.dir(licenseDir)
            doLast {
                ZipFile(mixinExtrasWrapperJar.get().asFile).use { zip ->
                    val entry = zip.getEntry("LICENSE_MixinExtras") ?: return@use
                    licenseDir.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        licenseDir.resolve("LICENSE_MixinExtras").outputStream().use { input.copyTo(it) }
                    }
                }
            }
        }

        jar {
            dependsOn(copyMixinExtras, copyMixinExtrasLicense)
        }
        remapJar {
            dependsOn(copyMixinExtras)
        }
    }
}

java {
    val javaCompat = if (stonecutter.eval(stonecutter.current.version, ">=1.21")) {
        JavaVersion.VERSION_21
    } else if (stonecutter.eval(stonecutter.current.version, ">=1.18")) {
        JavaVersion.VERSION_17
    } else {
        JavaVersion.VERSION_1_8
    }
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}
