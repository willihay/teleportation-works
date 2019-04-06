package org.bensam.tpworks;

import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.entity.ModEntities;
import org.bensam.tpworks.item.ModItems;
import org.bensam.tpworks.potion.ModPotions;
import org.bensam.tpworks.sound.ModSounds;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;

/**
 * @author WilliHay
 * 
 * Signals to Forge that we want to listen to the main event bus, which allows mods to register/subscribe
 * handler methods to run when certain events occur. This class is concerned with common (i.e. non-sided) registry events.
 */
@Mod.EventBusSubscriber(modid = TeleportationWorks.MODID)
public class CommonEventSubscriber
{

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event)
    {
        ModBlocks.register(event.getRegistry());
        TeleportationWorks.MOD_LOGGER.debug("ModBlocks registered");
    }
    
    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event)
    {
        ModBlocks.registerItemBlocks(event.getRegistry());
        TeleportationWorks.MOD_LOGGER.debug("ModBlocks ItemBlocks registered");

        ModItems.register(event.getRegistry());
        TeleportationWorks.MOD_LOGGER.debug("ModItems registered");
    }

    @SubscribeEvent
    public static void onRegisterEntities(RegistryEvent.Register<EntityEntry> event)
    {
        ModEntities.register(event.getRegistry());
        TeleportationWorks.MOD_LOGGER.debug("ModEntities registered");
    }
    
    @SubscribeEvent
    public static void onRegisterPotions(RegistryEvent.Register<Potion> event)
    {
        ModPotions.register(event.getRegistry());
        TeleportationWorks.MOD_LOGGER.debug("ModPotions registered");
    }
    
    @SubscribeEvent
    public static void onRegisterSounds(RegistryEvent.Register<SoundEvent> event)
    {
        ModSounds.register(event.getRegistry());
        TeleportationWorks.MOD_LOGGER.debug("ModSounds registered");
    }
}
