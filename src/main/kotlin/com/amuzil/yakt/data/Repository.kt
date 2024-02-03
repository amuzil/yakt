/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt.data

import org.intellij.lang.annotations.Language

data class Repository(val host: String, val owner: String, val name: String) {
	val url: String
		get() = "https://$host/$owner/$name"

	companion object {
		private val URL_FORMATS by lazy {
			// Not completely accurate, but good enough
			@Language("RegExp") val hostRegex = ".+\\.[A-Za-z]{2,6}"
			@Language("RegExp") val part = "[\\w.-]+"

			listOf(
				// SSH: git@github.com:owner/name.git
				Regex("""^git@($hostRegex):($part)/($part)\.git$"""),
				// HTTPS: https://github.com/owner/name.git
				Regex("""^https://($hostRegex)/($part)/($part)\.git$""")
			)
		}

		fun parse(url: String): Repository {
			val match =
				URL_FORMATS.firstNotNullOfOrNull { it.matchEntire(url) }
					?: throw IllegalArgumentException("Invalid repository URL: $url")

			val (host, owner, name) = match.destructured
			return Repository(host, owner, name)
		}
	}
}
