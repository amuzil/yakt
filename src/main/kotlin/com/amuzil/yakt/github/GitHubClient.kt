/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.yakt.github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.encodedPath
import io.ktor.http.parseQueryString
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

fun createGitHubClient() =
	HttpClient(Java) {
		engine {
			// TODO: Configure
		}
		install(ContentNegotiation) {
			json(
				Json {
					// ignoreUnknownKeys = true
					prettyPrint = true
					@OptIn(ExperimentalSerializationApi::class)
					prettyPrintIndent = "\t"
				}
			)
		}
	}

const val GITHUB_API_URL = "https://api.github.com"
const val GITHUB_API_ACCEPT_HEADER = "application/vnd.github+json"
const val GITHUB_API_VERSION = "2022-11-28"
// TODO: Remove
const val GITHUB_ACCESS_TOKEN =
	"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"

fun HttpClient.gitHubGet(path: String, vararg parameters: Pair<String, Any?>) = async {
	get {
		url {
			takeFrom(GITHUB_API_URL)
			encodedPath = path
		}
		header("Accept", GITHUB_API_ACCEPT_HEADER)
		// TODO
		//  val token = System.getenv("GITHUB_TOKEN")
		val token = GITHUB_ACCESS_TOKEN
		header("Authorization", "Header $token")
		header("X-GitHub-Api-Version", GITHUB_API_VERSION)

		parameters.forEach { (name, value) -> parameter(name, value) }
	}
}

inline fun <reified T : List<*>> HttpClient.gitHubGetPaged(
	path: String,
	vararg parameters: Pair<String, Any?>
): Deferred<T> = async {
	val getPage = { page: Int -> gitHubGet(path, *parameters, "per_page" to 100, "page" to page) }

	val response = mutableListOf<T>()

	val firstPage = getPage(1).await()
	response.add(firstPage.body())

	if (firstPage.headers["Link"] != null) {
		val headers =
			firstPage.headers["Link"]
				?.split(",")
				?.map { it.split(";") }
				?.map { (url, rel) -> url.trim().removeSurrounding("<", ">") to rel.trim() }

		val lastPageURL = headers?.find { it.second == "rel=\"last\"" }?.first

		val lastPageNumber =
			lastPageURL?.let { parseQueryString(it, it.indexOf("?") + 1) }?.get("page")?.toInt()
				?: error("Could not find last page number")

		(2..lastPageNumber)
			.map { page -> launch { response.add(getPage(page).await().body()) } }
			.joinAll()
	}

	response.flatten() as T
}
