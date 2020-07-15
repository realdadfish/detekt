package io.gitlab.arturbosch.detekt.extensions

import org.gradle.api.Project

open class DetektAndroidExtension(project: Project) : DetektExtension(project) {

    /**
     * List build variants for which no detekt task should be created.
     *
     * This is a combination of build types and flavors, such as fooDebug or barRelease.
     */
    var ignoredVariants: List<String> = emptyList()

    /**
     * List build types for which no detekt task should be created.
     */
    var ignoredBuildTypes: List<String> = emptyList()

    /**
     * List build flavors for which no detekt task should be created
     */
    var ignoredFlavors: List<String> = emptyList()
}
