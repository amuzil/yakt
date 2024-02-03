/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt

import com.amuzil.yakt.data.Repository
import com.amuzil.yakt.data.SemanticVersion
import com.amuzil.yakt.github.Tag
import com.amuzil.yakt.github.changelogVersions
import com.amuzil.yakt.github.createGitHubClient
import com.amuzil.yakt.github.listRepositoryTags
import kotlinx.coroutines.runBlocking
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class YAKTTask : DefaultTask() {
	init {
		group = "documentation"
		description = "Generate a changelog from a GitHub repository"
		// TODO: Optimise
		outputs.upToDateWhen { false }
	}

	@get:OutputFile abstract val destination: RegularFileProperty

	@get:Nested abstract val scraping: Scraping

	// Used by implementations
	@Suppress("unused")
	fun scraping(configureAction: Action<Scraping>) {
		configureAction.execute(scraping)
	}

	init {
		// Gradle works in mysterious ways
		@Suppress("LeakingThis") setDefaults()
	}

	private fun setDefaults() {
		destination.convention(project.layout.buildDirectory.file("changelog.md"))
		// Get the project's git origin url

		scraping.url.convention(
			project.provider {
				Runtime.getRuntime()
					.exec("git config --get remote.origin.url")
					.inputStream
					.bufferedReader()
					.readText()
					.trim()
			},
		)
		scraping.tagPrefix.convention("")
		scraping.type.convention(Scraping.Type.PULL_REQUESTS)
	}

	object Constants {
		const val HEADER_INDENT = "## "
		const val UNRELEASED_HEADER = "${HEADER_INDENT}Unreleased"
	}

	private fun getTags(): List<SemanticVersion> =
		// TODO: Possibility for performance improvement:
		//  don't fetch all tags, only the ones we need
		scraping.tagPrefix.get().let { prefix ->
			Runtime.getRuntime()
				.exec("git tag --list")
				.inputStream
				.bufferedReader()
				.readLines()
				.asSequence()
				.filter { it.startsWith(prefix) }
				.map { it.removePrefix(prefix) }
				.map { SemanticVersion.parse(it) }
				.sortedDescending()
				.distinctBy { it.core }
				.toList()
		}

	private fun removeExistingTags(
		changelog: List<String>,
		tags: Map<SemanticVersion, Tag>
	): Map<SemanticVersion, Tag> {
		with(
			changelog
				.asSequence()
				// Format: ## [0.0.0](url) (YYYY-MM-DD)
				.filter { it.startsWith(Constants.HEADER_INDENT) }
				.filter { it != Constants.UNRELEASED_HEADER }
				.map { it.substringAfter("${Constants.HEADER_INDENT}[").substringBefore("](") }
				.map { SemanticVersion.parse(it) }
				.toList()
		) {
			return tags.filterKeys { !contains(it) }
		}
	}

	@TaskAction
	fun execute() {
		runBlocking {
			val file = destination.get().asFile

			val repository = Repository.parse(scraping.url.get())
			val client = createGitHubClient()

			val changelogVersions =
				client
					.listRepositoryTags(repository)
					.await()
					.changelogVersions(scraping.tagPrefix.get())

			val tags =
				if (file.exists()) {
					removeExistingTags(file.readLines(), changelogVersions)
				} else {
					file.parentFile.mkdirs()
					file.writeText("# Changelog of ${repository.owner}/${repository.name}\n")

					changelogVersions
				}

			val existingLines = file.readLines().toMutableList()

			if (existingLines.contains(Constants.UNRELEASED_HEADER)) {
				val startIndex = existingLines.indexOf(Constants.UNRELEASED_HEADER)
				val endIndex =
					existingLines.subList(startIndex, existingLines.size).indexOfFirst {
						it.startsWith(Constants.HEADER_INDENT)
					}
				existingLines.subList(startIndex, startIndex + endIndex).clear()
			}

			println("Existing lines: $existingLines")

			val header = existingLines.takeWhile { !it.startsWith(Constants.HEADER_INDENT) }
			val unreleased = buildString {
				appendLine()
				appendLine(Constants.UNRELEASED_HEADER)
				appendLine()
				appendLine("Content")
			}
			val newLines =
				tags.map { (version, _) ->
					val versionString = version.toVersionString(scraping.tagPrefix.get())
					buildString {
						val headerVersion =
							"[$versionString](${repository.url}/releases/tag/$versionString)"
						appendLine("${Constants.HEADER_INDENT}$headerVersion (date)")
						appendLine()
						appendLine("Content")
					}
				}
			val footer = existingLines.dropWhile { !it.startsWith(Constants.HEADER_INDENT) }

			val text = (header + unreleased + newLines + footer).joinToString("\n").trimEnd() + "\n"
			println("Writing text: ${text.replace("\n", "\\n")}")
			file.writeText(text)
		}
	}

	interface Scraping {
		@get:Input val url: Property<String>
		@get:Input val tagPrefix: Property<String>
		@get:Input val type: Property<Type>

		enum class Type {
			PULL_REQUESTS
		}
	}
}
