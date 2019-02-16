// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  val kotlinVersion by extra("1.3.21")

  repositories {
    google()
    jcenter()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:3.3.0")
    classpath(kotlin("gradle-plugin", kotlinVersion))
  }
}

allprojects {
  repositories {
    google()
    jcenter()
  }
}

tasks.register("clean", Delete::class) {
  delete(rootProject.buildDir)
}

