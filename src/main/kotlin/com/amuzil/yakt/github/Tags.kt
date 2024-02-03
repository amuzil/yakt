/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt.github

import com.amuzil.yakt.data.Repository
import com.amuzil.yakt.data.SemanticVersion
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun HttpClient.listRepositoryTags(repository: Repository) = async {
	gitHubGetPaged<Tags>("/repos/${repository.owner}/${repository.name}/tags").await()
}

typealias Tags = List<Tag>

fun Tags.changelogVersions(prefix: String = "") =
	asSequence()
		.filter { it.name.startsWith(prefix) }
		.map { SemanticVersion.parse(it.name.removePrefix(prefix)) to it }
		.sortedByDescending { it.first }
		.distinctBy { it.first.core }
		.associate { it.first to it.second }

@Serializable
data class Tag(
	val name: String,
	val commit: Commit,
	@SerialName("zipball_url") val zipballUrl: String,
	@SerialName("tarball_url") val tarballUrl: String,
	@SerialName("node_id") val nodeId: String
) {
	fun version(prefix: String = "") = SemanticVersion.parse(name.removePrefix(prefix))
}

@Serializable data class Commit(val sha: String, val url: String)

fun main() {
	val client = createGitHubClient()
	runBlocking {
		client
			.listRepositoryTags(Repository("github.com", "amuzil", "yakt-demo"))
			.await()
			.forEach(::println)
	}
}
