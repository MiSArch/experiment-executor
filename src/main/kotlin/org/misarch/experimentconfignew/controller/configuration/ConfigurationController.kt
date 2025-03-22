package org.misarch.experimentconfignew.controller.configuration

import org.misarch.experimentconfignew.controller.model.dto.ServiceConfiguration
import org.misarch.experimentconfignew.service.configuration.ConfigurationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/configuration")
class ConfigurationController(
    private val configurationService: ConfigurationService
) {
    @GetMapping
    suspend fun getAllServices(): List<ServiceConfiguration> {
        return configurationService.findAllServices()
    }

    @GetMapping("/names")
    suspend fun getAllServiceNames(): List<String> {
        return configurationService.findAllServiceNames()
    }

    // TODO finish the other endpoints

}