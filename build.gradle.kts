/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
import com.fasterxml.jackson.databind.ObjectMapper
import io.gitlab.arturbosch.detekt.Detekt
import kotlinx.kover.api.KoverPaths
import org.gradle.internal.os.OperatingSystem

buildscript {
	repositories { mavenCentral() }

	dependencies { classpath(explainLibs.jackson.core) }
}

/*
Kotlin will warn us that our version catalogs can't be called by the implicit receiver.
This is, however, not the case, as we can call them just fine, so we suppress those warnings.
*/
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
	// build
	kotlin("jvm") version buildLibs.versions.kotlin.get()
	kotlin("plugin.serialization") version buildLibs.versions.kotlin.get()
	`java-gradle-plugin`
	alias(buildLibs.plugins.shadow)
	// check
	pmd
	alias(checkLibs.plugins.detekt)
	alias(checkLibs.plugins.kover)
	alias(checkLibs.plugins.sonarqube)
	alias(checkLibs.plugins.spotless)
	// explain
	alias(explainLibs.plugins.dokka)
	// publish
	alias(publishLibs.plugins.pluginPublish)
	signing
}

val printOrigin by
	tasks.registering {
		val origin =
			Runtime.getRuntime()
				.exec("git config --get remote.origin.url")
				.inputStream
				.bufferedReader()
				.readText()
				.trim()

		println("Origin: $origin")
	}

/* * * * * *
 * GENERAL *
 * * * * * */

group = "com.amuzil.yakt"

version = "1.0.0"

description = "Automatically load configuration values from Forge's mods.toml."

/* * * * *
 * BUILD *
 * * * * */

repositories {
	mavenCentral()
	maven("https://jitpack.io")
}

dependencies {
	compileOnly(gradleApi())

	// TODO why isn't shadow working?
	shadow(buildLibs.bundles.ktor)

	testImplementation(checkLibs.bundles.kotest)
	testImplementation(checkLibs.mockk)
	testImplementation(gradleTestKit())
}

kotlin { jvmToolchain(buildLibs.versions.java.get().toInt()) }

tasks.jar { archiveClassifier.set("thin") }

tasks.shadowJar {
	archiveClassifier.set("")

	configurations = listOf(project.configurations.shadow.get())
	minimize()
}

tasks.build { dependsOn(tasks.shadowJar) }

/* * * * *
 * CHECK *
 * * * * */

tasks.withType<Test> {
	useJUnitPlatform()
	reports { junitXml.required.set(false) }
	systemProperty("gradle.build.dir", project.buildDir)
}

detekt {
	parallel = true
	buildUponDefaultConfig = true
	config = files(".detekt.yaml")
}

tasks.withType<Detekt> {
	basePath = rootProject.projectDir.absolutePath

	reports { xml.required.set(true) }
}

sonarqube {
	properties {
		property("sonar.projectKey", "amuzil_yakt")
		property("sonar.organization", "amuzil")
		property("sonar.host.url", "https://sonarcloud.io")

		property("sonar.kotlin.file.suffixes", ".kt,.kts")

		// Analyse all files, including Kotlin (Gradle) scripts
		property("sonar.sources", ".")
		property("sonar.inclusions", "src/main/**/*, src/generated/**/*, *.kts")
		property("sonar.coverage.exclusions", "*.gradle.kts")

		// Other linters
		property("sonar.junit.reportPaths", file("build/test-results/test").absolutePath)
		property(
			"sonar.java.pmd.reportPaths",
			listOf(tasks.pmdMain, tasks.pmdTest).joinToString(",") {
				it.get().reports.xml.outputLocation.get().asFile.path
			}
		)
		property(
			"sonar.kotlin.detekt.reportPaths",
			listOf(tasks.detekt).joinToString(",") { it.get().xmlReportFile.get().asFile.path }
		)
		property(
			"sonar.coverage.jacoco.xmlReportPaths",
			"${buildDir}/${KoverPaths.PROJECT_XML_REPORT_DEFAULT_PATH}"
		)
	}
}

tasks.sonar {
	dependsOn(tasks.pmdMain)
	dependsOn(tasks.pmdTest)
	dependsOn(tasks.detekt)
	dependsOn(tasks.koverXmlReport)
}

val lint by
	tasks.registering(Task::class) {
		group = "verification"
		description =
			"Runs all code quality checks. Requires the SONAR_TOKEN environment variable to be set."

		dependsOn(tasks.sonar)
	}

spotless {
	// Load from file and replace placeholders
	val licenseHeaderKt =
		file("license-header.kt").readText().replace("\$AUTHORS", "Mahtaran & the Amuzil community")
	val prettierConfig = ".prettierrc"

	ratchetFrom("origin/main")

	kotlin {
		ktfmt().kotlinlangStyle()
		indentWithTabs(4)
		licenseHeader(licenseHeaderKt).yearSeparator("–")
		toggleOffOn()
	}

	kotlinGradle {
		ktfmt().kotlinlangStyle()
		indentWithTabs(4)
		licenseHeader(licenseHeaderKt, "(pluginManagement |import )").yearSeparator("–")
		toggleOffOn()
	}

	java {
		palantirJavaFormat()
		indentWithTabs(4)
		licenseHeader(licenseHeaderKt).yearSeparator("–")
		toggleOffOn()
	}

	json {
		target("src/**/*.json", prettierConfig)

		prettier(mapOf("prettier" to checkLibs.versions.prettier.asProvider().get()))
			.configFile(prettierConfig)
	}

	format("toml") {
		target("**/*.toml")

		prettier(
				mapOf(
					"prettier" to checkLibs.versions.prettier.asProvider().get(),
					"prettier-plugin-toml" to checkLibs.versions.prettier.toml.get()
				)
			)
			.configFile(prettierConfig)
	}

	format("markdown") {
		target("**/*.md")

		prettier(mapOf("prettier" to checkLibs.versions.prettier.asProvider().get()))
			.configFile(prettierConfig)
			.config(mapOf("tabWidth" to 2, "useTabs" to false))
	}

	format("yaml") {
		target("**/*.yml", "**/*.yaml")

		prettier(mapOf("prettier" to checkLibs.versions.prettier.asProvider().get()))
			.configFile(prettierConfig)
			.config(mapOf("tabWidth" to 2, "useTabs" to false))
	}
}

val format by
	tasks.registering(Task::class) {
		group = "verification"
		description = "Runs the formatter on the project"

		dependsOn(tasks.spotlessApply)
	}

/* * * * * *
 * EXPLAIN *
 * * * * * */

val changelog by
	tasks.registering(Exec::class) {
		group = "changelog"
		description = "Generates a changelog for the current version. Requires PNPM"

		workingDir = project.rootDir

		ObjectMapper()
			.writeValue(
				file(".gitmoji-changelogrc"),
				mapOf(
					"project" to
						mapOf(
							"name" to "ForgeKonfig",
							"description" to project.description,
							"version" to project.version
						)
				)
			)

		val command =
			listOf(
				// spotless:off
                "pnpx", "gitmoji-changelog",
                "--format", "markdown",
                "--preset", "generic",
                "--output", "changelog.md",
                "--group-similar-commits", "true",
                "--author", "true"
                // spotless:on
			)

		with(OperatingSystem.current()) {
			when {
				isWindows -> commandLine(listOf("cmd", "/c") + command)
				isLinux -> commandLine(command)
				else -> throw IllegalStateException("Unsupported operating system: $this")
			}
		}

		finalizedBy("spotlessMarkdownApply")
	}

val dokkaJar by
	tasks.registering(Jar::class) {
		group = "documentation"
		description = "Generates the documentation as Dokka HTML"

		dependsOn(tasks.dokkaHtml)

		onlyIf { !tasks.dokkaHtml.get().state.upToDate }

		from(tasks.dokkaHtml.get().outputDirectory)

		archiveClassifier.set("dokka")
	}

val javadocJar by
	tasks.registering(Jar::class) {
		group = "documentation"
		description = "Generates the documentation as Javadoc HTML"

		dependsOn(tasks.dokkaJavadoc)

		onlyIf { !tasks.dokkaJavadoc.get().state.upToDate }

		from(tasks.dokkaJavadoc.get().outputDirectory)

		archiveClassifier.set("javadoc")
	}

val buildWithDocs by
	tasks.registering(Task::class) {
		group = "documentation"
		description = "Builds the project and generates the documentation"

		dependsOn(tasks.build)
		dependsOn(tasks.kotlinSourcesJar)
		dependsOn(dokkaJar)
		dependsOn(javadocJar)
	}

/* * * * * *
 * PUBLISH *
 * * * * * */

gradlePlugin {
	website.set("https://docs.amuzil.com/yakt")
	vcsUrl.set("https://github.com/amuzil/yakt")
	plugins {
		create("yakt") {
			id = "com.amuzil.yakt"
			implementationClass = "com.amuzil.yakt.YAKT"
			displayName = "YAKT"
			description = project.description
			tags.set(
				listOf("minecraft", "forge", "minecraftforge", "mod", "config", "configuration")
			)
		}
	}
}
