package basicBackup

import kotlinx.serialization.Serializable

@Serializable
data class Configs(val configs: List<Config>)

@Serializable
data class Config(
    val source: String,
    val destination: String,
    val exclusions: Set<String>
)