package org.misarch.experimentconfignew.service.configuration

import org.misarch.experimentconfignew.controller.model.dto.ServiceConfiguration
import org.springframework.stereotype.Service

@Service
class ConfigurationService {

    suspend fun findAllServices(): List<ServiceConfiguration> {
        // TODO implement
        return emptyList()
    }

    suspend fun findAllServiceNames(): List<String> {
        return findAllServices().map { it.name }
    }
}