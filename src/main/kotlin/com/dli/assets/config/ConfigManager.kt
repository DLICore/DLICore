package com.dli.assets.config

import com.dli.assets.DLIAssets
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.lang.reflect.Type

class ConfigManager(private val plugin: DLIAssets) {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Material::class.java, MaterialAdapter())
        .create()

    var mainConfig: YamlConfiguration = YamlConfiguration()
    var itemsConfig: YamlConfiguration = YamlConfiguration()
    var blocksConfig: YamlConfiguration = YamlConfiguration()
    var mobsConfig: YamlConfiguration = YamlConfiguration()
    var packConfig: YamlConfiguration = YamlConfiguration()
    var guisConfig: YamlConfiguration = YamlConfiguration()
    var recipesConfig: YamlConfiguration = YamlConfiguration()
    var messagesConfig: YamlConfiguration = YamlConfiguration()
    var modelEngineConfig: YamlConfiguration = YamlConfiguration()

    fun loadAll() {
        plugin.logger.info("Loading configs...")
        mainConfig = loadYaml("config.yml")
        itemsConfig = loadYaml("items.yml")
        blocksConfig = loadYaml("blocks.yml")
        mobsConfig = loadYaml("mobs.yml")
        packConfig = loadYaml("pack.yml")
        guisConfig = loadYaml("guis.yml")
        recipesConfig = loadYaml("recipes.yml")
        messagesConfig = loadYaml("messages.yml")
        modelEngineConfig = loadYaml("model_engine.yml")
    }

    fun reload() {
        plugin.reloadConfig()
        mainConfig = plugin.config
        itemsConfig = loadYaml("items.yml")
        blocksConfig = loadYaml("blocks.yml")
        mobsConfig = loadYaml("mobs.yml")
        packConfig = loadYaml("pack.yml")
        guisConfig = loadYaml("guis.yml")
        recipesConfig = loadYaml("recipes.yml")
        messagesConfig = loadYaml("messages.yml")
        modelEngineConfig = loadYaml("model_engine.yml")
    }

    private fun loadYaml(fileName: String): YamlConfiguration {
        val file = File(plugin.dataFolder, fileName)
        if (!file.exists()) plugin.saveResource(fileName, false)
        return YamlConfiguration.loadConfiguration(file)
    }

    fun getMainConfig(): YamlConfiguration = mainConfig
    fun getItemsConfig(): YamlConfiguration = itemsConfig
    fun getBlocksConfig(): YamlConfiguration = blocksConfig
    fun getMobsConfig(): YamlConfiguration = mobsConfig
    fun getPackConfigYaml(): YamlConfiguration = packConfig
    fun getGuisConfig(): YamlConfiguration = guisConfig
    fun getRecipesConfig(): YamlConfiguration = recipesConfig
    fun getMessagesConfig(): YamlConfiguration = messagesConfig
    fun getModelEngineConfig(): YamlConfiguration = modelEngineConfig

    class MaterialAdapter : com.google.gson.JsonSerializer<Material>, com.google.gson.JsonDeserializer<Material> {
        override fun serialize(src: Material, typeOfSrc: Type, context: com.google.gson.JsonSerializationContext): com.google.gson.JsonElement {
            return com.google.gson.JsonPrimitive(src.name())
        }
        override fun deserialize(json: com.google.gson.JsonElement, typeOfT: Type, context: com.google.gson.JsonDeserializationContext): Material {
            return try { Material.valueOf(json.asString.toUpperCase()) } catch (e: IllegalArgumentException) { Material.AIR }
        }
    }
}