
plugins {
    `java-library`
}

group = "lunga"
version = "APPOINTMENT-BOOKING-UNSET-VERSION"
description = "validate-credential-module"


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
}

tasks.withType<Test> {
    useJUnitPlatform()
}
//tasks.bootJar {
//    enabled = false
//}

tasks.jar {
    enabled = true
    archiveBaseName.set("validate-credential-module")
    archiveVersion.set("APPOINTMENT-BOOKING-UNSET-VERSION")

    // Simple JAR - Keycloak provides all dependencies at runtime
    // Fat JAR can cause Quarkus loading issues with NullPointerException in OpenContainerPathTree.getRoots
    from(sourceSets.main.get().output)

    // Ensure META-INF/services is included for SPI registration
    from(sourceSets.main.get().resources)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}
