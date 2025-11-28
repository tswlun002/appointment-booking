plugins {
    `java-library`
}

group = "lunga"
version = "APPOINTMENT-BOOKING-UNSET-VERSION"
description = "generate-username-module-for-keycloak-ui-registering"


java {
    sourceCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}



extra["keycloakVersion"] = "26.0.4"

dependencies {

    // <!-- Keycloak Server SPI -->
    compileOnly("org.keycloak:keycloak-server-spi:${property("keycloakVersion")}")
    //<!-- Keycloak Server SPI Private (needed for some internals) -->
    compileOnly("org.keycloak:keycloak-server-spi-private:${property("keycloakVersion")}")
    //    <!-- Keycloak Core API -->
    compileOnly("org.keycloak:keycloak-core:${property("keycloakVersion")}")
    //    <!-- Keycloak Services (Contains UsernamePasswordForm) -->
    compileOnly("org.keycloak:keycloak-services:${property("keycloakVersion")}")
    compileOnly("org.slf4j:slf4j-api:2.0.12")

    // Mapping & Validation
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    implementation("org.apache.commons:commons-lang3:3.20.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
//tasks.bootJar {
//    enabled = false
//}

tasks.jar {
    enabled = true
    archiveBaseName.set("generate-username-module")
    archiveVersion.set("APPOINTMENT-BOOKING-UNSET-VERSION")

    // Include compiled classes
    from(sourceSets.main.get().output)

    // Include dependencies (fat JAR) - exclude Keycloak
    from({
        configurations.runtimeClasspath.get()
            .filter { !it.name.startsWith("keycloak") }
            .map { if (it.isDirectory) it else zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}