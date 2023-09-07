plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

group = "com.blue.infra"
version = "1.0"

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.blue.infra.Main"
    }
}

repositories {
    mavenCentral()
}

dependencies {

    //implementation("org.apache.lucene:lucene-core:8.11.2")
    implementation("org.apache.lucene:lucene-core:8.4.0")
    //implementation("org.apache.lucene:lucene-backward-codecs:8.4.1")

    implementation("commons-codec:commons-codec:1.16.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")

    // https://github.com/elastic/elasticsearch/blob/33d1317336dd02b0aab50d592a4f212b7dea0bdc/build-tools-internal/version.properties#L14
    implementation("org.elasticsearch:elasticsearch:7.16.2")

}

configurations { create("externalLibs") }
