plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.dli.assets"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/paper-weighted/") // For adventure-platform-bukkit
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")

    // Configuration - SnakeYAML
    implementation("org.yaml:snakeyaml:2.3")

    // JSON/Serialization - Gson or Jackson
    implementation("com.google.code.gson:gson:2.11.0")

    // Kotlin Stdlib
    implementation(kotlin("stdlib-jdk8"))

    // Adventure API (for modern text/components)
    compileOnly("net.kyori:adventure-api:4.18.0")
    // adventure-platform-bukkit is included in Paper API
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjvm-default=all", "-Xopt-in=kotlin.RequiresOptIn")
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes(
            "Main-Class" to "com.dli.assets.DLIAssets",
            "Implementation-Version" to project.version.toString()
        )
    }
    // Relocate libraries to avoid conflicts
    relocate("com.google.gson", "com.dli.assets.shaded.gson")
    relocate("org.yaml", "com.dli.assets.shaded.yaml")
}

tasks.jar {
    enabled = false // Use shadowJar only
}

tasks.build {
    dependsOn(tasks.shadowJar)
}