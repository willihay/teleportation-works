package org.bensam.tpworks.util;

import org.bensam.tpworks.TeleportationWorks;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.RequiresWorldRestart;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author WilliHay
 *
 */
@Config(modid = TeleportationWorks.MODID, category = "")
public final class ModConfig
{
    private static final String LANG_PREFIX = "config." + TeleportationWorks.MODID;
    
    @Name("World Settings")
    @LangKey(LANG_PREFIX + ".world")
    public static WorldSettings worldSettings = new WorldSettings();
    
    public static class WorldSettings
    {
        @Comment({"Add basic Teleportation items to Spawn Chest", "Must be set BEFORE generating a new world"})
        @LangKey(LANG_PREFIX + ".world.spawn_chest")
        @RequiresWorldRestart
        public boolean addItemsToSpawnChest = false;        
    }
    
    @Mod.EventBusSubscriber(modid = TeleportationWorks.MODID)
    private static class EventHandler
    {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
        {
            if (event.getModID().equals(TeleportationWorks.MODID))
            {
                ConfigManager.sync(TeleportationWorks.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
