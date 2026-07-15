package com.dli.assets.registry

import com.dli.assets.DLIAssets
import com.dli.assets.config.ConfigManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class AssetRegistry(private val plugin: DLIAssets) {

    private val configManager: ConfigManager = plugin.configManager
    private val namespace: String = configManager.getMainConfig().getString("general.namespace", "dliassets")
    private val cmdStart: Int = configManager.getMainConfig().getInt("assets.custom-model-data-start", 1000000)
    
    private val items = ConcurrentHashMap<String, RegisteredItem>()
    private val blocks = ConcurrentHashMap<String, RegisteredBlock>()
    private val mobs = ConcurrentHashMap<String, RegisteredMob>()
    private val cmdCounter = AtomicInteger(cmdStart)
    private val recipeRegistry = ConcurrentHashMap<NamespacedKey, Recipe>()

    fun loadAll() {
        plugin.logger.info("Registering assets...")
        items.clear()
        blocks.clear()
        mobs.clear()
        cmdCounter.set(cmdStart)
        recipeRegistry.keys.forEach { Bukkit.removeRecipe(it) }
        recipeRegistry.clear()
        
        registerItems()
        registerBlocks()
        registerMobs()
        registerRecipes()
        
        plugin.logger.info("Registered: ${items.size} items, ${blocks.size} blocks, ${mobs.size} mobs")
    }

    private fun registerItems() {
        val section = configManager.getItemsConfig().getConfigurationSection("items")
        section?.keys?.forEach { id ->
            val itemSection = section.getConfigurationSection(id) ?: return@forEach
            try {
                val material = itemSection.getString("material")?.let { Material.valueOf(it) } ?: Material.DIAMOND
                val displayName = itemSection.getString("displayname") ?: id
                val cmd = assignCustomModelData("$namespace:$id")
                
                items["$namespace:$id"] = RegisteredItem(
                    id = id,
                    namespacedId = "$namespace:$id",
                    material = material,
                    displayName = displayName,
                    customModelData = cmd,
                    entryData = itemSection
                )
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse item '$id': ${e.message}")
            }
        }
    }

    private fun assignCustomModelData(namespacedId: String): Int {
        return cmdCounter.incrementAndGet()
    }

    private fun registerBlocks() {
        val section = configManager.getBlocksConfig().getConfigurationSection("blocks")
        section?.keys?.forEach { id ->
            val blockSection = section.getConfigurationSection(id) ?: return@forEach
            try {
                val material = blockSection.getString("material")?.let { Material.valueOf(it) } ?: Material.NOTE_BLOCK
                val displayName = blockSection.getString("displayname") ?: id
                val cmd = assignCustomModelData("$namespace:$id")
                
                blocks["$namespace:$id"] = RegisteredBlock(
                    id = id,
                    namespacedId = "$namespace:$id",
                    material = material,
                    displayName = displayName,
                    customModelData = cmd,
                    entryData = blockSection
                )
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse block '$id': ${e.message}")
            }
        }
    }

    private fun registerMobs() {
        val section = configManager.getMobsConfig().getConfigurationSection("mobs")
        section?.keys?.forEach { id ->
            val mobSection = section.getConfigurationSection(id) ?: return@forEach
            try {
                val baseEntity = mobSection.getString("base-entity") ?: "ZOMBIE"
                val displayName = mobSection.getString("displayname") ?: id
                
                mobs["$namespace:$id"] = RegisteredMob(
                    id = id,
                    namespacedId = "$namespace:$id",
                    baseEntity = baseEntity,
                    displayName = displayName,
                    entryData = mobSection
                )
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse mob '$id': ${e.message}")
            }
        }
    }

    private fun registerRecipes() {
        recipeRegistry.keys.forEach { Bukkit.removeRecipe(it) }
        recipeRegistry.clear()
        
        val section = configManager.getRecipesConfig().getConfigurationSection("recipes")
        section?.keys?.forEach { id ->
            val recipeSection = section.getConfigurationSection(id) ?: return@forEach
            
            try {
                val type = recipeSection.getString("type") ?: "shaped"
                val resultItemId = recipeSection.getString("result.item") ?: return@forEach
                val resultAmount = recipeSection.getInt("result.amount", 1)
                
                val resultRegItem = getItem(resultItemId)
                val resultStack = resultRegItem?.createItemStack(resultAmount) 
                    ?: return@forEach
                
                val key = NamespacedKey(plugin, id)
                val recipe: Recipe? = when (type.lowercase()) {
                    "shaped" -> buildShapedRecipe(key, recipeSection, resultStack)
                    "shapeless" -> buildShapelessRecipe(key, recipeSection, resultStack)
                    else -> { plugin.logger.warning("Unsupported recipe type: $type for $id"); null }
                }
                
                recipe?.let {
                    recipeRegistry[key] = it
                    Bukkit.addRecipe(it)
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to parse recipe '$id': ${e.message}")
            }
        }
    }

    private fun buildShapedRecipe(key: NamespacedKey, section: org.bukkit.configuration.ConfigurationSection, result: ItemStack): ShapedRecipe? {
        val pattern = section.getStringList("pattern")
        if (pattern.isEmpty()) return null
        val keyMap = section.getConfigurationSection("key") ?: return null
        
        val recipe = ShapedRecipe(key, result)
        recipe.shape(*pattern.toTypedArray())
        
        keyMap.keys.forEach { char ->
            val ingSection = keyMap.getConfigurationSection(char)
            val ingItemId = ingSection?.getString("item") ?: return@forEach
            val ingredient = parseIngredient(ingItemId)
            ingredient?.let { recipe.setIngredient(char.first(), it) }
        }
        return recipe
    }

    private fun buildShapelessRecipe(key: NamespacedKey, section: org.bukkit.configuration.ConfigurationSection, result: ItemStack): ShapelessRecipe? {
        val ingredients = section.getList("ingredients")
        if (ingredients == null || ingredients.isEmpty()) return null
        
        val recipe = ShapelessRecipe(key, result)
        ingredients.forEach { ing ->
            val ingredient = parseIngredient(ing as String)
            ingredient?.let { recipe.addIngredient(it) }
        }
        return recipe
    }

    private fun parseIngredient(def: String): org.bukkit.inventory.Ingredient? {
        return if (def.contains(":")) {
            val parts = def.split(":")
            val item = getItem(parts[1], parts[0]) ?: getVanillaItem(parts[1])
            org.bukkit.inventory.Ingredient.of(item)
        } else {
            org.bukkit.inventory.Ingredient.of(getVanillaItem(def)!!)
        }
    }

    private fun getVanillaItem(id: String): ItemStack? {
        return try { ItemStack(Material.valueOf(id.toUpperCase())) } catch (e: Exception) {
            plugin.logger.warning("Unknown vanilla material: $id")
            null
        }
    }

    fun getItem(id: String, ns: String = namespace): RegisteredItem? = items["$ns:$id"]
    fun getBlock(id: String, ns: String = namespace): RegisteredBlock? = blocks["$ns:$id"]
    fun getMob(id: String, ns: String = namespace): RegisteredMob? = mobs["$ns:$id"]
    fun getAllItems(): Collection<RegisteredItem> = items.values
    fun getAllBlocks(): Collection<RegisteredBlock> = blocks.values
    fun getAllMobs(): Collection<RegisteredMob> = mobs.values
    fun getItemByCustomModelData(cmd: Int): RegisteredItem? = items.values.firstOrNull { it.customModelData == cmd }
    
    fun isCustomItem(stack: ItemStack?): Boolean {
        return stack?.itemMeta?.persistentDataContainer?.has(
            NamespacedKey(plugin, "dli_id"), PersistentDataType.STRING
        ) == true
    }

    fun getCustomItemId(stack: ItemStack): String? {
        return stack.itemMeta?.persistentDataContainer?.get(
            NamespacedKey(plugin, "dli_id"), PersistentDataType.STRING
        )
    }
    
    fun getRegisteredItem(stack: ItemStack): RegisteredItem? {
        val id = getCustomItemId(stack)
        return id?.let { items[it] }
    }

    data class RegisteredItem(
        val id: String,
        val namespacedId: String,
        val material: Material,
        val displayName: String,
        val customModelData: Int,
        val entryData: org.bukkit.configuration.ConfigurationSection
    ) {
        fun createItemStack(amount: Int = 1): ItemStack {
            val stack = ItemStack(material, amount)
            val meta = stack.itemMeta!!
            
            meta.displayName = plugin.miniMessage().deserialize(displayName)
            if (customModelData > 0) {
                meta.customModelData = customModelData
            }
            
            val pdc = meta.persistentDataContainer
            val key = NamespacedKey(plugin, "dli_id")
            pdc.set(key, PersistentDataType.STRING, namespacedId)
            
            stack.itemMeta = meta
            return stack
        }
        
        fun matches(stack: ItemStack?): Boolean {
            return stack?.let { getCustomItemId(it) == namespacedId } == true
        }
    }

    data class RegisteredBlock(
        val id: String,
        val namespacedId: String,
        val material: Material,
        val displayName: String,
        val customModelData: Int,
        val entryData: org.bukkit.configuration.ConfigurationSection
    )

    data class RegisteredMob(
        val id: String,
        val namespacedId: String,
        val baseEntity: String,
        val displayName: String,
        val entryData: org.bukkit.configuration.ConfigurationSection
    )
}