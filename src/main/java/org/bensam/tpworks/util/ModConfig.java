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
    
    @Name("Splash Potion Settings")
    @LangKey(LANG_PREFIX + ".splash_potion")
    public static SplashPotionSettings splashPotionSettings = new SplashPotionSettings();
    
    public static class SplashPotionSettings
    {
        @Comment("Teleport any boats within the potion splash radius")
        @LangKey(LANG_PREFIX + ".splash_potion.teleport_boats")
        public boolean teleportBoats = true;
        
        @Comment("Teleport boats only when they have passengers who can also be teleported")
        @LangKey(LANG_PREFIX + ".splash_potion.teleport_boats_when_ridden")
        public boolean teleportBoatsOnlyWhenRiddenByTeleportableEntity = true;
        
        @Comment("Teleport any hostile creatures (mobs) within the potion splash radius")
        @LangKey(LANG_PREFIX + ".splash_potion.teleport_hostiles")
        public boolean teleportHostileCreatures = true;
        
        @Comment("Teleport any minecarts within the potion splash radius")
        @LangKey(LANG_PREFIX + ".splash_potion.teleport_minecarts")
        public boolean teleportMinecarts = true;
        
        @Comment("Teleport minecarts only when they have passengers who can also be teleported")
        @LangKey(LANG_PREFIX + ".splash_potion.teleport_minecarts_when_ridden")
        public boolean teleportMinecartsOnlyWhenRiddenByTeleportableEntity = true;
        
        @Comment("Teleport any passive creatures within the potion splash radius")
        @LangKey(LANG_PREFIX + ".splash_potion.teleport_passives")
        public boolean teleportPassiveCreatures = true;
        
        @Comment("Teleport thrower if within the potion splash radius")
        @LangKey(LANG_PREFIX + ".splash_potion.teleport_player_thrower")
        public boolean teleportPlayerThrower = true;
        
        @Comment("Teleport other players within the potion splash radius")
        @LangKey(LANG_PREFIX + ".splash_potion.teleport_players_other")
        public boolean teleportPlayersOther = true;
        
        @Comment("Teleport any projectiles within the potion splash radius")
        @LangKey(LANG_PREFIX + ".splash_potion.teleport_projectiles")
        public boolean teleportProjectiles = false;
    }
    
    @Name("World Settings")
    @LangKey(LANG_PREFIX + ".world")
    public static WorldSettings worldSettings = new WorldSettings();
    
    public static class WorldSettings
    {
        @Comment({"Add basic Teleportation items to Spawn Chest", "Must be set BEFORE generating a new world"})
        @LangKey(LANG_PREFIX + ".world.spawn_chest")
        @RequiresWorldRestart
        public boolean addItemsToSpawnChest = false;
        
        public enum CraftingDifficulty { NORMAL, HARD };
        @Comment("HARD difficulty requires an Eye of Ender instead of an Ender Pearl in wands, bows, and beacons")
        @LangKey(LANG_PREFIX + ".world.crafting_difficulty")
        public CraftingDifficulty craftingDifficulty = CraftingDifficulty.NORMAL;
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
