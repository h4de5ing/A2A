// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
}
gradle.projectsEvaluated {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-Xbootclasspath/p:CClient\\libs\\framework.jar")
    }
}