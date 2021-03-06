package com.hoc.comicapp.plugin

import appConfig
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import versions

private inline val Project.libraryExtension get() = extensions.getByType<LibraryExtension>()
private inline val Project.appExtension get() = extensions.getByType<AppExtension>()

open class ComicAppExtension {
  var viewBinding: Boolean = false
  var parcelize: Boolean = false
}

class ComicAppPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      plugins.all {
        when (this) {
          is JavaPlugin, is JavaLibraryPlugin -> {
            project.convention.getPlugin<JavaPluginConvention>().run {
              targetCompatibility = VERSION_11
              sourceCompatibility = VERSION_11
            }
          }
          is LibraryPlugin -> {
            plugins.apply("kotlin-android")
            configAndroidLibrary()
          }
          is AppPlugin -> {
            plugins.apply("kotlin-android")
            configAndroidApplication()
          }
          is KotlinBasePluginWrapper -> configKotlinOptions()
        }
      }
    }

    val comicAppExtension = project.extensions.create("comicApp", ComicAppExtension::class)

    project.afterEvaluate {
      project.plugins.all {
        when (this) {
          is LibraryPlugin -> {
            @Suppress("UnstableApiUsage")
            libraryExtension.buildFeatures {
              viewBinding = comicAppExtension.viewBinding
              dataBinding = false
            }
            enableParcelize(comicAppExtension.parcelize)
          }
          is AppPlugin -> {
            appExtension.buildFeatures.run {
              @Suppress("UnstableApiUsage")
              viewBinding = comicAppExtension.viewBinding
            }
            enableParcelize(comicAppExtension.parcelize)
          }
        }
      }
    }
  }
}

private fun Project.enableParcelize(enabled: Boolean) {
  if (enabled) {
    plugins.apply("kotlin-parcelize")
  }
}

private fun Project.configKotlinOptions() {
  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = VERSION_11.toString()
      freeCompilerArgs = freeCompilerArgs + listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xinline-classes")
    }
  }
}

private fun Project.configAndroidLibrary() = libraryExtension.run {
  compileSdk = versions.sdk.compile
  buildToolsVersion = versions.sdk.buildTools

  defaultConfig {
    minSdk = versions.sdk.min
    targetSdk = versions.sdk.target

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = VERSION_11
    targetCompatibility = VERSION_11
  }
}

private fun Project.configAndroidApplication() = appExtension.run {
  buildToolsVersion(versions.sdk.buildTools)
  compileSdkVersion(versions.sdk.compile)

  defaultConfig {
    applicationId = appConfig.applicationId

    minSdk = versions.sdk.min
    targetSdk = versions.sdk.target

    versionCode = appConfig.versionCode
    versionName = appConfig.versionName

    resourceConfigurations.addAll(appConfig.supportedLocales)

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = VERSION_11
    targetCompatibility = VERSION_11
  }
}
