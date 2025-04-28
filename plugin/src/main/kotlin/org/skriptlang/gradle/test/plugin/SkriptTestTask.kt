package org.skriptlang.gradle.test.plugin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

abstract class SkriptTestTask : DefaultTask() {

    init {
        description = "Run Skript tests"
        group = LifecycleBasePlugin.VERIFICATION_GROUP
    }

    @get:InputDirectory
    @get:Option(option = "testScriptDirectory", description = "The directory containing the test scripts to run")
    abstract val testScriptDirectory: DirectoryProperty

    @get:InputDirectory
    @get:Option(option = "extraPluginsDirectory", description = "The directory of extra plugins to put on the test server")
    abstract val extraPluginsDirectory: DirectoryProperty

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

    private fun runCommand(requiredExitValue: Int, workingDirectory: Path, vararg command: String) {
        runCommand(requiredExitValue, workingDirectory, false, *command)
    }

    private fun runCommand(requiredExitValue: Int, workingDirectory: Path, printOutput: Boolean, vararg command: String) {
        val processBuilder = ProcessBuilder(command.asList()).directory(workingDirectory.toFile())
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()

        if (printOutput) {
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                println("| $line")
            }
        }

        process.waitFor()
        if (process.exitValue() != requiredExitValue) {
            throw IllegalStateException("${command.joinToString(" ")} returned exit code ${process.exitValue()}")
        }
    }

    @TaskAction
    fun runTests() {
        val skriptRepoDir = Files.createTempDirectory("skript-test-skript-repo").toAbsolutePath()
        println("git init")
        runCommand(0, skriptRepoDir, "git", "init")
        println("git remote add origin " + skriptRepo.getOrElse("https://github.com/SkriptLang/Skript.git"))
        runCommand(0, skriptRepoDir, "git", "remote", "add", "origin", skriptRepo.getOrElse("https://github.com/SkriptLang/Skript.git"))
        println("git fetch --depth 1 origin " + skriptRepoRef.getOrElse("master"))
        runCommand(0, skriptRepoDir, "git", "fetch", "--depth", "1", "origin", skriptRepoRef.getOrElse("master"))
        println("git checkout FETCH_HEAD")
        runCommand(0, skriptRepoDir, "git", "checkout", "FETCH_HEAD")
        println("git submodule update --init --depth 1")
        runCommand(0, skriptRepoDir, "git", "submodule", "update", "--init", "--depth", "1")

        val vanillaTestDir = skriptRepoDir.resolve("src/test/skript/tests").toFile()

        // delete vanilla test scripts if not running them
        if (!runVanillaTests.getOrElse(true)) {
            println("Deleting vanilla test scripts")
            if (vanillaTestDir.exists()) {
                vanillaTestDir.deleteRecursively()
                vanillaTestDir.mkdir()
            }
        }

        // copy test scripts
        val testScriptDir = testScriptDirectory.get().asFile
        if (testScriptDir.exists()) {
            println("Copying test scripts from ${testScriptDir.absolutePath} to ${vanillaTestDir.absolutePath}")
            val customTests = File(vanillaTestDir, "custom")
            customTests.mkdir()
            testScriptDir.copyRecursively(customTests)
        }

        // copy extra plugins
        val extraPluginsDirectory = extraPluginsDirectory.getOrNull()
        if (extraPluginsDirectory != null) {
            println("Adding extra plugins to environments")
            val environmentsDir = skriptRepoDir.resolve("src/test/skript/environments")
            val mapper = ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
            Files.walk(environmentsDir)
                .filter { it.isRegularFile() && it.extension == "json" }
                .forEach { envPath ->
                    val environmentFile = envPath.toFile()
                    println("Processing environment file: ${environmentFile.absolutePath}")

                    try {
                        // Read the environment file
                        val environment = mapper.readTree(environmentFile) as ObjectNode

                        // Initialize resources array if it doesn't exist
                        if (!environment.has("resources")) {
                            environment.set<JsonNode>("resources", mapper.createArrayNode())
                        }

                        val resources = environment.get("resources") as ArrayNode

                        // Add each plugin as a resource
                        extraPluginsDirectory.asFile.listFiles()?.forEach { pluginPath ->
                            println("Adding plugin: ${pluginPath.name}")
                            val resource = mapper.createObjectNode()
                            resource.put("source", pluginPath.absolutePath)
                            resource.put("target", "plugins/${pluginPath.name}")
                            resources.add(resource)
                        }

                        // Write back the updated environment
                        mapper.writeValue(environmentFile, environment)
                    } catch (e: Exception) {
                        println("Error processing file ${environmentFile.absolutePath}")
                        e.printStackTrace()
                    }
                }
        }

        println("./gradlew.bat quickTest")
        try {
            runCommand(0, skriptRepoDir, true, "./gradlew.bat", "quickTest")
        } catch (_: IllegalStateException) {
            throw IllegalStateException("Tests failed")
        }
        println("complete")
    }

}
