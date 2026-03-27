plugins {
    `kotlin-dsl`
}

repositories {
    maven { url = uri("https://repo1.maven.org/maven2") }
    maven { url = uri("https://plugins.gradle.org/m2") }
}

dependencies {
    implementation("de.undercouch:gradle-download-task:5.7.0")
}
