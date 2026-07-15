package com.dli.assets.item

import com.dli.assets.DLIAssets
import com.dli.assets.registry.AssetRegistry
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

class CustomItemManager(private val plugin: DLIAssets) : Listener {

    private val assetRegistry: AssetRegistry = plugin.assetRegistry
    private val configManager = plugin.configManager
    private val interactionCooldowns = ConcurrentHashMap<String, Long>()
    private val cooldownMs = configManager.getMainConfig().getLong("assets.interaction-cooldown", 200)

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        
        val action = event.action
        val item = event.item
        
        val regItem = assetRegistry.getRegisteredItem(item!!) ?: return
        
        event.useInteractedBlock = org.bukkit.event.Event.Result.DENY
        event.useItemInHand = org.bukkit.event.Event.Result.DENY
        
        val player = event.player
        val namespacedId = regItem.namespacedId
        
        val cooldownKey = "${player.uniqueId}:$namespacedId"
        val now = System.currentTimeMillis()
        if (interactionCooldowns.getOrDefault(cooldownKey, 0L) > now) {
            return
        }
        interactionCooldowns[cooldownKey] = now + cooldownMs
        
        when (action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                triggerMechanics(player, regItem, "on_right_click", event)
            }
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                triggerMechanics(player, regItem, "on_left_click", event)
            }
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        // Handle custom item clicks in GUIs
    }

    @EventHandler
    fun onItemHeldChange(event: PlayerItemHeldEvent) {
        val player = event.player
        val newItem = player.inventory.getItem(event.newSlot)
        val oldItem = player.inventory.getItem(event.oldSlot)
        
        assetRegistry.getRegisteredItem(oldItem)?.let { regItem ->
            triggerMechanics(player, regItem, "on_unequip", null)
        }
        
        assetRegistry.getRegisteredItem(newItem)?.let { regItem ->
            triggerMechanics(player, regItem, "on_equip", null)
        }
    }

    private fun triggerMechanics(
        player: Player, 
        regItem: AssetRegistry.RegisteredItem, 
        trigger: String, 
        event: org.bukkit.event.Event?
    ) {
        val mechanics = regItem.entryData.getConfigurationSection("mechanics")
        val triggerMechanics = mechanics?.getConfigurationSection(trigger)
        if (triggerMechanics == null) return
        
        val mechList = triggerMechanics.getKeys(false)
        mechList.forEach { key ->
            val mechSection = triggerMechanics.getConfigurationSection(key) ?: return@forEach
            try {
                executeMechanic(player, regItem, mechSection, event)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to execute mechanic '$trigger' for ${regItem.namespacedId}: ${e.message}")
            }
        }
    }

    private fun executeMechanic(
        player: Player, 
        regItem: AssetRegistry.RegisteredItem, 
        mech: org.bukkit.configuration.ConfigurationSection, 
        event: org.bukkit.event.Event?
    ) {
        val type = mech.getString("type") ?: return
        
        when (type.lowercase()) {
            "particle" -> {
                val particleName = mech.getString("particle") ?: "redstone"
                val count = mech.getInt("count", 10)
                val offset = mech.getDouble("offset", 0.5)
                val color = mech.getString("color")
                
                val particle = org.bukkit.Particle.valueOf(particleName.toUpperCase())
                val data = if (color != null && particle == org.bukkit.Particle.REDSTONE) {
                    val c = org.bukkit.Color.fromRGB(Integer.parseInt(color.replace("#", ""), 16))
                    org.bukkit.Particle.DustOptions(c, 1f)
                } else null
                
                player.world.spawnParticle(particle, player.location.add(0, 1, 0), count, offset, offset, offset, 0.1, data)
            }
            "sound" -> {
                val soundName = mech.getString("sound") ?: "entity.experience_orb.pickup"
                val volume = mech.getDouble("volume", 1.0).toFloat()
                val pitch = mech.getDouble("pitch", 1.0).toFloat()
                player.world.playSound(player.location, soundName, volume, pitch)
            }
            "command" -> {
                val cmd = mech.getString("command") ?: return
                val asConsole = mech.getBoolean("console", false)
                val finalCmd = cmd.replace("{player}", player.name).replace("{uuid}", player.uniqueId.toString())
                if (asConsole) Bukkit.dispatchCommand(Bukkit.consoleSender, finalCmd)
                else player.performCommand(finalCmd)
            }
            "message" -> {
                val msg = mech.getString("message") ?: return
                val actionbar = mech.getBoolean("actionbar", false)
                val parsed = plugin.miniMessage().deserialize(msg)
                if (actionbar) player.sendActionBar(parsed)
                else player.sendMessage(parsed)
            }
            "give_item" -> {
                val itemId = mech.getString("item") ?: return
                val amount = mech.getInt("amount", 1)
                val targetItem = assetRegistry.getItem(itemId)?.createItemStack(amount) ?: return
                player.inventory.addItem(targetItem)
            }
            "damage_item" -> {
                val amount = mech.getInt("amount", 1)
                val item = event?.let { (it as? PlayerInteractEvent)?.item } ?: player.inventory.itemInMainHand
                if (!item.type.isAir()) {
                    item.durability = (item.durability + amount).toShort()
                }
            }
            "consume" -> {
                val item = event?.let { (it as? PlayerInteractEvent)?.item } ?: player.inventory.itemInMainHand
                if (!item.type.isAir() && item.amount > 1) item.amount--
                else if (!item.type.isAir()) item.type = Material.AIR
            }
            "cooldown" -> {
                val ticks = mech.getInt("ticks", 20)
                val key = mech.getString("key") ?: regItem.namespacedId
                player.setCooldown(regItem.material, ticks)
            }
        }
    }

    fun createCustomItem(id: String, amount: Int = 1): ItemStack? {
        return assetRegistry.getItem(id)?.createItemStack(amount)
    }

    fun giveCustomItem(player: Player, id: String, amount: Int = 1): Boolean {
        val item = createCustomItem(id, amount) ?: return false
        val leftover = player.inventory.addItem(item)
        return leftover.isEmpty()
    }

    fun isCustomItem(stack: ItemStack?): Boolean {
        return stack?.let { assetRegistry.isCustomItem(it) } ?: false
    }

    fun getCustomItemId(stack: ItemStack?): String? {
        return stack?.let { assetRegistry.getCustomItemId(it) }
    }

    fun reload() {
        interactionCooldowns.clear()
        plugin.logger.fine("CustomItemManager reloaded")
    }
}