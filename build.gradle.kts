// import com.github.spotbugs.snom.Confidence
// import com.github.spotbugs.snom.Effort


plugins {
    `java-library`
    alias(libs.plugins.shadowPlugin)
    alias(libs.plugins.generatePOMPlugin)
    // alias(libs.plugins.spotBugsPlugin)
}


group = "com.clanjhoo"
version = "1.0.0-SNAPSHOT"
description = "Fixes manually placed trial spawners not dropping from ominous loot table when in ominous state."

ext.set("projectName", gradle.extra["projectName"].toString())
maven.pom {
    name = gradle.extra["projectName"].toString()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.ORACLE
    }
}

repositories {
    gradlePluginPortal {
        content {
            includeGroup("com.gradleup")
        }
    }
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
        content {
            includeGroup("io.papermc.paper")
            includeGroup("com.mojang")
            includeGroup("org.bukkit")
            includeGroup("org.spigotmc")
            includeGroup("net.md-5")
        }
    }
    mavenCentral()
}

dependencies {
    compileOnly(libs.papermc.paperapi)
    compileOnly(libs.kyori.adventure.minimessage)
    compileOnly(libs.kyori.adventure.gson)
    compileOnly(libs.kyori.platform.bukkit)
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filesMatching("**/plugin.yml") {
            expand( project.properties )
        }
    }
}
