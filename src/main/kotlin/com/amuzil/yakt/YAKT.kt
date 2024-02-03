/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt

import org.gradle.api.Plugin
import org.gradle.api.Project

// This is the entry point for the plugin
@Suppress("unused")
class YAKT : Plugin<Project> {
	override fun apply(target: Project) {
		target.tasks.register("yakt", YAKTTask::class.java)
	}
}
