package io.gitlab.arturbosch.detekt

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import io.gitlab.arturbosch.detekt.extensions.DetektAndroidExtension
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
    describe("detekt android plugin") {
        it("applies the base gradle plugin and creates a regular detekt task") {
            val project = ProjectBuilder.builder().build()

            with(project.pluginManager) {
                apply(DetektAndroidPlugin::class.java)
                apply(LifecycleBasePlugin::class.java)
            }

            Assertions.assertThat(project.getTask("check").dependencies()).contains("detekt")
        }

        it("lets the base gradle plugin use it's configuration") {
            val project = ProjectBuilder.builder().build()

            with(project.pluginManager) {
                apply(DetektAndroidPlugin::class.java)
                apply(LifecycleBasePlugin::class.java)
            }

            project.configureExtension<DetektAndroidExtension> {
                parallel = true
            }

            Assertions.assertThat((project.getTask("detekt") as Detekt).parallel).isTrue()
        }

        it("creates experimental tasks if the Android library plugin is present") {
            with(ProjectBuilder.builder().build()) {
                pluginSetup("com.android.library")

                configureExtension<LibraryExtension> {
                    compileSdkVersion(COMPILE_SDK_VERSION)
                }

                evaluate()

                Assertions.assertThat(getTask("detektMain").dependencies())
                    .containsExactlyInAnyOrder("detektDebug", "detektRelease")

                Assertions.assertThat(getTask("detektTest").dependencies())
                    .containsExactlyInAnyOrder(
                        "detektDebugUnitTest",
                        "detektReleaseUnitTest",
                        "detektDebugAndroidTest"
                    )
            }
        }

        it("creates experimental tasks if the Android application plugin is present") {
            with(ProjectBuilder.builder().build()) {

                pluginSetup("com.android.application")

                configureExtension<AppExtension> {
                    compileSdkVersion(COMPILE_SDK_VERSION)
                }

                evaluate()

                Assertions.assertThat(getTask("detektMain").dependencies())
                    .containsExactlyInAnyOrder("detektDebug", "detektRelease")

                Assertions.assertThat(getTask("detektTest").dependencies())
                    .containsExactlyInAnyOrder(
                        "detektDebugUnitTest",
                        "detektReleaseUnitTest",
                        "detektDebugAndroidTest"
                    )
            }
        }

        it("creates experimental tasks if the Android test plugin is present") {
            with(ProjectBuilder.builder().build()) {
                ProjectBuilder.builder().withParent(this).withName("prod").build()

                pluginSetup("com.android.test")

                configureExtension<TestExtension> {
                    compileSdkVersion(COMPILE_SDK_VERSION)
                    targetProjectPath("prod")
                }

                evaluate()

                // Test extensions by default have only the debug variant configured
                Assertions.assertThat(getTask("detektMain").dependencies())
                    .containsExactlyInAnyOrder("detektDebug")

                // There is no test code for test modules, so we don't create a task for that either
                Assertions.assertThat(tasks.findByName("detektTest")).isNull()
            }
        }

        it("creates experimental tasks for different build variants") {
            with(ProjectBuilder.builder().build()) {

                pluginSetup("com.android.library")

                configureExtension<LibraryExtension> {
                    compileSdkVersion(COMPILE_SDK_VERSION)
                    flavorTestSetup()
                }

                evaluate()

                Assertions.assertThat(getTask("detektMain").dependencies())
                    .containsExactlyInAnyOrder(
                        "detektYoungHarryDebug",
                        "detektYoungHarryRelease",
                        "detektOldHarryDebug",
                        "detektOldHarryRelease",
                        "detektYoungSallyDebug",
                        "detektYoungSallyRelease",
                        "detektOldSallyDebug",
                        "detektOldSallyRelease"
                    )

                Assertions.assertThat(getTask("detektTest").dependencies())
                    .containsExactlyInAnyOrder(
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

        it("creates experimental tasks for different build variants excluding ignored variants") {
            with(ProjectBuilder.builder().build()) {

                pluginSetup("com.android.library")

                configureExtension<LibraryExtension> {
                    compileSdkVersion(COMPILE_SDK_VERSION)
                    flavorTestSetup()
                }

                configureExtension<DetektAndroidExtension> {
                    ignoredVariants = listOf("youngHarryDebug", "oldSallyRelease")
                }

                evaluate()

                Assertions.assertThat(getTask("detektMain").dependencies())
                    .containsExactlyInAnyOrder(
                        "detektYoungHarryRelease",
                        "detektOldHarryDebug",
                        "detektOldHarryRelease",
                        "detektYoungSallyDebug",
                        "detektYoungSallyRelease",
                        "detektOldSallyDebug"
                    )

                Assertions.assertThat(getTask("detektTest").dependencies())
                    .containsExactlyInAnyOrder(
                        // unit tests
                        "detektYoungHarryReleaseUnitTest",
                        "detektOldHarryDebugUnitTest",
                        "detektOldHarryReleaseUnitTest",
                        "detektYoungSallyDebugUnitTest",
                        "detektYoungSallyReleaseUnitTest",
                        "detektOldSallyDebugUnitTest",
                        // instrumentation tests
                        "detektOldHarryDebugAndroidTest",
                        "detektYoungSallyDebugAndroidTest",
                        "detektOldSallyDebugAndroidTest"
                    )
            }
        }

        it("creates experimental tasks for different build variants excluding ignored build types") {
            with(ProjectBuilder.builder().build()) {

                pluginSetup("com.android.library")

                configureExtension<LibraryExtension> {
                    compileSdkVersion(COMPILE_SDK_VERSION)
                    flavorTestSetup()
                }

                configureExtension<DetektAndroidExtension> {
                    ignoredBuildTypes = listOf("release")
                }

                evaluate()

                Assertions.assertThat(getTask("detektMain").dependencies())
                    .containsExactlyInAnyOrder(
                        "detektYoungHarryDebug",
                        "detektOldHarryDebug",
                        "detektYoungSallyDebug",
                        "detektOldSallyDebug"
                    )

                Assertions.assertThat(getTask("detektTest").dependencies())
                    .containsExactlyInAnyOrder(
                        // unit tests
                        "detektYoungHarryDebugUnitTest",
                        "detektOldHarryDebugUnitTest",
                        "detektYoungSallyDebugUnitTest",
                        "detektOldSallyDebugUnitTest",
                        // instrumentation tests
                        "detektYoungHarryDebugAndroidTest",
                        "detektOldHarryDebugAndroidTest",
                        "detektYoungSallyDebugAndroidTest",
                        "detektOldSallyDebugAndroidTest"
                    )
            }
        }

        it("creates no tasks if all build types are ignored") {
            with(ProjectBuilder.builder().build()) {

                pluginSetup("com.android.library")

                configureExtension<LibraryExtension> {
                    compileSdkVersion(COMPILE_SDK_VERSION)
                    flavorTestSetup()
                }

                configureExtension<DetektAndroidExtension> {
                    ignoredBuildTypes = listOf("debug", "release")
                }

                evaluate()

                Assertions.assertThat(tasks.findByName("detektMain")).isNull()
                Assertions.assertThat(tasks.findByName("detektTest")).isNull()
            }
        }

        it("creates experimental tasks for different build variants excluding ignored flavors") {
            with(ProjectBuilder.builder().build()) {

                pluginSetup("com.android.library")

                configureExtension<LibraryExtension> {
                    compileSdkVersion(COMPILE_SDK_VERSION)
                    flavorTestSetup()
                }

                configureExtension<DetektAndroidExtension> {
                    ignoredFlavors = listOf("oldHarry", "youngSally")
                }

                evaluate()

                Assertions.assertThat(getTask("detektMain").dependencies())
                    .containsExactlyInAnyOrder(
                        "detektYoungHarryDebug",
                        "detektYoungHarryRelease",
                        "detektOldSallyDebug",
                        "detektOldSallyRelease"
                    )

                Assertions.assertThat(getTask("detektTest").dependencies())
                    .containsExactlyInAnyOrder(
                        // unit tests
                        "detektYoungHarryDebugUnitTest",
                        "detektYoungHarryReleaseUnitTest",
                        "detektOldSallyDebugUnitTest",
                        "detektOldSallyReleaseUnitTest",
                        // instrumentation tests
                        "detektYoungHarryDebugAndroidTest",
                        "detektOldSallyDebugAndroidTest"
                    )
            }
        }
    }
})

internal const val COMPILE_SDK_VERSION = 29

internal fun Task.dependencies() = taskDependencies.getDependencies(this).map { it.name }

internal fun Project.pluginSetup(androidPlugin: String) {
    with(project.pluginManager) {
        apply(DetektAndroidPlugin::class.java)
        apply(androidPlugin)
        apply("kotlin-android")
    }
}

internal fun Project.getTask(name: String) = project.tasks.getAt(name)

internal fun Project.evaluate() = (this as ProjectInternal).evaluate()

internal fun BaseExtension.flavorTestSetup() {
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

internal inline fun <reified T : Any> Project.configureExtension(configuration: T.() -> Unit = {}) {
    project.extensions.findByType(T::class.java)?.apply(configuration)
}
