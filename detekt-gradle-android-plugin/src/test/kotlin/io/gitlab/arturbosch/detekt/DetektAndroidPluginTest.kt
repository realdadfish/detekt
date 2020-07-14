package io.gitlab.arturbosch.detekt

import org.assertj.core.api.Assertions
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object DetektAndroidPluginTest : Spek({
    describe("detekt Android plugin") {
        fun Task.dependencies() = taskDependencies.getDependencies(this).map { it.name }

        fun Project.getTask(name: String) = project.tasks.getAt(name)

        it("applies the base gradle plugin and creates a regular detekt task") {
            val project = ProjectBuilder.builder().build()

            project.pluginManager.apply(DetektAndroidPlugin::class.java)
            project.pluginManager.apply(LifecycleBasePlugin::class.java)

            Assertions.assertThat(project.getTask("check").dependencies()).contains("detekt")
        }

        it("creates experimental tasks if the Android library plugin is present") {
            val project = ProjectBuilder.builder().build()

            project.pluginManager.apply(DetektAndroidPlugin::class.java)
            project.pluginManager.apply("com.android.library")

            Assertions.assertThat(project.getTask("detektMain").dependencies())
                .contains("detektDebug", "detektRelease")

            Assertions.assertThat(project.getTask("detektTest").dependencies())
                .contains("detektDebugUnitTest")
        }

        it("creates experimental tasks if the Android application plugin is present") {
            TODO()
        }
    }
})
