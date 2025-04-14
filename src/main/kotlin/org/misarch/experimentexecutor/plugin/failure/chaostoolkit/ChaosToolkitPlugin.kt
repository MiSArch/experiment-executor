package org.misarch.experimentexecutor.plugin.failure.chaostoolkit

import org.misarch.experimentexecutor.executor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface

class ChaosToolkitPlugin : FailurePluginInterface {
    override suspend fun executeFailure(failure: Failure): Boolean {
        return runCatching {
            val filePath = failure.chaosToolkit!!.pathUri
            val process = ProcessBuilder(
                "bash", "-c",
                "docker run --rm -v $filePath:/app/experiment.yaml -v /var/run/docker.sock:/var/run/docker.sock custom-chaostoolkit"
            ).redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            process.waitFor()
            true
        }.getOrElse { e ->
            e.printStackTrace()
            false
        }
    }
}