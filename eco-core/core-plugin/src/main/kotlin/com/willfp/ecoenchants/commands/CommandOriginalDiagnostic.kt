package com.willfp.ecoenchants.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecoenchants.plugin
import com.willfp.ecoenchants.progression.OriginalRuntime
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

object CommandOriginalDiagnostic : Subcommand(plugin, "diagnosticoriginal", "ecoenchants.command.reload", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.size != 1) { sender.sendMessage("Usage: /ecoenchants diagnosticoriginal <online-player|stop>"); return }
        if (args[0] == "stop") { OriginalRuntime.stopDiagnostic(); sender.sendMessage("Original hit diagnostic stopped."); return }
        val player = Bukkit.getPlayerExact(args[0])
        if (player == null) { sender.sendMessage("Player must be online."); return }
        OriginalRuntime.startDiagnostic(player.uniqueId)
        sender.sendMessage("Original hit diagnostic: ${player.name}, 30 seconds, at most 8 impacts. Read the server log.")
    }
}
