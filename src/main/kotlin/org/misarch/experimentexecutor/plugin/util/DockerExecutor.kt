package org.misarch.experimentexecutor.plugin.util

import kotlinx.coroutines.coroutineScope
import java.io.BufferedReader
import java.io.InputStreamReader

class DockerExecutor {
    suspend fun executeDocker(dockerRunCommand: String) {
        coroutineScope {
            val process = ProcessBuilder(
                "bash", "-c",
                dockerRunCommand
            ).redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            var containerId: String?
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                containerId = reader.readLine()?.trim()
            }

            if (containerId != null) {
                ProcessBuilder("bash", "-c", "docker wait $containerId").start().waitFor()
                ProcessBuilder("bash", "-c", "docker rm $containerId").start().waitFor()
            }
        }
    }
}