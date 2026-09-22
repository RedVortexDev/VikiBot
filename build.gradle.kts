plugins {
    application
    checkstyle
    java
    alias(libs.plugins.shadow)
}

group = "red.vortx.vikibot"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jda)
    implementation(libs.nightConfig)
    runtimeOnly(libs.slf4j.simple)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

application {
    mainClass = "red.vortx.vikibot.VikiBot"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
