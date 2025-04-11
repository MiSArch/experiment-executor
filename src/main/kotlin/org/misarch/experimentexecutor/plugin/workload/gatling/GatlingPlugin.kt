package org.misarch.experimentexecutor.plugin.workload.gatling

import org.misarch.experimentexecutor.executor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import java.io.File

class GatlingPlugin : WorkloadPluginInterface {
    override suspend fun executeWorkLoad(workLoad: WorkLoad): Boolean {
        return runCatching {
            val process = ProcessBuilder("./gradlew", "gatlingRun")
                .directory(File("/Users/p371728/master/thesis/misarch/gatling-test/untitled"))
                .redirectOutput(ProcessBuilder.Redirect.INHERIT) // Redirect output to console
                .redirectError(ProcessBuilder.Redirect.INHERIT) // Redirect error to console
                .start()
            val exitCode = process.waitFor()
            exitCode == 0
        }.getOrElse { e ->
            e.printStackTrace()
            false
        }
    }
}