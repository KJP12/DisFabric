plugins {
    id("fabric-loom") version "1.0.+"
    id("maven-publish")
    id("project-report")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

val archives_base_name: String by project
val minecraft_version: String by project
val mod_version: String by project
val maven_group: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_version: String by project

val archivesBaseName = "$archives_base_name-$minecraft_version"
version = mod_version
group = maven_group

repositories {
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        mavenContent {
            includeGroup("maven.modrinth")
        }
    }
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots") { name = "OSS Sonatype Snapshots" }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.the-glitch.network") { name = "The Glitch" }
    maven("https://maven.nucleoid.xyz") { name = "NucleoidMC" }
    maven("https://maven.shedaniel.me/")
    maven("https://jitpack.io/") {
        name = "JitPack"
        mavenContent {
            includeGroupByRegex("com\\.github\\..*")
        }
    }
}

dependencies {
    val excludeFabricApi = Action<ExternalModuleDependency> {
        exclude(group = "net.fabricmc.fabric-api", module = "fabric-api")
    }

    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings("net.fabricmc:yarn:${yarn_mappings}:v2")

    modImplementation("net.fabricmc:fabric-loader:${loader_version}")
    modImplementation("net.fabricmc.fabric-api", "fabric-api", fabric_version) {
        exclude(module = "fabric-gametest-api-v1")
    }
    // Markdown
    implementation("org.commonmark:commonmark:0.18.1")
    modImplementation("dev.gegy:markdown-chat:1.3.0", excludeFabricApi)

    // Fabric Tailor
    modImplementation("com.github.samolego.Config2Brigadier:config2brigadier-fabric:1.2.3", excludeFabricApi)
    modImplementation("maven.modrinth:fabrictailor:2.0.1")
    modRuntimeOnly("maven.modrinth:drogtor:1.1.3+1.19")
    include(modImplementation("fr.catcore:server-translations-api:1.4.14+1.19-rc2", excludeFabricApi))

    modRuntimeOnly("net.fabricmc:fabric-language-kotlin:1.8.2+kotlin.1.7.10")

    include(implementation("net.dv8tion:JDA:5.0.0-alpha.18") {
        exclude(module = "opus-java")
    })

    // 1.3.2
    modImplementation("maven.modrinth:vanish:zr3pJkB2") {
        excludeFabricApi(this)
        isTransitive = false
    }

    // JIJ is useless, why; required for Vanish
    modRuntimeOnly("eu.pb4:player-data-api:0.2.1+1.19")
    modRuntimeOnly("eu.pb4:placeholder-api:2.0.0-beta.7+1.19")
    modRuntimeOnly("com.github.LlamaLad7:MixinExtras:0.1.1")
    modRuntimeOnly("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")

    include(modApi("me.sargunvohra.mcmods:autoconfig1u:3.3.1", excludeFabricApi))

    include(implementation("com.fasterxml.jackson.core", "jackson-databind", "2.13.3"))
    include(implementation("net.sf.trove4j", "trove4j", "3.0.3"))
    include(implementation("org.apache.commons", "commons-collections4", "4.4"))
    include(implementation("com.neovisionaries", "nv-websocket-client", "2.14"))
    include(implementation("com.squareup.okhttp3", "okhttp", "4.10.0"))
    include(implementation("com.fasterxml.jackson.core", "jackson-annotations", "2.13.3"))
    include(implementation("com.fasterxml.jackson.core", "jackson-core", "2.13.3"))
    include(implementation("com.squareup.okio", "okio", "3.2.0"))
    include(implementation("com.squareup.okio", "okio-jvm", "3.2.0"))
    include(implementation("com.neovisionaries", "nv-websocket-client", "2.14"))
}

tasks {
    processResources {
        inputs.property("version", version)

        filesMatching("fabric.mod.json") {
            expand("version" to version)
        }
    }

    withType<JavaCompile> {
        // ensure that the encoding is set to UTF-8, no matter what the system default is
        // this fixes some edge cases with special characters not displaying correctly
        // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
        // If Javadoc is generated, this must be specified in that task too.
        options.encoding = "UTF-8"
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${archivesBaseName}" }
        }
    }
}
