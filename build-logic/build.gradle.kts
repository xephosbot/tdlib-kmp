plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("tdlib-convention") {
            id = "tdlib-convention"
            implementationClass = "TdlibConventionPlugin"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    implementation(libs.android.gradle.plugin)
    implementation(libs.gradle.download.task.gradle.plugin)
}