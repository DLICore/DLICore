package com.dli.assets.command

import com.dli.assets.DLIAssets
import com.dli.assets.config.ConfigManager
import com.dli.assets.registry.AssetRegistry
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.Collections

class DLIAssetsCommand(private val plugin: DLIAssets) : CommandExecutor, TabCompleter {

    private val configManager: ConfigManager = plugin.configManager
    private val assetRegistry: AssetRegistry = plugin.assetRegistry
    private val itemManager = plugin.customItemManager
    private val packManager = plugin.resourcePackManager

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        val subCommand = args[0].lowercase()
        val subArgs = args.copyOfRange(1, args.size)

        return when (subCommand) {
            "reload" -> handleReload(sender)
            "give" -> handleGive(sender, subArgs)
            "giveall" -> handleGiveAll(sender, subArgs)
            "pack" -> handlePack(sender, subArgs)
            "list" -> handleList(sender, subArgs)
            "info" -> handleInfo(sender, subArgs)
            "spawn" -> handleSpawn(sender, subArgs)
            "gui" -> handleGui(sender, subArgs)
            "debug" -> handleDebug(sender, subArgs)
            else -> {
                sender.sendMessage(plugin.miniMessage().deserialize("<red>Unknown subcommand. Use /dliassets help"))
                true
            }
        }
    }

    private fun handleReload(sender: CommandSender): Boolean {
        if (!sender.hasPermission("dliassets.admin.reload")) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>No permission."))
            return true
        }
        
        sender.sendMessage(plugin.miniMessage().deserialize("<yellow>Reloading DLIAssets..."))
        plugin.reloadPlugin()
        sender.sendMessage(plugin.miniMessage().deserialize("<green>Reload complete!"))
        return true
    }

    private fun handleGive(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("dliassets.give")) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>No permission."))
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>Usage: /dliassets give <player> <namespace:id> [amount]"))
            return true
        }
        
        val targetName = args[0]
        val itemId = args[1]
        val amount = args.getOrElse(2) { "1" }.toIntOrNull() ?: 1
        
        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>Player '$targetName' not found."))
            return true
        }
        
        val success = itemManager.giveCustomItem(target, itemId, amount)
        if (success) {
            sender.sendMessage(plugin.miniMessage().deserialize("<green>Gave <white>$amount</white>x <gold>$itemId</gold> to <white>${target.name}</white>"))
            target.sendMessage(plugin.miniMessage().deserialize("<gray>Received <gold>$itemId</gold> x$amount"))
        } else {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>Item not found: $itemId"))
        }
        return true
    }

    private fun handleGiveAll(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("dliassets.admin.give")) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>No permission."))
            return true
        }
        
        val target = if (args.isNotEmpty()) Bukkit.getPlayer(args[0]) else sender as? Player
        val player = target ?: return false
        
        var count = 0
        assetRegistry.getAllItems().forEach { regItem ->
            itemManager.giveCustomItem(player, regItem.namespacedId, 1)
            count++
        }
        
        sender.sendMessage(plugin.miniMessage().deserialize("<green>Gave all <white>$count</white> custom items to <white>${player.name}</white>"))
        return true
    }

    private fun handlePack(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("dliassets.admin.pack")) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>No permission."))
            return true
        }
        
        val action = args.getOrElse(0) { "regenerate" }
        
        when (action) {
            "regenerate", "generate" -> {
                sender.sendMessage(plugin.miniMessage().deserialize("<yellow>Regenerating resource pack..."))
                packManager.generatePack()
                sender.sendMessage(plugin.miniMessage().deserialize("<green>Resource pack generated!"))
            }
            "hash" -> sender.sendMessage(plugin.miniMessage().deserialize("<aqua>Pack SHA-256: ${packManager.getPackSha256()}"))
            "url" -> sender.sendMessage(plugin.miniMessage().deserialize("<aqua>Pack URL: ${packManager.getPackUrl() ?: "Not available"}"))
            else -> sender.sendMessage(plugin.miniMessage().deserialize("<red>Unknown pack action: $action"))
        }
        return true
    }

    private fun handleList(sender: CommandSender, args: Array<String>): Boolean {
        val category = args.getOrElse(0) { "items" }.lowercase()
        
        when (category) {
            "items", "item" -> {
                sender.sendMessage(plugin.miniMessage().deserialize("<gold>=== Custom Items (${assetRegistry.getAllItems().size}) ==="))
                assetRegistry.getAllItems().forEach { item ->
                    sender.sendMessage(plugin.miniMessage().deserialize("  <white>${item.namespacedId}</white> <gray>(CMD: ${item.customModelData})</gray> - ${stripMM(item.entry.displayName)}"))
                }
            }
            "blocks", "block" -> {
                sender.sendMessage(plugin.miniMessage().deserialize("<gold>=== Custom Blocks (${assetRegistry.getAllBlocks().size}) ==="))
                assetRegistry.getAllBlocks().forEach { block ->
                    sender.sendMessage(plugin.miniMessage().deserialize("  <white>${block.namespacedId}</white> <gray>(Base: ${block.entry.material})</gray>"))
                }
            }
            "mobs", "mob", "entities" -> {
                sender.sendMessage(plugin.miniMessage().deserialize("<gold>=== Custom Mobs (${assetRegistry.getAllMobs().size}) ==="))
                assetRegistry.getAllMobs().forEach { mob ->
                    sender.sendMessage(plugin.miniMessage().deserialize("  <white>${mob.namespacedId}</white> <gray>(Base: ${mob.entry.baseEntity})</gray>"))
                }
            }
            else -> sender.sendMessage(plugin.miniMessage().deserialize("<red>Categories: items, blocks, mobs"))
        }
        return true
    }

    private fun handleInfo(sender: CommandSender, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>Usage: /dliassets info <namespace:id>"))
            return true
        }
        
        val id = args[0]
        val item = assetRegistry.getItem(id)
        val block = assetRegistry.getBlock(id)
        val mob = assetRegistry.getMob(id)
        
        item?.let {
            sender.sendMessage(plugin.miniMessage().deserialize("<gold>=== Item Info: ${it.namespacedId} ==="))
            sender.sendMessage(plugin.miniMessage().deserialize("  Material: ${it.material}"))
            sender.sendMessage(plugin.miniMessage().deserialize("  CMD: ${it.customModelData}"))
            sender.sendMessage(plugin.miniMessage().deserialize("  Name: ${stripMM(it.entry.displayName)}"))
            return true
        }
        
        block?.let {
            sender.sendMessage(plugin.miniMessage().deserialize("<gold>=== Block Info: ${it.namespacedId} ==="))
            sender.sendMessage(plugin.miniMessage().deserialize("  Base Material: ${it.material}"))
            return true
        }
        
        mob?.let {
            sender.sendMessage(plugin.miniMessage().deserialize("<gold>=== Mob Info: ${it.namespacedId} ==="))
            sender.sendMessage(plugin.miniMessage().deserialize("  Base Entity: ${it.baseEntity}"))
            return true
        }
        
        sender.sendMessage(plugin.miniMessage().deserialize("<red>Asset not found: $id"))
        return true
    }

    private fun handleSpawn(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("dliassets.admin.spawn")) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>No permission."))
            return true
        }
        
        if (args.size < 1) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>Usage: /dliassets spawn <mob_id> [player]"))
            return true
        }
        
        val mobId = args[0]
        val target = if (args.size > 1) Bukkit.getPlayer(args[1]) else sender as? Player
        val player = target ?: return false
        
        val mob = assetRegistry.getMob(mobId)
        if (mob == null) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>Mob not found: $mobId"))
            return true
        }
        
        sender.sendMessage(plugin.miniMessage().deserialize("<yellow>Mob spawning not fully implemented yet. Use /summon with custom NBT."))
        return true
    }

    private fun handleGui(sender: CommandSender, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>Only players can open GUIs."))
            return true
        }
        
        val guiId = args.getOrElse(0) { "main_menu" }
        sender.sendMessage(plugin.miniMessage().deserialize("<yellow>GUI System not fully implemented yet."))
        return true
    }

    private fun handleDebug(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("dliassets.admin.debug")) return true
        
        val action = args.getOrElse(0) { "info" }
        
        when (action) {
            "config" -> sender.sendMessage(plugin.miniMessage().deserialize("<aqua>Configs loaded: Items(${assetRegistry.getAllItems().size}), Blocks(${assetRegistry.getAllBlocks().size}), Mobs(${assetRegistry.getAllMobs().size})"))
            "pack" -> {
                sender.sendMessage(plugin.miniMessage().deserialize("<aqua>Pack File: ${packManager.getPackFile()?.absolutePath ?: "Not generated"}"))
                sender.sendMessage(plugin.miniMessage().deserialize("<aqua>Pack Hash: ${packManager.getPackSha256()}"))
            }
            "registry" -> sender.sendMessage(plugin.miniMessage().deserialize("<aqua>Item Registry Size: ${assetRegistry.items.size}"))
        }
        return true
    }

    private fun sendHelp(sender: CommandSender) {
        val mm = plugin.miniMessage()
        sender.sendMessage(mm.deserialize("<gradient:#ff0000:#ff8800>=== DLIAssets Commands ==="))
        sender.sendMessage(mm.deserialize("<yellow>/dliassets reload</yellow> <gray>- Reload all configs & assets"))
        sender.sendMessage(mm.deserialize("<yellow>/dliassets give <player> <id> [amount]</yellow> <gray>- Give custom item"))
        sender.sendMessage(mm.deserialize("<yellow>/dliassets giveall [player]</yellow> <gray>- Give all items to player"))
        sender.sendMessage(mm.deserialize("<yellow>/dliassets pack [regenerate|hash|url]</yellow> <gray>- Resource pack management"))
        sender.sendMessage(mm.deserialize("<yellow>/dliassets list [items|blocks|mobs]</yellow> <gray>- List registered assets"))
        sender.sendMessage(mm.deserialize("<yellow>/dliassets info <id></yellow> <gray>- Show asset details"))
        sender.sendMessage(mm.deserialize("<yellow>/dliassets spawn <mob_id> [player]</yellow> <gray>- Spawn custom mob"))
        sender.sendMessage(mm.deserialize("<yellow>/dliassets gui <id></yellow> <gray>- Open custom GUI"))
        sender.sendMessage(mm.deserialize("<yellow>/dliassets debug [config|pack|registry]</yellow> <gray>- Debug info"))
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (args.size == 1) {
            return listOf("reload", "give", "giveall", "pack", "list", "info", "spawn", "gui", "debug")
                .filter { it.startsWith(args[0].lowercase()) }
        }
        
        val sub = args[0].lowercase()
        return when (sub) {
            "give", "info" -> {
                if (args.size == 2) {
                    if (sub == "give") Bukkit.onlinePlayers.map { it.name }.filter { it.startsWith(args[1], true) }
                    else assetRegistry.items.keys.filter { it.contains(args[1], true) } +
                         assetRegistry.blocks.keys.filter { it.contains(args[1], true) } +
                         assetRegistry.mobs.keys.filter { it.contains(args[1], true) }
                } else if (sub == "give" && args.size == 3) {
                    assetRegistry.items.keys.filter { it.contains(args[2], true) }
                } else Collections.emptyList()
            }
            "pack" -> if (args.size == 2) listOf("regenerate", "hash", "url").filter { it.startsWith(args[1]) } else Collections.emptyList()
            "list" -> if (args.size == 2) listOf("items", "blocks", "mobs").filter { it.startsWith(args[1]) } else Collections.emptyList()
            "spawn" -> if (args.size == 2) assetRegistry.mobs.keys.filter { it.contains(args[1], true) } else if (args.size == 3) Bukkit.onlinePlayers.map { it.name }.filter { it.startsWith(args[2], true) } else Collections.emptyList()
            "gui" -> if (args.size == 2) configManager.getGuisConfig().getKeys(false).filter { it.contains(args[1], true) } else Collections.emptyList()
            else -> Collections.emptyList()
        }
    }

    private fun stripMM(input: String): String {
        return input.replace("<[^>]+>".toRegex(), "")
    }
}