package com.clanjhoo.trialspawnerlootfix.listeners;

import com.clanjhoo.trialspawnerlootfix.TrialSpawnerLootFix;
import com.clanjhoo.trialspawnerlootfix.capsule.WorldGuardCapsule;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TrialSpawner;
import org.bukkit.block.Vault;
import org.bukkit.block.data.type.Vault.State;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.spawner.TrialSpawnerConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;


public class TrialSpawnerListener implements Listener {
    private final TrialSpawnerLootFix plugin;
    private final Random rng;
    private final NamespacedKey keyTag;

    public final static Map<LootTable, Integer> DEFAULT_OMINOUS_SPAWNER_LOOT = Map.of(
            LootTables.OMINOUS_TRIAL_CHAMBER_KEY.getLootTable(), 3,
            LootTables.OMINOUS_TRIAL_CHAMBER_CONSUMABLES.getLootTable(), 7
    );

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

        TrialSpawner trialSpawner = (TrialSpawner) event.getBlock().getState(false);
        TrialSpawnerConfiguration config = trialSpawner.isOminous() ?
                trialSpawner.getOminousConfiguration() :
                trialSpawner.getNormalConfiguration();
        if (trialSpawner.isOminous()) {
            if (!config.getPossibleRewards().containsKey(LootTables.OMINOUS_TRIAL_CHAMBER_KEY.getLootTable()) ||
                    !config.getPossibleRewards().containsKey(LootTables.OMINOUS_TRIAL_CHAMBER_CONSUMABLES.getLootTable())) {
                config.setPossibleRewards(DEFAULT_OMINOUS_SPAWNER_LOOT);
            }
            else {
                return;
            }
        }

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
                        .Builder(location.clone().add(0, 1, 0))
                        .build()
        );

        final String theTag = WorldGuardCapsule.getFlagValue(location);
        if (theTag != null && !theTag.isEmpty()) {
            for (ItemStack item : newDropList) {
                if (item.getType() != Material.OMINOUS_TRIAL_KEY && item.getType() != Material.TRIAL_KEY)
                    continue;
                item.editPersistentDataContainer(container -> container.set(keyTag, PersistentDataType.STRING, theTag));
            }
        }

        event.setDispensedLoot(new ArrayList<>(newDropList));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTrialVaultInteract(VaultChangeStateEvent event) {
        // Check if vault
        Block block = event.getBlock();
        Vault theVault = (Vault) block.getState(false);

        // Set expected key tag
        ItemStack expectedKey = theVault.getKeyItem();
        String currentTag = expectedKey.getPersistentDataContainer().get(keyTag, PersistentDataType.STRING);
        String expectedTag = WorldGuardCapsule.getFlagValue(block.getLocation());

        if ((expectedTag != null && !expectedTag.equals(currentTag)) ||
                (currentTag != null && !currentTag.equals(expectedTag))) {
            plugin.log(Level.INFO, expectedTag);
            plugin.log(Level.INFO, currentTag);
            expectedKey.editPersistentDataContainer((container) -> {
                if (expectedTag != null)
                    container.set(keyTag, PersistentDataType.STRING, expectedTag);
                else
                    container.remove(keyTag);
            });
            theVault.setKeyItem(expectedKey);

            if (event.getNewState() == State.UNLOCKING) {
                event.getPlayer().sendRichMessage("<yellow>Key item updated! Please try unlocking again");
                event.setCancelled(true);
            }
        }
    }
}
