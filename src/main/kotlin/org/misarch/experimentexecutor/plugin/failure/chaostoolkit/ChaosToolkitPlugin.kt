package org.misarch.experimentexecutor.plugin.failure.chaostoolkit

import org.misarch.experimentexecutor.executor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import java.io.File

class ChaosToolkitPlugin: FailurePluginInterface {
    override suspend fun executeFailure(failure: Failure): Boolean {
        return runCatching {
            val currentDir = File(".").absolutePath

            val process = ProcessBuilder("bash", "-c",
                "source ~/.venvs/chaostk/bin/activate && PYTHONPATH=\"$currentDir\" chaos run test.yaml")
                .directory(File("."))
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            val exitCode = process.waitFor()
            exitCode == 0
        }.getOrElse { e ->
            e.printStackTrace()
            false
        }
    }
}