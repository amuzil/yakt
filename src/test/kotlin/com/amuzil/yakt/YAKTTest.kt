/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.should
import io.kotest.matchers.types.beInstanceOf
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

fun Project.applyYAKT() {
	pluginManager.apply("com.amuzil.yakt")
}

class YAKTTest :
	BehaviorSpec({
		Given("a Gradle project") {
			val projectDir = tempdir()
			val project = ProjectBuilder.builder().withProjectDir(projectDir).build()

			When("the YAKT plugin is applied") {
				project.applyYAKT()

				Then("the yakt task should be created") {
					project.tasks.findByName("yakt") should beInstanceOf<YAKTTask>()
				}
			}
		}
	})
