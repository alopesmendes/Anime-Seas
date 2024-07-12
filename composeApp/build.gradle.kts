import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.jetbrainsCompose)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.detekt)
	alias(libs.plugins.ktlint)
}

kotlin {
	@OptIn(ExperimentalWasmDsl::class)
	wasmJs {
		moduleName = "composeApp"
		browser {
			commonWebpackConfig {
				outputFileName = "composeApp.js"
				devServer =
					(devServer ?: KotlinWebpackConfig.DevServer()).apply {
						static =
							(static ?: mutableListOf()).apply {
								// Serve sources to debug inside browser
								add(project.projectDir.path)
							}
					}
			}
		}
		binaries.executable()
	}

	androidTarget {
		@OptIn(ExperimentalKotlinGradlePluginApi::class)
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_11)
		}
	}

	jvm("desktop")

	listOf(
		iosX64(),
		iosArm64(),
		iosSimulatorArm64(),
	).forEach { iosTarget ->
		iosTarget.binaries.framework {
			baseName = "ComposeApp"
			isStatic = true
		}
	}

	sourceSets {
		val desktopMain by getting

		androidMain.dependencies {
			implementation(compose.preview)
			implementation(libs.androidx.activity.compose)
		}
		commonMain.dependencies {
			implementation(compose.runtime)
			implementation(compose.foundation)
			implementation(compose.material)
			implementation(compose.ui)
			implementation(compose.components.resources)
			implementation(compose.components.uiToolingPreview)
			// implementation(libs.detekt.formatting)
		}
		desktopMain.dependencies {
			implementation(compose.desktop.currentOs)
		}
	}
}

android {
	namespace = "org.ailtontech.animeseas"
	compileSdk =
		libs.versions.android.compileSdk
			.get()
			.toInt()

	sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
	sourceSets["main"].res.srcDirs("src/androidMain/res")
	sourceSets["main"].resources.srcDirs("src/commonMain/resources")

	defaultConfig {
		applicationId = "org.ailtontech.animeseas"
		minSdk =
			libs.versions.android.minSdk
				.get()
				.toInt()
		targetSdk =
			libs.versions.android.targetSdk
				.get()
				.toInt()
		versionCode = 1
		versionName = "1.0"
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
	buildTypes {
		getByName("release") {
			isMinifyEnabled = false
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	buildFeatures {
		compose = true
	}
	dependencies {
		debugImplementation(compose.uiTooling)
	}
}

compose.desktop {
	application {
		mainClass = "MainKt"

		nativeDistributions {
			targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
			packageName = "org.ailtontech.animeseas"
			packageVersion = "1.0.0"
		}
	}
}

ktlint {
	version.set(libs.versions.ktlin.version.get())
	verbose.set(true)
	ignoreFailures.set(false)
	android.set(false)
	enableExperimentalRules.set(true)
	outputToConsole.set(true)
	outputColorName.set("RED")

	reporters {
		reporter(ReporterType.HTML)
		reporter(ReporterType.CHECKSTYLE)
		reporter(ReporterType.JSON)
	}

	filter {
		exclude("**/generated/**")
		include("**/kotlin/**")
	}
}

tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask> {
	reportsOutputDirectory.set(
		project.layout.buildDirectory.dir("reports/ktlint"),
	)
}

detekt {
	buildUponDefaultConfig = true
	allRules = false
	config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
	reports {
		html.required.set(true)
		xml.required.set(true)
		txt.required.set(true)
		sarif.required.set(true)
	}
}

tasks.register<Copy>("copyPreCommitHook") {
	description = "Copy pre-commit git hook from the scripts to the .git/hooks folder."
	group = "git hooks"
	outputs.upToDateWhen { false }
	from("$rootDir/pre-commit")
	into("$rootDir/.git/hooks/")
}

tasks.register<Copy>("copyPrepareCommitHook") {
	description = "Copy prepare-commit git hook from the scripts to the .git/hooks folder."
	group = "git hooks"
	outputs.upToDateWhen { false }
	from("$rootDir/prepare-commit-msg")
	into("$rootDir/.git/hooks/")
}

tasks.build {
	dependsOn("copyPreCommitHook")
	dependsOn("copyPrepareCommitHook")
}