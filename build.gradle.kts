import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "dev.encore"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

ktor {
    fatJar {
        archiveFileName.set("encore.jar")
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("encore.jar")
    destinationDirectory.set(file("deploy"))
    manifest {
        attributes["Main-Class"] = "io.ktor.server.netty.EngineMain"
    }
}

val copyStaticGame by tasks.registering(Copy::class) {
    from("static-game")
    into("deploy/static-game")
}

val copyStaticBackstage by tasks.registering(Copy::class) {
    from("static-backstage")
    into("deploy/static-backstage")
}

tasks.shadowJar {
    finalizedBy(copyStaticBackstage)
    finalizedBy(copyStaticGame)
}

tasks.named<JavaExec>("run") {
    jvmArgs(
        "--sun-misc-unsafe-memory-access=allow",
        "--enable-native-access=ALL-UNNAMED"
    )
}

dependencies {
    // Ktor core
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    // implementation(libs.ktor.serialization.kotlinx.protobuf)
    implementation(libs.logback.classic)

    // Security
    implementation(libs.library.bcrypt)

    // Database
    implementation(libs.mongodb.driver.kotlin.coroutine)
    implementation(libs.mongodb.bson.kotlinx)

    // Tests
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.server.test.host)
}
