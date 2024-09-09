package org.skriptlang.gradle.test.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class SkriptTestPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register("skriptTest", SkriptTestTask::class.java)
    }

}
