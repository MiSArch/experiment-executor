import com.fasterxml.jackson.annotation.JsonProperty

data class MiSArchFailureConfig(
    @JsonProperty("failures") val failures: List<MisArchFailure>,
    @JsonProperty("pause") val pause: Long,
)

data class MisArchFailure(
    @JsonProperty("name") val name: String,
    @JsonProperty("pubSubDeterioration") val pubSubDeterioration: PubSubDeterioration?,
    @JsonProperty("serviceInvocationDeterioration") val serviceInvocationDeterioration: List<ServiceInvocationDeterioration>?,
    @JsonProperty("artificialMemoryUsage") val artificialMemoryUsage: Long?,
    @JsonProperty("artificialCPUUsage") val artificialCPUUsage: List<ArtificialCPUUsage>?,
)

data class PubSubDeterioration(
    @JsonProperty("delay") val delay: Int,
    @JsonProperty("delayProbability") val delayProbability: Double,
    @JsonProperty("errorProbability") val errorProbability: Double,
)

data class ServiceInvocationDeterioration(
    @JsonProperty("path") val path: String,
    @JsonProperty("delay") val delay: Int,
    @JsonProperty("delayProbability") val delayProbability: Double,
    @JsonProperty("errorProbability") val errorProbability: Double,
    @JsonProperty("errorCode") val errorCode: Int,
)

data class ArtificialCPUUsage(
    @JsonProperty("usageDuration") val usageDuration: Int,
    @JsonProperty("pauseDuration") val pauseDuration: Int,
)
