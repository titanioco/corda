package net.corda.plugins

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtraPropertiesExtension

fun Project.task(name: String): Task = tasks.single { it.name == name }
fun Project.configuration(name: String): Configuration = configurations.single { it.name == name }
fun<T : Any> Project.ext(name: String): T = (configuration("ext") as ExtraPropertiesExtension).get(name) as T

class Utils {
    companion object {
        @JvmStatic
        fun createCompileConfiguration(name: String, project: Project) {
            if(!project.configurations.any { it.name == name }) {
                val configuration = project.configurations.create(name)
                configuration.isTransitive = false
                project.configurations.single { it.name == "compile" }.extendsFrom(configuration)
            }
        }
    }

}