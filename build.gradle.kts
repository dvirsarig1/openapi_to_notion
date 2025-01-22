import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

repositories {
    mavenCentral()
}

dependencies {
    val notionSdkVersion: String by project
    val swaggerParserVersion: String by project
    val logbackVersion: String by project
    val kamlVersion: String by project
    val kotlinSerializationVersion: String by project
    val cliktVersion: String by project

    implementation("io.swagger.parser.v3:swagger-parser:$swaggerParserVersion")
    implementation("com.github.seratch:notion-sdk-jvm-core:$notionSdkVersion")
    implementation("com.github.seratch:notion-sdk-jvm-slf4j:$notionSdkVersion")
    implementation("com.github.seratch:notion-sdk-jvm-httpclient:$notionSdkVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.charleskorn.kaml:kaml:$kamlVersion")
    implementation("com.google.inject:guice:7.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveFileName.set("app.jar")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "MainKt"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

tasks.test {
    useJUnitPlatform()
}

