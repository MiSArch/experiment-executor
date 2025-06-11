package org.misarch.experimentexecutor.controller.experiment.model

data class EncodedFileDTO(
    val fileName: String,
    val encodedWorkFileContent: String,
    val encodedUserStepsFileContent: String,
)
