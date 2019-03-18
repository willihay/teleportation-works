package org.bensam.tpworks.entity;

import org.bensam.tpworks.item.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraftforge.fml.client.registry.IRenderFactory;

/**
 * @author WilliHay
 *
 */
public class RenderTeleportationSplashPotion extends RenderSnowball<EntityTeleportationSplashPotion>
{
    public static final Factory RENDER_FACTORY = new Factory();

    public RenderTeleportationSplashPotion(RenderManager renderManager, RenderItem itemRenderer)
    {
        super(renderManager, ModItems.TELEPORTATION_SPLASH_POTION, itemRenderer);
    }

    public static class Factory implements IRenderFactory<EntityTeleportationSplashPotion>
    {
        @Override
        public Render<? super EntityTeleportationSplashPotion> createRenderFor(RenderManager manager)
        {
            return new RenderTeleportationSplashPotion(manager, Minecraft.getMinecraft().getRenderItem());
        }
    }
}
