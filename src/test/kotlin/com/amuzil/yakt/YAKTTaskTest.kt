/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt

import com.amuzil.yakt.data.Repository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.be
import io.kotest.matchers.should

class YAKTTaskTest :
	BehaviorSpec({
		data class RepositoryTestData(val url: String, val expectedRepository: Repository)

		Given("a valid GitHub repository URL") {
			val validUrlTestCases =
				mapOf(
					"SSH" to
						RepositoryTestData(
							"git@github.com:owner/name.git",
							Repository("github.com", "owner", "name")
						),
					"HTTPS" to
						RepositoryTestData(
							"https://github.com/owner/name.git",
							Repository("github.com", "owner", "name")
						)
				)

			withData(validUrlTestCases) { (url, expectedRepository) ->
				When("the URL is parsed") {
					val actualRepository = Repository.parse(url)

					Then("the owner and name should be correct") {
						actualRepository should be(expectedRepository)
					}
				}
			}
		}
	})
