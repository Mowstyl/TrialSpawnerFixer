package com.clanjhoo.trialspawnerlootfix.listeners;

import com.clanjhoo.trialspawnerlootfix.TrialSpawnerLootFix;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TrialSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.spawner.TrialSpawnerConfiguration;

import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;


public class TrialSpawnerListener implements Listener {
    private final TrialSpawnerLootFix plugin;
    private final Random rng;
    private final NamespacedKey keyTag;

    /*
     * Default trables:
     * TRIAL_CHAMBER_KEY -> Trial spawner drops a normal key
     * TRIAL_CHAMBER_CONSUMABLES -> Trial spawner drops an item
     * OMINOUS_TRIAL_CHAMBER_KEY -> Ominous trial spawner drops a normal key
     * OMINOUS_TRIAL_CHAMBER_CONSUMABLES -> Ominous trial spawner drops an item
     * TRIAL_CHAMBER_ITEMS_TO_DROP_WHEN_OMINOUS -> Ominous trial spawner shoots an item
     */

    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //

    public TrialSpawnerListener(TrialSpawnerLootFix plugin) {
        this.plugin = plugin;
        this.rng = new Random();
        this.keyTag = new NamespacedKey(plugin, "keytags");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTrialSpawnerLoot(BlockDispenseLootEvent event) {
        if (event.getBlock().getType() != Material.TRIAL_SPAWNER)
            return;

        TrialSpawner trialSpawner = (TrialSpawner) event.getBlock().getState();
        TrialSpawnerConfiguration config = trialSpawner.isOminous() ?
                trialSpawner.getOminousConfiguration() :
                trialSpawner.getNormalConfiguration();

        int total = config.getPossibleRewards().values().stream().reduce(Integer::sum).orElse(0);
        int acc = 0;
        int selected = rng.nextInt(0, total);
        LootTable picked = null;
        for (Map.Entry<LootTable, Integer> entry : config.getPossibleRewards().entrySet()) {
            acc += entry.getValue();
            if (selected < acc) {
                picked = entry.getKey();
                break;
            }
        }

        Location location = event.getBlock().getLocation();
        if (picked == null) {
            if (total != 0) {
                plugin.log(Level.WARNING, "Error picking LootTable for spawner in " + location);
                event.setCancelled(true);
            }
            else {
                plugin.log(Level.INFO, "No LootTable found for spawner in " + location);
                event.setDispensedLoot(List.of());
            }
            return;
        }

        Collection<ItemStack> newDropList = picked.populateLoot(
                rng,
                new LootContext
                        .Builder(location)
                        .build()
        );

        if (TrialSpawnerLootFix.VAULT_KEY_TAG != null) {
            ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .createQuery()
                    .getApplicableRegions(BukkitAdapter.adapt(location));
            Collection<String> keyTags = set.queryAllValues(null, TrialSpawnerLootFix.VAULT_KEY_TAG);
            final String allTags;
            if (keyTags != null && keyTags.isEmpty()) {
                StringBuilder allTagsBuilder = new StringBuilder().append(";");
                for (String tag : keyTags) {
                    if (tag != null && !tag.isBlank())
                        allTagsBuilder.append(tag).append(";");
                }
                if (allTagsBuilder.length() > 1)
                    allTags = null;
                else
                    allTags = allTagsBuilder.toString();
            }
            else {
                allTags = null;
            }

            if (allTags != null) {
                for (ItemStack item : newDropList) {
                    if (item.getType() != Material.OMINOUS_TRIAL_KEY && item.getType() != Material.TRIAL_KEY)
                        continue;
                    item.editPersistentDataContainer(container -> container.set(keyTag, PersistentDataType.STRING, allTags));
                }
            }
        }

        event.setDispensedLoot(new ArrayList<>(newDropList));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTrialVaultInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        // Check if vault
        Block theVault = event.getClickedBlock();
        if (theVault == null || theVault.getType() != Material.VAULT)
            return;

        // Check if holding item
        ItemStack theKey = event.getItem();
        if (theKey == null)
            return;

        // Get expected key item
        BlockData vaultData = theVault.getBlockData();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(vaultData.getAsString(), type);
        Object rawConfig = map.get("config");
        ItemStack expectedKey = null;
        if (rawConfig != null) {
            Map<String, Object> config = (Map<String, Object>) rawConfig;
            Object rawKeyItem = config.get("key_item");
            if (rawKeyItem != null) {
                Map<String, Object> keyItem = (Map<String, Object>) rawKeyItem;
                expectedKey = ItemStack.deserialize(keyItem);
            }
        }
        if (expectedKey == null) {
            boolean isOminous = vaultData.getAsString().toLowerCase().contains("ominous=true");
            expectedKey = isOminous ? new ItemStack(Material.OMINOUS_TRIAL_KEY) : new ItemStack(Material.TRIAL_KEY);
        }

        // Check key item and quantity
        if (!theKey.isSimilar(expectedKey)) {
            event.getPlayer().sendRichMessage("<red>Wrong key!");
            event.setCancelled(true);
            return;
        }
        if (theKey.getAmount() >= expectedKey.getAmount()) {
            event.getPlayer().sendRichMessage("<red>Insufficient keys!");
            event.setCancelled(true);
            return;
        }

        // Check region tags
        Location location = theVault.getLocation();
        if (TrialSpawnerLootFix.VAULT_KEY_TAG != null) {
            ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .createQuery()
                    .getApplicableRegions(BukkitAdapter.adapt(location));
            Collection<String> keyTags = set.queryAllValues(null, TrialSpawnerLootFix.VAULT_KEY_TAG);
            boolean fail = false;

            if (keyTags != null && !keyTags.isEmpty()) {
                String allTags = theKey.getPersistentDataContainer().get(keyTag, PersistentDataType.STRING);
                for (String tag : keyTags) {
                    if (!allTags.contains(";" + tag + ";")) {
                        fail = true;
                        break;
                    }
                }
            }

            if (fail) {
                event.getPlayer().sendRichMessage("<red>Wrong key!");
                event.setCancelled(true);
            }
        }
    }
}
