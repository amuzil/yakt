/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt.data

import org.intellij.lang.annotations.Language

data class SemanticVersion(
	val major: Int,
	val minor: Int,
	val patch: Int,
	val preRelease: String? = null,
	val buildMetadata: String? = null
) : Comparable<SemanticVersion> {
	fun toVersionString(prefix: String = ""): String {
		val preReleaseString = preRelease?.let { "-$it" } ?: ""
		val buildMetadataString = buildMetadata?.let { "+$it" } ?: ""
		return "$prefix$major.$minor.$patch$preReleaseString$buildMetadataString"
	}

	val core: SemanticVersion
		get() = copy(preRelease = null, buildMetadata = null)

	override fun compareTo(other: SemanticVersion): Int {
		// Reference: https://semver.org/#spec-item-11
		return when {
			major != other.major -> major compareTo other.major
			minor != other.minor -> minor compareTo other.minor
			patch != other.patch -> patch compareTo other.patch
			else -> comparePreReleases(other.preRelease)
		}
	}

	private fun comparePreReleases(otherPreRelease: String?): Int {
		// Reference: https://semver.org/#spec-item-11
		return when {
			preRelease == null && otherPreRelease == null -> 0
			preRelease == null -> 1
			otherPreRelease == null -> -1
			else -> comparePreReleases(preRelease, otherPreRelease)
		}
	}

	companion object {
		private val REGEX by lazy {
			@Language("RegExp") val integer = """0|[1-9]\d*"""
			@Language("RegExp") val alphanumeric = """[0-9A-Za-z-]"""
			@Language("RegExp") val label = """\d*[A-Za-z-]$alphanumeric*"""
			@Language("RegExp") val identifier = """(?:$integer|$label)"""
			@Language("RegExp") val preRelease = """(?:-($identifier(?:\.$identifier)*))?"""
			@Language("RegExp")
			val buildMetadata = """(?:\+($alphanumeric+(?:\.$alphanumeric+)*))?"""

			Regex("""^($integer)\.($integer)\.($integer)$preRelease$buildMetadata$""")
		}

		fun parse(string: String): SemanticVersion {
			val match =
				REGEX.matchEntire(string)
					?: throw IllegalArgumentException("Invalid semantic version: $string")

			val (major, minor, patch, preRelease, buildMetadata) = match.destructured
			return SemanticVersion(
				major.toInt(),
				minor.toInt(),
				patch.toInt(),
				preRelease.takeIf { it.isNotEmpty() },
				buildMetadata.takeIf { it.isNotEmpty() }
			)
		}

		private fun comparePreReleases(ourPreRelease: String, otherPreRelease: String): Int {
			// Reference: https://semver.org/#spec-item-11
			val ourParts = ourPreRelease.split('.')
			val otherParts = otherPreRelease.split('.')

			for (index in 0 until minOf(ourParts.size, otherParts.size)) {
				val ourPart = ourParts[index]
				val otherPart = otherParts[index]

				if (ourPart != otherPart) {
					return comparePreReleaseParts(ourPart, otherPart)
				}
			}

			// A larger set of pre-release fields has a higher precedence than a smaller set,
			// if all the preceding identifiers are equal.
			return ourParts.size compareTo otherParts.size
		}

		private fun comparePreReleaseParts(ourPart: String, otherPart: String): Int {
			// Reference: https://semver.org/#spec-item-11
			val numericRegex = Regex("\\d+")
			val ourNumeric = numericRegex.matches(ourPart)
			val otherNumeric = numericRegex.matches(otherPart)

			return when {
				// Identifiers consisting of only digits are compared numerically.
				ourNumeric && otherNumeric -> ourPart.toInt() compareTo otherPart.toInt()
				// Numeric identifiers always have lower precedence than non-numeric identifiers.
				ourNumeric && !otherNumeric -> -1
				!ourNumeric && otherNumeric -> 1
				// Identifiers with letters or hyphens are compared lexically in ASCII sort order.
				else -> ourPart compareTo otherPart
			}
		}
	}
}
