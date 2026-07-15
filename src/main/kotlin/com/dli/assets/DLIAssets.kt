package com.dli.assets

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.Executors

class DLIAssets : JavaPlugin() {

    companion object {
        @JvmStatic
        var instance: DLIAssets? = null
            private set

        val namespace = "dliassets"
    }

    lateinit var configManager: ConfigManager
    lateinit var assetRegistry: AssetRegistry
    lateinit var customItemManager: CustomItemManager
    lateinit var resourcePackManager: ResourcePackManager
    
    private val miniMessage by lazy { MiniMessage.miniMessage() }
    private val executor = Executors.newSingleThreadExecutor()

    override fun onEnable() {
        instance = this
        logger.info("DLIAssets loading...")

        saveDefaultConfig()
        saveResource("items.yml", false)
        saveResource("blocks.yml", false)
        saveResource("mobs.yml", false)
        saveResource("pack.yml", false)
        saveResource("model_engine.yml", false)
        saveResource("recipes.yml", false)
        saveResource("guis.yml", false)
        saveResource("messages.yml", false)

        configManager = ConfigManager(this)
        assetRegistry = AssetRegistry(this)
        customItemManager = CustomItemManager(this)
        resourcePackManager = ResourcePackManager(this)

        assetRegistry.loadAll()

        Bukkit.pluginManager.registerEvents(customItemManager, this)
        
        val command = DLIAssetsCommand(this)
        getCommand("dliassets")?.setExecutor(command)
        getCommand("dliassets")?.setTabCompleter(command)
        getCommand("dligive")?.setExecutor(command)
        getCommand("dligive")?.setTabCompleter(command)

        generateResourcePackAsync()

        logger.info("DLIAssets enabled! (v${pluginMeta.version})")
    }

    override fun onDisable() {
        logger.info("DLIAssets disabling...")
        resourcePackManager.cleanup()
        executor.shutdown()
        instance = null
    }

    fun miniMessage(): MiniMessage = miniMessage

    private fun generateResourcePackAsync() {
        executor.submit {
            try {
                resourcePackManager.generatePack()
                logger.info("Resource pack generated.")
            } catch (e: Exception) {
                logger.severe("Resource pack error: ${e.message}")
            }
        }
    }

    fun reloadPlugin() {
        logger.info("Reloading DLIAssets...")
        reloadConfig()
        configManager.reload()
        assetRegistry.loadAll()
        customItemManager.reload()
        generateResourcePackAsync()
        logger.info("Reload complete.")
    }
}