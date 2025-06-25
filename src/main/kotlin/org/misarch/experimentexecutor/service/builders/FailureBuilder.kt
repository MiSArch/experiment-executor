package org.misarch.experimentexecutor.service.builders

import MiSArchFailureConfig
import MisArchFailure
import Pauses
import ServiceInvocationDeterioration
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.model.Action
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.model.ChaosToolkitConfig
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.model.Pause
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.model.Probe
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.model.PythonProvider
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.model.SteadyStateHypothesis
import java.util.UUID

fun buildChaosToolkitConfig(
    testUUID: UUID,
    testVersion: String,
    testDuration: Int,
) = ChaosToolkitConfig(
    title = "$testUUID:$testVersion",
    description = "$testUUID:$testVersion",
    steadyStateHypothesis =
        SteadyStateHypothesis(
            title = "Container is running",
            probes =
                listOf(
                    Probe(
                        type = "probe",
                        name = "Container is running",
                        tolerance = true,
                        provider =
                            PythonProvider(
                                module = "chaostoolkit_docker",
                                func = "are_containers_running",
                                arguments =
                                    mapOf(
                                        "names" to
                                            listOf(
                                                "infrastructure-docker-gateway-1",
                                                "infrastructure-docker-gateway-dapr-1",
                                                "infrastructure-docker-gateway-ecs-1",
                                            ),
                                    ),
                            ),
                    ),
                ),
        ),
    method =
        listOf(
            Action(
                type = "action",
                name = "kill-container",
                provider =
                    PythonProvider(
                        type = "python",
                        module = "chaostoolkit_docker",
                        func = "kill_containers",
                        arguments =
                            mapOf(
                                "names" to
                                    listOf(
                                        "infrastructure-docker-gateway-1",
                                        "infrastructure-docker-gateway-dapr-1",
                                        "infrastructure-docker-gateway-ecs-1",
                                    ),
                            ),
                    ),
                pauses =
                    Pause(
                        before = testDuration / 5,
                        after = testDuration / 8,
                    ),
            ),
            Action(
                type = "action",
                name = "restart-container",
                provider =
                    PythonProvider(
                        type = "python",
                        module = "chaostoolkit_docker",
                        func = "start_containers",
                        arguments =
                            mapOf(
                                "names" to
                                    listOf(
                                        "infrastructure-docker-gateway-1",
                                        "infrastructure-docker-gateway-dapr-1",
                                        "infrastructure-docker-gateway-ecs-1",
                                    ),
                            ),
                    ),
            ),
        ),
)

fun buildMisarchExperimentConfig(testDuration: Int): List<MiSArchFailureConfig> =
    listOf(
        MiSArchFailureConfig(
            failures =
                listOf(
                    MisArchFailure(
                        name = "catalog",
                        pubSubDeterioration = null,
                        serviceInvocationDeterioration =
                            listOf(
                                ServiceInvocationDeterioration(
                                    path = "/",
                                    delay = 1000,
                                    delayProbability = 1.0,
                                    errorProbability = 1.0,
                                    errorCode = 404,
                                ),
                            ),
                        artificialMemoryUsage = null,
                        artificialCPUUsage = null,
                    ),
                ),
            pauses =
                Pauses(
                    before = testDuration / 3,
                    after = testDuration / 6,
                ),
        ),
        MiSArchFailureConfig(
            failures =
                listOf(
                    MisArchFailure(
                        name = "catalog",
                        pubSubDeterioration = null,
                        serviceInvocationDeterioration = null,
                        artificialMemoryUsage = null,
                        artificialCPUUsage = null,
                    ),
                ),
            pauses =
                Pauses(
                    before = 0,
                    after = 0,
                ),
        ),
    )
