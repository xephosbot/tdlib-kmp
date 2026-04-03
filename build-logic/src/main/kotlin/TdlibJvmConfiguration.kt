import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Bundles all 6 JNI desktop platform libraries into the JVM JAR resources under:
 *   natives/{resourcePath}/{libName}
 *
 * At runtime, [TdLibLoader] extracts the appropriate library for the host OS/arch.
 */
fun configureJvmTarget(project: Project, deps: TdlibDependencies, kotlin: KotlinMultiplatformExtension) = with(project) {
    val jniResourcesDir = layout.buildDirectory.dir("jvmJniResources")

    val stageTasks = TdlibTarget.jniTargets.map { jniTarget ->
        val extractTask = deps.register(jniTarget)
        val extractDir = layout.buildDirectory.dir("tdlib/${jniTarget.extractDir}")
        val suffix = jniTarget.extractDir
            .split("-")
            .joinToString("") { it.replaceFirstChar(Char::titlecase) }

        tasks.register("stageJvmJni$suffix", Copy::class.java) {
            group = "tdlib"
            description = "Stage ${jniTarget.assetName} JNI library into classpath resource layout"
            dependsOn(extractTask)
            from(extractDir.map { it.dir("lib") }) {
                include("*tdjsonjava*")
            }
            into(jniResourcesDir.map { it.dir("natives/${jniTarget.resourcePath}") })
        }.name
    }

    kotlin.sourceSets.getByName("jvmMain") {
        resources.srcDir(jniResourcesDir)
    }

    tasks.matching { it.name == "compileKotlinJvm" || it.name == "jvmProcessResources" }.configureEach {
        stageTasks.forEach { dependsOn(it) }
    }
}

/**
 * Copies Android .so files into the jniLibs source directory expected by AGP.
 *
 * Android archives are flat (no lib/ subdirectory), so we copy *.so directly
 * from the extract root into build/androidJniLibs/{abi}/.
 */
fun configureAndroidTarget(project: Project, deps: TdlibDependencies) = with(project) {
    val jniLibsBaseDir = layout.buildDirectory.dir("androidJniLibs")

    val copyTasks = TdlibTarget.androidTargets.map { androidTarget ->
        val extractTask = deps.register(androidTarget)
        val extractDir = layout.buildDirectory.dir("tdlib/${androidTarget.extractDir}")
        val suffix = androidTarget.extractDir
            .split("-")
            .joinToString("") { it.replaceFirstChar(Char::titlecase) }

        tasks.register("copyAndroidJniLibs$suffix", Copy::class.java) {
            group = "tdlib"
            description = "Copy ${androidTarget.resourcePath} .so libs for Android AAR"
            dependsOn(extractTask)
            // Android .so archives are flat (no lib/ subdirectory)
            from(extractDir) { include("*.so") }
            into(jniLibsBaseDir.map { it.dir(androidTarget.resourcePath) })
        }.name
    }

    extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)
        .onVariants { variant ->
            variant.sources.jniLibs?.addStaticSourceDirectory(
                jniLibsBaseDir.get().asFile.absolutePath
            )
        }

    // Wire copy tasks before AGP merges JNI library folders.
    tasks.configureEach {
        if (name.startsWith("merge") && name.endsWith("JniLibFolders")) {
            copyTasks.forEach { dependsOn(it) }
        }
    }
}
