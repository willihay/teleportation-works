package org.bensam.tpworks.util;

import org.bensam.tpworks.TeleportationWorks;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.RangeInt;
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
    
    @Name("Equipped Item Settings")
    @LangKey(LANG_PREFIX + ".equipped_item")
    public static EquippedItemSettings equippedItemSettings = new EquippedItemSettings();
    
    public static class EquippedItemSettings
    {
        @Comment("When cycling through Bow of Teleportation destinations, include saved Teleport Rails")
        @LangKey(LANG_PREFIX + ".equipped_item.bow_destinations_include_rails")
        public boolean bowDestinationsIncludeRails = false;
        
        @Comment("Wand of Teleportation also teleports boats that a player is riding")
        @LangKey(LANG_PREFIX + ".equipped_item.wand_teleports_boats_ridden")
        public boolean wandTeleportsBoatsRidden = true;
        
        @Comment("Wand of Teleportation also teleports creatures that a player is riding")
        @LangKey(LANG_PREFIX + ".equipped_item.wand_teleports_creatures_ridden")
        public boolean wandTeleportsCreaturesRidden = true;
        
        @Comment("Wand of Teleportation also teleports minecarts that a player is riding")
        @LangKey(LANG_PREFIX + ".equipped_item.wand_teleports_minecarts_ridden")
        public boolean wandTeleportsMinecartsRidden = true;
    }
    
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
    
    @Name("Teleport Block Settings")
    @LangKey(LANG_PREFIX + ".teleport_block")
    public static TeleportBlockSettings teleportBlockSettings = new TeleportBlockSettings();
    
    public static class TeleportBlockSettings
    {
        @Comment("When cycling through Teleport Beacon destinations, include saved Teleport Rails")
        @LangKey(LANG_PREFIX + ".teleport_block.beacon_destinations_include_rails")
        public boolean beaconDestinationsIncludeRails = false;
        
        @Comment("When cycling through Teleport Beacon destinations, include same beacon as an option")
        @LangKey(LANG_PREFIX + ".teleport_block.beacon_destinations_include_self")
        public boolean beaconDestinationsIncludeSelf = false;

        @Comment("Cooldown time (in ticks) after a Teleport Rail teleports a minecart, during which it will not teleport another cart")
        @LangKey(LANG_PREFIX + ".teleport_block.rail_cooldown_ticks")
        @RangeInt(min = 0, max = 20)
        public int railCooldownTime = 5;
        
        @Comment("When cycling through Teleport Rail destinations, include saved Teleport Beacons")
        @LangKey(LANG_PREFIX + ".teleport_block.rail_destinations_include_beacons")
        public boolean railDestinationsIncludeBeacons = false;
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
