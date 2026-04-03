import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class TdlibConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val ext = extensions.create<TdlibExtension>("tdlib")

        afterEvaluate {
            val version = ext.version.get()
            val deps = TdlibDependencies(project, version)
            val kotlin = the<KotlinMultiplatformExtension>()

            kotlin.applyHierarchyTemplate(tdlibSourceSetHierarchyTemplate)

            if (ext.jvmEnabled) {
                configureJvmTarget(project, deps, kotlin)
            }

            if (ext.androidEnabled) {
                configureAndroidTarget(project, deps)
            }

            ext.nativeTargets.forEach { spec ->
                val nativeTarget = kotlin.targets.getByName(spec.kotlinTargetName) as KotlinNativeTarget
                configureCinteropTarget(project, deps, nativeTarget, spec)
            }
        }
    }
}

