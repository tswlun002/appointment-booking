
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
extra["caffeineVersion"] = "3.2.3"
extra["resilience4jVersion"] = "2.2.0"

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

    // Cache
    implementation("com.github.ben-manes.caffeine:caffeine:${property("caffeineVersion")}")
    // Optional extensions for caffeine cache
    implementation("com.github.ben-manes.caffeine:guava:${property("caffeineVersion")}")
    implementation("com.github.ben-manes.caffeine:jcache:${property("caffeineVersion")}")

    // Resilience4j - Circuit Breaker and Retry
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-retry:${property("resilience4jVersion")}")

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
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")


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
    testImplementation("org.wiremock.integrations.testcontainers:wiremock-testcontainers-module:1.0-alpha-15")
    // Add the standalone WireMock JAR with the official group ID for the client API
    testImplementation("org.wiremock:wiremock-standalone:3.13.2") // Or use the latest 3.x version
}
evaluationDependsOnChildren()


// 1. Ensure 'build' depends on submodules (as you already have)
tasks.named("build") {
    dependsOn(project(":validate-credential-module").tasks.named("build"))
    dependsOn(project(":generate-username-ui-register-module").tasks.named("build"))
}

// 2. 💡 CRITICAL: Ensure 'test' also depends on submodules
tasks.named("test") {
    dependsOn(project(":validate-credential-module").tasks.named("build"))
    dependsOn(project(":generate-username-ui-register-module").tasks.named("build"))
}
tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("appointment-booking")
    mainClass.set("capitec.branch.appointment.AppointmentBookingApplication")
}


