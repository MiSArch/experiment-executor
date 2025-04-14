package org.misarch.experimentexecutor.plugin.failure.chaostoolkit

import org.misarch.experimentexecutor.executor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import java.io.File

class ChaosToolkitPlugin: FailurePluginInterface {
    override suspend fun executeFailure(failure: Failure): Boolean {
        return runCatching {
            val currentDir = File(".").absolutePath
            val venv = "~/.venvs/chaostk/bin/activate"

            val process = ProcessBuilder("bash", "-c",
                "source $venv && PYTHONPATH=\"$currentDir\" chaos run ${failure.chaosToolkit!!.pathUri}")
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