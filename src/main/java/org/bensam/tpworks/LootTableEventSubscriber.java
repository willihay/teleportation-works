package org.bensam.tpworks;

import org.bensam.tpworks.util.ModConfig;
import org.bensam.tpworks.util.ModConfig.WorldSettings;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryTable;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author WilliHay
 *
 */
@Mod.EventBusSubscriber(modid = TeleportationWorks.MODID)
public class LootTableEventSubscriber
{
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event)
    {
        if (ModConfig.worldSettings.addItemsToSpawnChest)
        {
            if (event.getName().toString().equals("minecraft:chests/spawn_bonus_chest"))
            {
                TeleportationWorks.MOD_LOGGER.debug("Found spawn_bonus_chest");
                LootEntryTable entry = new LootEntryTable(
                        new ResourceLocation(TeleportationWorks.MODID, "chests/spawn_bonus_chest"), 1, 0, new LootCondition[0],
                        "teleportation_works_inject_entry");
                LootPool pool = new LootPool(new LootEntry[] { entry }, new LootCondition[0], new RandomValueRange(1),
                        new RandomValueRange(0), "teleportation_works_inject_pool");

                event.getTable().addPool(pool);
            }
        }
    }
}
