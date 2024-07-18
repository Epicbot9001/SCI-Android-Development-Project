// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.3.1" apply false
//    id("androidx.navigation:navigation-safe-args-gradle-plugin") version "2.2.2" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

buildscript {
    val kotlin_version = "1.8.22"
    extra["kotlin_version"] = kotlin_version
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}
