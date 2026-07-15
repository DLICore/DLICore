package com.dli.assets.config

data class PackConfig(
    val namespace: String,
    val packFormat: Int,
    val description: String,
    val generation: GenerationConfig,
    val distribution: DistributionConfig,
    val language: LanguageConfig
)

data class GenerationConfig(
    val enabled: Boolean,
    val outputDir: String,
    val cleanBefore: Boolean,
    val autoModels: Boolean,
    val autoBlockstates: Boolean,
    val autoEntityModels: Boolean,
    val compressionLevel: Int,
    val textures: TextureConfig
)

data class TextureConfig(
    val sourceDir: String,
    val generatePlaceholders: Boolean,
    val optimize: Boolean
)

data class DistributionConfig(
    val builtinServer: BuiltinServerConfig,
    val external: ExternalConfig
)

data class BuiltinServerConfig(
    val enabled: Boolean,
    val path: String,
    val maxSizeMb: Int
)

data class ExternalConfig(
    val enabled: Boolean,
    val url: String,
    val sha256: String,
    val force: Boolean
)

data class LanguageConfig(
    val default: String,
    val supported: List<String>
)