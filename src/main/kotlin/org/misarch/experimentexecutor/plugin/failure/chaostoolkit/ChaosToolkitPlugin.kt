package org.misarch.experimentexecutor.plugin.failure.chaostoolkit

import org.misarch.experimentexecutor.executor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class ChaosToolkitPlugin : FailurePluginInterface {
    private var containerId: String? = null

    override suspend fun executeFailure(failure: Failure, testUUID: UUID): Boolean {
        return runCatching {
            val filePath = failure.chaosToolkit!!.pathUri
            // TODO remove the container after the experiment
            val process = ProcessBuilder(
                "bash", "-c",
                "docker run -d -e TEST_UUID=$testUUID -v $filePath:/app/experiment.yaml -v /var/run/docker.sock:/var/run/docker.sock custom-chaostoolkit"
            ).redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                containerId = reader.readLine()?.trim()
            }
            containerId != null
        }.getOrElse {
            false
        }
    }

    override suspend fun startExperiment(): Boolean {
        return true
    }
}