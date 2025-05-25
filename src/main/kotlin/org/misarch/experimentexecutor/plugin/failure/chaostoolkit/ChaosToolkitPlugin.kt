package org.misarch.experimentexecutor.plugin.failure.chaostoolkit

import kotlinx.coroutines.runBlocking
import org.misarch.experimentexecutor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import org.misarch.experimentexecutor.plugin.util.DockerExecutor
import java.util.UUID

class ChaosToolkitPlugin : FailurePluginInterface {
    override suspend fun initalizeFailure(failure: Failure, testUUID: UUID) {
        val filePath = failure.chaosToolkit.pathUri
        runBlocking {
            DockerExecutor().executeDocker(
                "docker run -d " +
                        "-e TEST_UUID=$testUUID " +
                        "-v $filePath:/app/experiment.yaml " +
                        "-v /var/run/docker.sock:/var/run/docker.sock " +
                        "custom-chaostoolkit"
            )
        }
    }

    override suspend fun startTimedExperiment() {}
}