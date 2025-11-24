plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "lunga"
version = "APPOINTMENT-BOOKING-UNSET-VERSION"
description = "appointment-booking"

java {
    sourceCompatibility = JavaVersion.VERSION_25

    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

// VERSIONS
extra["resteasyVersion"] = "6.2.10.Final"
extra["keycloakVersion"] = "26.0.5"

dependencies {

    //springboot web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.apache.httpcomponents.client5:httpclient5")

    //security
    //implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    //Database
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // Keycloak
    implementation("org.keycloak:keycloak-admin-client:${property("keycloakVersion")}")
    implementation("org.jboss.resteasy:resteasy-client:${property("resteasyVersion")}")
    implementation("org.jboss.resteasy:resteasy-jackson2-provider:${property("resteasyVersion")}")

    //Lombok
    compileOnly("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Email
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("jakarta.mail:jakarta.mail-api")


    // Env utilities
    implementation("me.paulschwarz:spring-dotenv:4.0.0")
    // Validator
    implementation("commons-validator:commons-validator:1.9.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Mapping
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.0")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation (platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:kafka")
}

tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("appointment-booking")
    mainClass.set("lunga.appointmentbooking.AppointmentBookingApplication")
}
