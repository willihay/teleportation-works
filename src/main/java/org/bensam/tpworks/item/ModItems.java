package org.bensam.tpworks.item;

import java.util.Arrays;

import org.bensam.tpworks.TeleportationWorks;

import com.google.common.base.Preconditions;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * @author WilliHay
 * 
 * Thanks to Cadiboo for the registration code examples!
 *
 */
@ObjectHolder(TeleportationWorks.MODID)
public class ModItems
{
    public static final ItemGeneric ENDER_EYE_SHARD = null;
    public static final ItemGeneric ENDER_EYE_TRANSLUCENT = null;
    public static final ItemTeleportationWand TELEPORTATION_WAND = null;
    public static final ItemTeleportationSplashPotion TELEPORTATION_SPLASH_POTION = null;
    public static final ItemTeleportationTippedArrow TELEPORTATION_TIPPED_ARROW = null;
    
    // @formatter:off
    public static void register(IForgeRegistry<Item> registry)
    {
        registry.register(new ItemGeneric("ender_eye_shard", true));
        registry.register(new ItemGeneric("ender_eye_translucent", false));
        registry.register(new ItemTeleportationWand("teleportation_wand"));
        registry.register(new ItemTeleportationSplashPotion("teleportation_splash_potion"));
        registry.register(new ItemTeleportationTippedArrow("teleportation_tipped_arrow"));
    }

    public static void registerItemModels()
    {
        Arrays.stream(new Item[]
                {
                    ENDER_EYE_SHARD,
                    ENDER_EYE_TRANSLUCENT,
                    TELEPORTATION_WAND,
                    TELEPORTATION_SPLASH_POTION,
                    TELEPORTATION_TIPPED_ARROW
                }).forEach(item -> 
                {
                    Preconditions.checkNotNull(item, "Item cannot be null!");
                    ModelLoader.setCustomModelResourceLocation(item, 0, 
                            new ModelResourceLocation(item.getRegistryName(), "inventory"));
                });
    }
    // @formatter:on
    
    public static void registerOreDictionaryEntries()
    {
    }
}
