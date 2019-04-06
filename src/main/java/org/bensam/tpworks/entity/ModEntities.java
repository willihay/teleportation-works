package org.bensam.tpworks.entity;

import org.bensam.tpworks.TeleportationWorks;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * @author WilliHay
 *
 */
public class ModEntities
{
    private static int networkID = 1; // unique ID for entities from this mod
    
    public static void register(IForgeRegistry<EntityEntry> registry)
    {
        // Register the teleportation splash potion entity.
        EntityEntry entryTeleportationSplashPotion = EntityEntryBuilder.create()
                .id(new ResourceLocation(TeleportationWorks.MODID, "teleportation_splash_potion"), networkID++)
                .name(TeleportationWorks.MODID + ":teleportation_splash_potion")
                .entity(EntityTeleportationSplashPotion.class)
                .tracker(64, 3, true)
                .build();
        registry.register(entryTeleportationSplashPotion);

        // Register the teleportation arrow entity.
        EntityEntry entryTeleportationTippedArrow = EntityEntryBuilder.create()
                .id(new ResourceLocation(TeleportationWorks.MODID, "teleportation_tipped_arrow"), networkID++)
                .name(TeleportationWorks.MODID + ":teleportation_tipped_arrow")
                .entity(EntityTeleportationTippedArrow.class)
                .tracker(64, 3, true)
                .build();
        registry.register(entryTeleportationTippedArrow);
    }

    public static void registerRenderer()
    {
        RenderingRegistry.registerEntityRenderingHandler(EntityTeleportationSplashPotion.class, RenderTeleportationSplashPotion.RENDER_FACTORY);
        RenderingRegistry.registerEntityRenderingHandler(EntityTeleportationTippedArrow.class, RenderTeleportationTippedArrow.RENDER_FACTORY);
    }
}
