/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.be
import io.kotest.matchers.should

class SemanticVersionTest :
	BehaviorSpec({
		data class ParseData(val version: String, val expectedVersion: SemanticVersion)

		data class CompareData(
			val version1: SemanticVersion,
			val version2: SemanticVersion,
			val expectedComparison: Int
		) {
			constructor(
				version1: String,
				version2: String,
				equal: Boolean = false
			) : this(
				SemanticVersion.parse(version1),
				SemanticVersion.parse(version2),
				if (equal) 0 else 1
			)
		}

		Given("a valid semantic version") {
			val validVersionTestCases =
				mapOf(
					"major" to ParseData("1.0.0", SemanticVersion(1, 0, 0)),
					"minor" to ParseData("0.1.0", SemanticVersion(0, 1, 0)),
					"patch" to ParseData("0.0.1", SemanticVersion(0, 0, 1)),
					"major and minor" to ParseData("1.2.0", SemanticVersion(1, 2, 0)),
					"major and patch" to ParseData("1.0.2", SemanticVersion(1, 0, 2)),
					"minor and patch" to ParseData("0.1.2", SemanticVersion(0, 1, 2)),
					"major, minor and patch" to ParseData("1.2.3", SemanticVersion(1, 2, 3)),
					"major, minor, patch and preRelease" to
						ParseData("1.2.3-alpha", SemanticVersion(1, 2, 3, "alpha")),
					"major, minor, patch, numbered preRelease" to
						ParseData("1.2.3-alpha.1", SemanticVersion(1, 2, 3, "alpha.1")),
					"major, minor, patch, preRelease and build" to
						ParseData("1.2.3-alpha+build", SemanticVersion(1, 2, 3, "alpha", "build"))
				)

			for ((type, testData) in validVersionTestCases) {
				When("the $type version is parsed") {
					val (version, expectedVersion) = testData
					val actualVersion = SemanticVersion.parse(version)

					Then("the major, minor and patch should be correct") {
						actualVersion should be(expectedVersion)
					}
				}
			}
		}

		Given("an invalid semantic version") {
			val invalidVersionTestCases =
				mapOf(
					"alphabetic major" to "a.0.0",
					"alphabetic minor" to "0.a.0",
					"alphabetic patch" to "0.0.a",
					"missing major" to ".0.0",
					"missing minor" to "0..0",
					"missing patch" to "0.0.",
					"missing major and minor" to ".0.",
					"missing minor and patch" to "0..",
					"missing major and patch" to ".0.",
					"missing major, minor and patch" to "...",
					"one missing segment" to "0.0",
					"two missing segments" to "0",
					"empty" to "",
					"only preRelease" to "-alpha",
					"only build" to "+build",
					"only preRelease and build" to "-alpha+build",
				)

			for ((type, version) in invalidVersionTestCases) {
				When("the $type version is parsed") {
					Then("an exception should be thrown") {
						shouldThrow<IllegalArgumentException> { SemanticVersion.parse(version) }
					}
				}
			}
		}

		Given("two valid semantic versions") {
			val versionComparisons =
				mapOf(
					"major" to CompareData("1.0.0", "0.0.0"),
					"minor" to CompareData("0.1.0", "0.0.0"),
					"patch" to CompareData("0.0.1", "0.0.0"),
					"major and minor" to CompareData("1.2.0", "1.1.0"),
					"major and patch" to CompareData("1.0.2", "1.0.1"),
					"minor and patch" to CompareData("0.1.2", "0.1.1"),
					"major, minor and patch" to CompareData("1.2.3", "1.2.2"),
					"preRelease" to CompareData("1.2.3", "1.2.3-alpha"),
					"numbered preRelease" to CompareData("1.2.3-alpha.2", "1.2.3-alpha.1"),
					"longer preRelease" to CompareData("1.2.3-alpha.1", "1.2.3-alpha"),
					"alphabetic preRelease" to CompareData("1.2.3-beta", "1.2.3-alpha"),
					"preRelease and build" to CompareData("1.2.3-alpha+build", "1.2.3-alpha", true)
				)

			for ((type, testData) in versionComparisons) {
				When("the $type versions are compared") {
					val (version1, version2, expectedComparison) = testData
					val actualComparison = version1 compareTo version2
					val actualInverseComparison = version2 compareTo version1

					Then("the comparison should be correct") {
						withClue(
							"${version1.toVersionString()} compared to ${version2.toVersionString()}"
						) {
							actualComparison should be(expectedComparison)
							actualInverseComparison should be(-expectedComparison)
						}
					}
				}
			}
		}
	})
