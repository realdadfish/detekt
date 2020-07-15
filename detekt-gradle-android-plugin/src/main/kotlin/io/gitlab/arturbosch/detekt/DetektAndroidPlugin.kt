package io.gitlab.arturbosch.detekt

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.TestedVariant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import io.gitlab.arturbosch.detekt.extensions.DetektAndroidExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import java.io.File

class DetektAndroidPlugin : Plugin<Project> {

    private lateinit var project: Project
    private val mainTaskProvider: TaskProvider<Task> by lazy {
        project.tasks.register("${DetektPlugin.DETEKT_TASK_NAME}Main") {
            it.group = "verification"
            it.description = "EXPERIMENTAL & SLOW: Run detekt analysis for production classes across " +
                    "all variants with type resolution"
        }
    }

    private val testTaskProvider: TaskProvider<Task> by lazy {
        project.tasks.register("${DetektPlugin.DETEKT_TASK_NAME}Test") {
            it.group = "verification"
            it.description = "EXPERIMENTAL & SLOW: Run detekt analysis for test classes across " +
                    "all variants with type resolution"
        }
    }

    override fun apply(project: Project) {
        this.project = project
        project.pluginManager.apply(DetektPlugin::class.java)
        val extension = project.extensions.getByType(DetektExtension::class.java)
        val androidExtension = project.extensions.create(DETEKT_ANDROID_EXTENSION, DetektAndroidExtension::class.java)
        project.registerDetektTasks(extension, androidExtension)
    }

    private fun Project.registerDetektTasks(
        extension: DetektExtension,
        androidExtension: DetektAndroidExtension
    ) {
        project.plugins.withId("kotlin-android") {
            // There is not a single Android plugin, but each registers an extension based on BaseExtension,
            // so we catch them all by looking for this one
            project.afterEvaluate {
                val baseExtension = project.extensions.findByType(BaseExtension::class.java)
                baseExtension?.let {
                    val bootClasspath = files(baseExtension.bootClasspath)
                    baseExtension.variants
                        ?.matching { !androidExtension.matchesIgnoredConfiguration(it) }
                        ?.all { variant ->
                            project.registerAndroidDetektTask(bootClasspath, extension, variant).also { provider ->
                                mainTaskProvider.dependsOn(provider)
                            }
                            variant.testVariants
                                .filter { !androidExtension.matchesIgnoredConfiguration(it) }
                                .forEach { testVariant ->
                                    project.registerAndroidDetektTask(bootClasspath, extension, testVariant)
                                        .also { provider ->
                                            testTaskProvider.dependsOn(provider)
                                        }
                                }
                        }
                }
            }
        }
    }

    private fun DetektAndroidExtension.matchesIgnoredConfiguration(variant: BaseVariant): Boolean =
        ignoredVariants.contains(variant.name) ||
                ignoredBuildTypes.contains(variant.buildType.name) ||
                ignoredFlavors.contains(variant.flavorName)

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

    companion object {
        const val DETEKT_ANDROID_EXTENSION = "detektAndroid"
    }
}
