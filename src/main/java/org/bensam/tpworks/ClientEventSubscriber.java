package org.bensam.tpworks;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static net.minecraftforge.fml.relauncher.Side.CLIENT;

import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.item.ModItems;
import org.bensam.tpworks.potion.ModPotions;

import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;

/**
 * @author WilliHay
 * 
 * Signals to Forge that we want to listen to the main event bus, which allows mods to register/subscribe
 * handler methods to run when certain events occur. This class is concerned with client-side registry events.
 */
@Mod.EventBusSubscriber(modid = TeleportationWorks.MODID, value = CLIENT)
public class ClientEventSubscriber
{

    @SubscribeEvent
    public static void onRegisterModels(ModelRegistryEvent event)
    {
        ModBlocks.registerItemBlockModels();
        TeleportationWorks.MOD_LOGGER.debug("ModBlocks ItemBlock models registered");
        
        ModItems.registerItemModels();
        TeleportationWorks.MOD_LOGGER.debug("ModItems Item models registered");
    }

    @SubscribeEvent
    public static void onRegisterItemColorHandlers(ColorHandlerEvent.Item event)
    {
        // Register the teleportation potion color for the tinting on the potion and tipped arrow item model overlay.
        event.getItemColors().registerItemColorHandler(new IItemColor()
        {
            public int colorMultiplier(ItemStack stack, int tintIndex)
            {
                return tintIndex > 0 ? -1 : ModPotions.TELEPORTATION_POTION.getLiquidColor();
            }
        }, ModItems.TELEPORTATION_SPLASH_POTION, ModItems.TELEPORTATION_SPLASH_POTION_EXTENDED, ModItems.TELEPORTATION_TIPPED_ARROW);
        
        TeleportationWorks.MOD_LOGGER.debug("Teleportation Potion color registered");
    }
}
