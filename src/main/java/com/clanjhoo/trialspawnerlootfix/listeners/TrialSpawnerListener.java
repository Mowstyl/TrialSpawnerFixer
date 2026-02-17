package com.clanjhoo.trialspawnerlootfix.listeners;

import com.clanjhoo.trialspawnerlootfix.TrialSpawnerLootFix;
import org.bukkit.Material;
import org.bukkit.block.TrialSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;


public class TrialSpawnerListener implements Listener {
    private final TrialSpawnerLootFix plugin;
    private final Random rng;

    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //

    public TrialSpawnerListener(TrialSpawnerLootFix plugin) {
        this.plugin = plugin;
        this.rng = new Random();
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onTrialSpawnerLoot(BlockDispenseLootEvent event) {
        if (event.getBlock().getType() != Material.TRIAL_SPAWNER)
            return;
        TrialSpawner trialSpawner = (TrialSpawner) event.getBlock().getState();
        if (!trialSpawner.isOminous())
            return;
        if (event.getLootTable().toString().contains("ominous"))
            return;

        LootTables rawTable = LootTables.OMINOUS_TRIAL_CHAMBER_CONSUMABLES;
        if (rng.nextInt(0, 10) < 3)
            rawTable = LootTables.OMINOUS_TRIAL_CHAMBER_KEY;
        LootTable ominousSpawnerTable = rawTable.getLootTable();

        Collection<ItemStack> newDropList = ominousSpawnerTable.populateLoot(
                rng,
                new LootContext
                        .Builder(event.getBlock().getLocation().add(0, 1, 0))
                        .build()
                );
        event.setDispensedLoot(new ArrayList<>(newDropList));
    }
}
