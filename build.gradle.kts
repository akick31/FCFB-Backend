import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.springframework.boot") version "2.7.18"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.fcfb"
version = "1.0.0"
description = "FCFB-Arceus"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()

    maven {
        name = "Sonatype Snapshots (Legacy)"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        name = "Sonatype Snapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.13")
    implementation("ch.qos.logback:logback-core:1.2.13")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.session:spring-session-core")
    implementation("org.projectlombok:lombok")
    implementation("org.mariadb.jdbc:mariadb-java-client")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("javax.persistence:javax.persistence-api")
    implementation("dev.kord:kord-core:0.13.1")
    implementation("org.reactivestreams:reactive-streams:1.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.9.0")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.3")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.6.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.vladmihalcea:hibernate-types-52:2.21.1")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
    implementation("ml.dmlc:xgboost4j_2.13:3.0.4")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("com.h2database:h2")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
