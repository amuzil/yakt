/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
import java.net.URL

dependencyResolutionManagement {
	versionCatalogs {
		create("buildLibs") {
			from(
				files(
					"gradle/build.versions.toml",
				)
			)
		}
		create("checkLibs") {
			from(
				files(
					"gradle/check.versions.toml",
				)
			)
		}
		create("explainLibs") {
			from(
				files(
					"gradle/explain.versions.toml",
				)
			)
		}
		create("publishLibs") {
			from(
				files(
					"gradle/publish.versions.toml",
				)
			)
		}
	}
}

rootProject.name = "yakt"

plugins { id("org.danilopianini.gradle-pre-commit-git-hooks") version "1.1.1" }

gitHooks {
	preCommit {
		from {
			"""
			./gradlew check
			"""
				.trimIndent()
		}
	}

	commitMsg {
		from {
			URL(
					"https://gist.githubusercontent.com/mahtaran/b202b92a26fdd52c78197e7373cb3a91/raw/amuzil-commit-msg-git-hook.sh"
				)
				.readText()
		}
	}

	createHooks(true)
}
