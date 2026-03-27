import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin that makes all build-logic classes
 * (TdlibDependencies, TdlibProjectContext, configuration helpers, etc.)
 * available on the classpath of the consuming project.
 *
 * Apply this plugin in your module's build.gradle.kts:
 *   plugins {
 *       id("tdlib-convention")
 *   }
 */
class TdlibConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // This plugin exists primarily to put build-logic classes on the
        // consumer's classpath.  Actual wiring (TdlibProjectContext creation,
        // target configuration, etc.) is done explicitly in the module's
        // build script or can be added here later.
    }
}

