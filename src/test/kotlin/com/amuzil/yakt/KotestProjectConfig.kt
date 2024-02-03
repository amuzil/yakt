/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.junitxml.JunitXmlReporter

// Automatically detected
@Suppress("unused")
object KotestProjectConfig : AbstractProjectConfig() {
	init {
		println("Detected Project Config")
	}

	override fun extensions() = listOf(JunitXmlReporter())
}
