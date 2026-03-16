plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.test"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "helpchat-repo"
        url = uri("https://repo.helpch.at/releases/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("dev.aurelium:auraskills-api:2.3.9")
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.9")
}

tasks {
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    shadowJar {
        relocate("dev.aurelium.slate", "dev.aurelium.auraskills.slate")
        archiveClassifier.set("") // Remove -all suffix if desired
    }

    build {
        dependsOn(shadowJar)
    }
}
