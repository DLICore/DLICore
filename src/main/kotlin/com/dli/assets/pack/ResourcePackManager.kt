package com.dli.assets.pack

import com.dli.assets.DLIAssets
import com.dli.assets.config.ConfigManager
import com.dli.assets.registry.AssetRegistry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResourcePackManager(
    private val plugin: DLIAssets,
    private val configManager: ConfigManager,
    private val assetRegistry: AssetRegistry
) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val packConfig: ConfigManager.PackConfig = configManager.getPackConfig()
    private val genConfig: ConfigManager.GenerationConfig = packConfig.generation
    
    private var generatedPackFile: File? = null
    private var packSha256: String = ""

    fun generatePack(): File {
        plugin.logger.info("Starting resource pack generation...")
        val startTime = System.currentTimeMillis()
        
        try {
            val packDir = preparePackDirectory()
            generatePackMeta(packDir)
            generateModels(packDir)
            copyTextures(packDir)
            generateLanguageFiles(packDir)
            
            val zipFile = createZip(packDir)
            generatedPackFile = zipFile
            packSha256 = calculateSha256(zipFile)
            
            plugin.logger.info("Resource pack generated in ${System.currentTimeMillis() - startTime}ms: ${zipFile.name} (${formatSize(zipFile.length())})")
            
            if (packConfig.distribution.builtinServer.enabled) {
                registerBuiltinServer(zipFile)
            }
            
            return zipFile
        } catch (e: Exception) {
            plugin.logger.severe("Failed to generate resource pack: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Pack generation failed", e)
        }
    }

    fun getPackFile(): File? = generatedPackFile
    fun getPackSha256(): String = packSha256
    fun getPackUrl(): String? {
        if (packConfig.distribution.builtinServer.enabled && generatedPackFile != null) {
            return "/${packConfig.distribution.builtinServer.path}/pack.zip"
        }
        return packConfig.distribution.external.url.takeIf { it.isNotBlank() }
    }

    fun cleanup() {
        generatedPackFile?.delete()
        generatedPackFile = null
        packSha256 = ""
    }

    private fun preparePackDirectory(): File {
        val dataFolder = plugin.dataFolder.toPath()
        val packDir = dataFolder.resolve(genConfig.outputDir).toFile()
        
        if (genConfig.cleanBefore && packDir.exists()) {
            deleteRecursive(packDir)
        }
        packDir.mkdirs()
        
        packDir.resolve("assets/minecraft/models/item").toFile().mkdirs()
        packDir.resolve("assets/minecraft/models/block").toFile().mkdirs()
        packDir.resolve("assets/minecraft/blockstates").toFile().mkdirs()
        packDir.resolve("assets/${packConfig.namespace}/models/item").toFile().mkdirs()
        packDir.resolve("assets/${packConfig.namespace}/models/block").toFile().mkdirs()
        packDir.resolve("assets/${packConfig.namespace}/blockstates").toFile().mkdirs()
        packDir.resolve("assets/${packConfig.namespace}/textures/item").toFile().mkdirs()
        packDir.resolve("assets/${packConfig.namespace}/textures/block").toFile().mkdirs()
        packDir.resolve("assets/${packConfig.namespace}/lang").toFile().mkdirs()
        
        return packDir
    }

    private fun generatePackMeta(packDir: File) {
        val packMeta = mapOf(
            "pack" to mapOf(
                "pack_format" to packConfig.packFormat,
                "description" to packConfig.description
            )
        )
        
        FileWriter(packDir.resolve("pack.mcmeta").toFile()).use { writer ->
            gson.toJson(packMeta, writer)
        }
        
        val iconSrc = plugin.dataFolder.toPath().resolve("pack.png")
        if (Files.exists(iconSrc)) {
            Files.copy(iconSrc, packDir.toPath().resolve("pack.png"))
        }
    }

    private fun generateModels(packDir: File) {
        plugin.logger.fine("Generating item/block models...")
        
        assetRegistry.getAllItems().forEach { regItem ->
            generateItemModel(packDir, regItem)
        }
        
        if (genConfig.autoBlockstates) {
            assetRegistry.getAllBlocks().forEach { regBlock ->
                generateBlockModel(packDir, regBlock)
                generateBlockstate(packDir, regBlock)
            }
        }
    }

    private fun generateItemModel(packDir: File, item: AssetRegistry.RegisteredItem) {
        val entry = item.entry
        
        val parent = if (entry.entryData.containsKey("model")) {
            val modelSection = entry.entryData.getConfigurationSection("model")
            modelSection?.getString("parent") ?: "item/generated"
        } else "item/generated"
        
        val textures = mutableMapOf<String, String>()
        val modelSection = entry.entryData.getConfigurationSection("model")
        val textureList = modelSection?.getStringList("textures") ?: emptyList()
        
        textureList.forEachIndexed { index, tex ->
            val key = when (index) {
                0 -> "layer0"
                1 -> "layer1"
                2 -> "layer2"
                3 -> "layer3"
                else -> "layer$index"
            }
            val cleanTex = tex.removeSuffix(".png")
            textures[key] = "${packConfig.namespace}:item/$cleanTex"
        }
        
        val modelJson = mutableMapOf<String, Any>(
            "parent" to parent,
            "textures" to textures
        )
        
        val modelFile = File(packDir, "assets/${packConfig.namespace}/models/item/${entry.id}.json")
        modelFile.parentFile?.mkdirs()
        FileWriter(modelFile).use { writer -> gson.toJson(modelJson, writer) }
    }

    private fun generateBlockModel(packDir: File, block: AssetRegistry.RegisteredBlock) {
        val entry = block.entry
        
        val modelJson = mutableMapOf<String, Any>(
            "parent" to "block/cube_all",
            "textures" to mapOf("all" to "${packConfig.namespace}:block/${entry.id}")
        )
        
        val modelFile = File(packDir, "assets/${packConfig.namespace}/models/block/${entry.id}.json")
        modelFile.parentFile?.mkdirs()
        FileWriter(modelFile).use { writer -> gson.toJson(modelJson, writer) }
    }

    private fun generateBlockstate(packDir: File, block: AssetRegistry.RegisteredBlock) {
        val entry = block.entry
        
        val stateJson = mapOf(
            "variants" to mapOf(
                "" to mapOf("model" to "${packConfig.namespace}:block/${entry.id}")
            )
        )
        
        val stateFile = File(packDir, "assets/${packConfig.namespace}/blockstates/${entry.id}.json")
        stateFile.parentFile?.mkdirs()
        FileWriter(stateFile).use { writer -> gson.toJson(stateJson, writer) }
    }

    private fun copyTextures(packDir: File) {
        val sourceDir = plugin.dataFolder.toPath().resolve(genConfig.textures.sourceDir)
        if (!Files.exists(sourceDir)) {
            plugin.logger.warning("Texture source directory not found: $sourceDir")
            return
        }
        
        val targetItemDir = packDir.toPath().resolve("assets/${packConfig.namespace}/textures/item")
        val targetBlockDir = packDir.toPath().resolve("assets/${packConfig.namespace}/textures/block")
        val targetEntityDir = packDir.toPath().resolve("assets/${packConfig.namespace}/textures/entity")
        
        Files.walk(sourceDir).forEach { path ->
            if (Files.isRegularFile(path) && path.toString().endsWith(".png")) {
                val relPath = sourceDir.relativize(path)
                val relStr = relPath.toString()
                
                val targetDir = when {
                    relStr.startsWith("item/") || relStr.startsWith("items/") -> targetItemDir
                    relStr.startsWith("block/") || relStr.startsWith("blocks/") -> targetBlockDir
                    relStr.startsWith("entity/") -> targetEntityDir
                    else -> targetItemDir
                }
                
                val targetFile = targetDir.resolve(relPath.subpath(1, relPath.nameCount))
                targetFile.parent?.toFile()?.mkdirs()
                
                try {
                    Files.copy(path, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to copy texture $path: ${e.message}")
                }
            }
        }
    }

    private fun generateLanguageFiles(packDir: File) {
        val langDir = packDir.resolve("assets/${packConfig.namespace}/lang").toFile()
        langDir.mkdirs()
        
        packConfig.language.supported.forEach { lang ->
            val langFile = File(langDir, "$lang.json")
            val translations = mutableMapOf<String, String>()
            
            assetRegistry.getAllItems().forEach { item ->
                translations["item.${packConfig.namespace}.${item.entry.id}"] = stripMiniMessage(item.entry.displayName)
            }
            assetRegistry.getAllBlocks().forEach { block ->
                translations["block.${packConfig.namespace}.${block.entry.id}"] = stripMiniMessage(block.entry.displayName)
            }
            assetRegistry.getAllMobs().forEach { mob ->
                translations["entity.${packConfig.namespace}.${mob.entry.id}"] = stripMiniMessage(mob.entry.displayName)
            }
            
            FileWriter(langFile).use { writer -> gson.toJson(translations, writer) }
        }
    }

    private fun stripMiniMessage(input: String): String {
        return input.replace("<[^>]+>".toRegex(), "")
    }

    private fun createZip(sourceDir: File): File {
        val zipFile = File(plugin.dataFolder, "dliassets_pack_${System.currentTimeMillis()}.zip")
        val out = ZipOutputStream(FileOutputStream(zipFile))
        out.level = genConfig.compressionLevel
        
        try {
            Files.walk(sourceDir.toPath())
                .filter { Files.isRegularFile(it) }
                .forEach { path ->
                    val relativePath = sourceDir.toPath().relativize(path).toString().replace('\\', '/')
                    out.putNextEntry(ZipEntry(relativePath))
                    Files.copy(path, out)
                    out.closeEntry()
                }
        } finally {
            out.close()
        }
        
        return zipFile
    }

    private fun registerBuiltinServer(zipFile: File) {
        try {
            val server = Bukkit.getServer()
            val method = server.javaClass.getMethod("addBuiltinResourcePack", String::class.java, File::class.java, String::class.java)
            method.invoke(server, packConfig.distribution.builtinServer.path, zipFile, packSha256)
            plugin.logger.info("Registered resource pack with builtin HTTP server at path: ${packConfig.distribution.builtinServer.path}")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to register builtin resource pack (Paper 1.20.5+ required): ${e.message}")
        }
    }

    private fun calculateSha256(file: File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var read = fis.read(buffer)
                while (read != -1) {
                    digest.update(buffer, 0, read)
                    read = fis.read(buffer)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun deleteRecursive(file: File) {
        file.walkTopDown().forEach { it.delete() }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024
        if (kb < 1024) return "$kb KB"
        val mb = kb / 1024
        return "$mb MB"
    }
}