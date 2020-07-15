package io.gitlab.arturbosch.detekt

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import org.assertj.core.api.Assertions
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object DetektAndroidPluginTest : Spek({
    describe("detekt Android plugin") {
        it("applies the base gradle plugin and creates a regular detekt task") {
            val project = ProjectBuilder.builder().build()

            with(project.pluginManager) {
                apply(DetektAndroidPlugin::class.java)
                apply(LifecycleBasePlugin::class.java)
            }

            Assertions.assertThat(project.getTask("check").dependencies()).contains("detekt")
        }

        it("creates experimental tasks if the Android library plugin is present") {
            val project = ProjectBuilder.builder().build()

            with(project.pluginManager) {
                apply(DetektAndroidPlugin::class.java)
                apply("com.android.library")
                apply("kotlin-android")
            }

            project.configureAndroidPlugin<LibraryExtension>()

            project.evaluate()

            Assertions.assertThat(project.getTask("detektMain").dependencies())
                .contains("detektDebug", "detektRelease")

            Assertions.assertThat(project.getTask("detektTest").dependencies())
                .contains(
                    "detektDebugUnitTest",
                    "detektReleaseUnitTest",
                    "detektDebugAndroidTest"
                )
        }

        it("creates experimental tasks if the Android application plugin is present") {
            val project = ProjectBuilder.builder().build()

            with(project.pluginManager) {
                apply(DetektAndroidPlugin::class.java)
                apply("com.android.application")
                apply("kotlin-android")
            }

            project.configureAndroidPlugin<AppExtension>()

            project.evaluate()

            Assertions.assertThat(project.getTask("detektMain").dependencies())
                .contains("detektDebug", "detektRelease")

            Assertions.assertThat(project.getTask("detektTest").dependencies())
                .contains(
                    "detektDebugUnitTest",
                    "detektReleaseUnitTest",
                    "detektDebugAndroidTest"
                )
        }

        it("creates experimental tasks if the Android test plugin is present") {
            val testProject = ProjectBuilder.builder().build()
            ProjectBuilder.builder().withParent(testProject).withName("prod").build()

            with(testProject.pluginManager) {
                apply(DetektAndroidPlugin::class.java)
                apply("com.android.test")
                apply("kotlin-android")
            }

            testProject.configureAndroidPlugin<TestExtension> {
                targetProjectPath("prod")
            }

            testProject.evaluate()

            // Test extensions by default have only the debug variant configured
            Assertions.assertThat(testProject.getTask("detektMain").dependencies())
                .contains("detektDebug")

            // There is no test code for test code, the main task is still created for consistency
            Assertions.assertThat(testProject.getTask("detektTest").dependencies())
                .isEmpty()
        }

        it("creates experimental tasks for different build variants") {
            val project = ProjectBuilder.builder().build()

            with(project.pluginManager) {
                apply(DetektAndroidPlugin::class.java)
                apply("com.android.library")
                apply("kotlin-android")
            }

            project.configureAndroidPlugin<LibraryExtension> {
                flavorDimensions("age", "name")
                productFlavors(Action {
                    it.create("harry").apply {
                        dimension = "name"
                    }
                    it.create("sally").apply {
                        dimension = "name"
                    }
                    it.create("young").apply {
                        dimension = "age"
                    }
                    it.create("old").apply {
                        dimension = "age"
                    }
                })
            }

            project.evaluate()

            Assertions.assertThat(project.getTask("detektMain").dependencies())
                .contains(
                    "detektYoungHarryDebug",
                    "detektYoungHarryRelease",
                    "detektOldHarryDebug",
                    "detektOldHarryRelease",
                    "detektYoungSallyDebug",
                    "detektYoungSallyRelease",
                    "detektOldSallyDebug",
                    "detektOldSallyRelease"
                )

            Assertions.assertThat(project.getTask("detektTest").dependencies())
                .contains(
                    // unit tests
                    "detektYoungHarryDebugUnitTest",
                    "detektYoungHarryReleaseUnitTest",
                    "detektOldHarryDebugUnitTest",
                    "detektOldHarryReleaseUnitTest",
                    "detektYoungSallyDebugUnitTest",
                    "detektYoungSallyReleaseUnitTest",
                    "detektOldSallyDebugUnitTest",
                    "detektOldSallyReleaseUnitTest",
                    // instrumentation tests
                    "detektYoungHarryDebugAndroidTest",
                    "detektOldHarryDebugAndroidTest",
                    "detektYoungSallyDebugAndroidTest",
                    "detektOldSallyDebugAndroidTest"
                )
        }
    }
})

internal fun Task.dependencies() = taskDependencies.getDependencies(this).map { it.name }

internal fun Project.getTask(name: String) = project.tasks.getAt(name)

internal fun Project.evaluate() = (this as ProjectInternal).evaluate()

internal inline fun <reified T : BaseExtension> Project.configureAndroidPlugin(configuration: T.() -> Unit = {}) {
    project.extensions.findByType(T::class.java)?.apply {
        compileSdkVersion(29)
        configuration(this)
    }
}
