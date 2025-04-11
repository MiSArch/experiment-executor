package org.misarch.experimentexecutor.plugin.failure

import org.misarch.experimentexecutor.executor.model.Failure

interface FailurePluginInterface {

    suspend fun executeFailure(failure: Failure): Boolean

}