package io.gitlab.arturbosch.detekt.internal

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

fun Project.registerDetektTask(
    name: String,
    extension: DetektExtension,
    configuration: Detekt.() -> Unit
): TaskProvider<Detekt> =
    tasks.register(name, Detekt::class.java) {
        it.debugProp.set(provider { extension.debug })
        it.parallelProp.set(provider { extension.parallel })
        it.disableDefaultRuleSetsProp.set(provider { extension.disableDefaultRuleSets })
        it.buildUponDefaultConfigProp.set(provider { extension.buildUponDefaultConfig })
        it.failFastProp.set(provider { extension.failFast })
        it.autoCorrectProp.set(provider { extension.autoCorrect })
        it.config.setFrom(provider { extension.config })
        it.baseline.set(layout.file(project.provider { extension.baseline }))
        it.ignoreFailuresProp.set(project.provider { extension.ignoreFailures })
        configuration(it)
    }
