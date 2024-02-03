/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.be
import io.kotest.matchers.should

class RepositoryTest :
	BehaviorSpec({
		data class ParseData(val url: String, val expectedRepository: Repository)

		Given("a valid GitHub repository URL") {
			val validUrlTestCases =
				mapOf(
					"SSH" to
						ParseData(
							"git@github.com:owner/name.git",
							Repository("github.com", "owner", "name")
						),
					"HTTPS" to
						ParseData(
							"https://github.com/owner/name.git",
							Repository("github.com", "owner", "name")
						)
				)

			for ((type, testData) in validUrlTestCases) {
				When("the $type URL is parsed") {
					val (url, expectedRepository) = testData
					val actualRepository = Repository.parse(url)

					Then("the owner and name should be correct") {
						actualRepository should be(expectedRepository)
					}
				}
			}
		}

		Given("an invalid GitHub repository URL") {
			val invalidUrlTestCases =
				mapOf(
					"SSH (wrong user)" to "user@github.com:owner/name.git",
					"SSH (wrong delimiter)" to "git@github.com/owner/name.git",
					"SSH (missing owner)" to "git@github.com/name.git",
					"SSH (missing name)" to "git@github.com:owner.git",
					"SSH (extra part)" to "git@github.com:owner/name/sub.git",
					"SSH (no extension)" to "git@github.com:owner/name",
					"SSH (wrong extension)" to "git@github.com:owner/name.gi",
					"HTTPS (non-SSL)" to
						@Suppress("HttpUrlsUsage") "http://github.com/owner/name.git",
					"HTTPS (missing part)" to "https://github.com/owner.git",
					"HTTPS (extra part)" to "https://github.com/owner/name/sub.git",
					"HTTPS (no extension)" to "https://github.com/owner/name",
					"HTTPS (wrong extension)" to "https://github.com/owner/name.gi"
				)

			for ((type, url) in invalidUrlTestCases) {
				When("the $type URL is parsed") {
					Then("an exception should be thrown") {
						shouldThrow<IllegalArgumentException> { Repository.parse(url) }
					}
				}
			}
		}
	})
