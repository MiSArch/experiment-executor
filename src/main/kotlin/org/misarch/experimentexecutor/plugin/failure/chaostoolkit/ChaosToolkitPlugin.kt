package org.misarch.experimentexecutor.plugin.failure.chaostoolkit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.misarch.experimentexecutor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class ChaosToolkitPlugin : FailurePluginInterface {
    private var containerId: String? = null

    override suspend fun executeFailure(failure: Failure, testUUID: UUID): Boolean {
        withContext(Dispatchers.IO) {
            val filePath = failure.chaosToolkit!!.pathUri
            val process = ProcessBuilder(
                "bash", "-c",
                "docker run -d -e TEST_UUID=$testUUID -v $filePath:/app/experiment.yaml -v /var/run/docker.sock:/var/run/docker.sock custom-chaostoolkit"
            ).redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                containerId = reader.readLine()?.trim()
            }

            if (containerId != null) {
                ProcessBuilder("bash", "-c", "docker wait $containerId").start().waitFor()
                ProcessBuilder("bash", "-c", "docker rm $containerId").start().waitFor()
            }
        }
        return containerId != null
    }

    override suspend fun startExperiment(): Boolean {
        return true
    }
}