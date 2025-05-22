package org.misarch.experimentexecutor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExperimentExecutorApplication

fun main(args: Array<String>) {
    runApplication<ExperimentExecutorApplication>(*args)
}
