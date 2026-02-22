package com.clanjhoo.trialspawnerlootfix;


import com.clanjhoo.trialspawnerlootfix.listeners.TrialSpawnerListener;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class TrialSpawnerLootFix extends JavaPlugin {

	private AudienceProvider adventure;
	private TrialSpawnerListener listener;
	public static StringFlag VAULT_KEY_TAG;


	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void onLoad() {
		// Register flags
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		StringFlag auxFlag;
		String flagName;
		flagName = "vault-key-tag";
		try {
			auxFlag = new StringFlag(flagName, "");
			registry.register(auxFlag);
			VAULT_KEY_TAG = auxFlag; // only set our field if there was no error
		} catch (FlagConflictException e) {
			// some other plugin registered a flag by the same name already.
			// you can use the existing flag, but this may cause conflicts - be sure to check type
			Flag<?> existing = registry.get(flagName);
			if (existing instanceof StringFlag) {
				VAULT_KEY_TAG = (StringFlag) existing;
			}
			else {
				// types don't match - this is bad news! some other plugin conflicts with you
				// hopefully this never actually happens
			}
		}
	}

	@Override
	public void onEnable() {
		// Initialize an audiences instance for the plugin
		this.adventure = BukkitAudiences.create(this);

		// Listener
		listener = new TrialSpawnerListener(this);
		Bukkit.getPluginManager().registerEvents(listener, this);
	}

	public void log(Level level, String rawMessage) {
		if (adventure == null) {
			getLogger().log(level, rawMessage);
			return;
		}
		rawMessage = "[" + getName() + "] " + rawMessage;
		TextColor color = null;
		if (level == Level.SEVERE)
			color = NamedTextColor.RED;
		else if (level == Level.WARNING)
			color = NamedTextColor.YELLOW;
		else if (level == Level.CONFIG)
			color = NamedTextColor.GREEN;

		Component message = MiniMessage.miniMessage().deserialize(rawMessage);
		if (color != null)
			message = message.colorIfAbsent(color);

		adventure.console().sendMessage(message);
	}

	public static TrialSpawnerLootFix getInstance() {
		return (TrialSpawnerLootFix) Bukkit.getPluginManager().getPlugin("TrialSpawnerLootFix");
	}

	@Override
	public void onDisable() {
		if(this.adventure != null) {
			this.adventure.close();
			this.adventure = null;
		}
	}
}