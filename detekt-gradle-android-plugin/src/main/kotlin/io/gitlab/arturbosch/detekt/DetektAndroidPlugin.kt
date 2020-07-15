package io.gitlab.arturbosch.detekt

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.TestedVariant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import java.io.File

class DetektAndroidPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply(DetektPlugin::class.java)
        val extension = project.extensions.getByType(DetektExtension::class.java)
        project.registerDetektTasks(extension)
    }

    private fun Project.registerDetektTasks(extension: DetektExtension) {
        project.plugins.withId("kotlin-android") {
            // There is not a single Android plugin, but each registers an extension based on BaseExtension,
            // so we catch them all by looking for this one
            project.afterEvaluate {
                val androidExtension = project.extensions.findByType(BaseExtension::class.java)
                androidExtension?.let {
                    val mainTaskProvider = project.tasks.register("${DetektPlugin.DETEKT_TASK_NAME}Main") {
                        it.group = "verification"
                        it.description = "EXPERIMENTAL & SLOW: Run detekt analysis for production classes across " +
                                "all variants with type resolution"
                    }
                    val testTaskProvider = project.tasks.register("${DetektPlugin.DETEKT_TASK_NAME}Test") {
                        it.group = "verification"
                        it.description = "EXPERIMENTAL & SLOW: Run detekt analysis for test classes across " +
                                "all variants with type resolution"
                    }
                    val bootClasspath = files(androidExtension.bootClasspath)
                    androidExtension.variants?.all { variant ->
                        project.registerAndroidDetektTask(bootClasspath, extension, variant).also { provider ->
                            mainTaskProvider.dependsOn(provider)
                        }
                        variant.testVariants.forEach { testVariant ->
                            project.registerAndroidDetektTask(bootClasspath, extension, testVariant).also { provider ->
                                testTaskProvider.dependsOn(provider)
                            }
                        }
                    }
                }
            }
        }
    }

    private val BaseExtension.variants: DomainObjectSet<out BaseVariant>?
        get() = when (this) {
            is AppExtension -> applicationVariants
            is LibraryExtension -> libraryVariants
            is TestExtension -> applicationVariants
            else -> null
        }

    private val BaseVariant.testVariants: List<BaseVariant>
        get() = if (this is TestedVariant) listOfNotNull(testVariant, unitTestVariant)
        else emptyList()

    private fun Project.registerAndroidDetektTask(
        bootClasspath: FileCollection,
        extension: DetektExtension,
        variant: BaseVariant
    ): TaskProvider<Detekt> =
        registerDetektTask(DetektPlugin.DETEKT_TASK_NAME + variant.name.capitalize(), extension) {
            setSource(variant.sourceSets.map { it.javaDirectories })
            classpath.setFrom(variant.getCompileClasspath(null) + bootClasspath)
            reports.xml.destination = File(extension.reportsDir, variant.name + ".xml")
            reports.html.destination = File(extension.reportsDir, variant.name + ".html")
            reports.txt.destination = File(extension.reportsDir, variant.name + ".txt")
            description = "EXPERIMENTAL & SLOW: Run detekt analysis for ${variant.name} classes with type resolution"
        }
}
