package org.misarch.experimentconfignew.service.experiment

import org.misarch.experimentconfignew.executor.model.Failure
import org.springframework.stereotype.Service

@Service
class SetupExperimentFailureService {

    suspend fun setupExperimentFailure(failure: Failure) {
    }

    suspend fun resetExperimentFailure() {
    }
}