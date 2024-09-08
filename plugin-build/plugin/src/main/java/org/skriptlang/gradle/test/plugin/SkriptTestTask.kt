package org.skriptlang.gradle.test.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.nio.file.Files
import java.nio.file.Path

abstract class SkriptTestTask : DefaultTask() {
    init {
        description = "Run Skript tests"
        group = BasePlugin.BUILD_GROUP
    }

    @get:InputFile
    @get:Option(option = "testScriptDirectory", description = "The directory containing the test scripts to run")
    abstract val testScriptDirectory: RegularFileProperty

    @get:InputFile
    @get:Option(option = "extraPluginsDirectory", description = "The directory of extra plugins to put on the test server")
    abstract val extraPluginsDirectory: RegularFileProperty

    @get:Input
    @get:Option(option = "skriptRepoRef", description = "The Git ref to check out the Skript repo at")
    @get:Optional
    abstract val skriptRepoRef: Property<String>

    @get:Input
    @get:Option(option = "skriptRepo", description = "The Git URL to the Skript repo")
    @get:Optional
    abstract val skriptRepo: Property<String>

    @get:Input
    @get:Option(option = "runVanillaTests", description = "Controls whether the vanilla Skript tests are run")
    @get:Optional
    abstract val runVanillaTests: Property<Boolean>

    fun runCommand(requiredExitValue: Int, workingDirectory: Path, vararg command: String) {
        val processBuilder = ProcessBuilder(command.asList()).directory(workingDirectory.toFile())
        val process = processBuilder.start()
        process.waitFor()
        if (process.exitValue() != requiredExitValue) {
            throw IllegalStateException("${command.joinToString(" ")} returned exit code ${process.exitValue()}")
        }
    }

    @TaskAction
    fun runTests() {
        val skriptRepoDir = Files.createTempDirectory("skript-test-skript-repo").toAbsolutePath()
        runCommand(0, skriptRepoDir, "git", "init")
        runCommand(0, skriptRepoDir, "git", "remote", "add", "origin", skriptRepo.getOrElse("https://github.com/SkriptLang/Skript.git"))
        runCommand(0, skriptRepoDir, "git", "fetch", "--depth", "1", "origin", skriptRepoRef.getOrElse("master"))
        runCommand(0, skriptRepoDir, "git", "checkout", "FETCH_HEAD")
        runCommand(0, skriptRepoDir, "git", "submodule", "update", "--init", "--depth", "1")
        try {
            runCommand(0, skriptRepoDir, "./gradlew.bat", "quick")
        } catch (exception: IllegalStateException) {
            throw IllegalStateException("Tests failed")
        }
    }

}
