package org.bensam.tpworks.block.teleportrail;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.client.ForgeHooksClient;

/**
 * @author WilliHay
 *
 */
public class RendererTeleportRail extends TileEntitySpecialRenderer<TileEntityTeleportRail>
{

    @Override
    public void render(TileEntityTeleportRail te, double x, double y, double z, float partialTicks, int destroyStage,
                       float alpha)
    {
        if (!te.isStoredByPlayer())
            return;
        
        // Render rotating topper item when active.
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableBlend();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.pushMatrix();

        // Item appears on top of beacon.
        GlStateManager.translate(x + 0.5D, y + 1.0D, z + 0.5D);

        // Item should smoothly rotate around the Y-axis.
        GlStateManager.rotate((te.getWorld().getTotalWorldTime() + partialTicks) * 4, 0, 1, 0);

        IBakedModel model = Minecraft.getMinecraft().getRenderItem()
                .getItemModelWithOverrides(TileEntityTeleportRail.TOPPER_ITEM_WHEN_STORED, te.getWorld(), null);
        model = ForgeHooksClient.handleCameraTransforms(model, ItemCameraTransforms.TransformType.GROUND, false);

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        Minecraft.getMinecraft().getRenderItem().renderItem(TileEntityTeleportRail.TOPPER_ITEM_WHEN_STORED, model);

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
    }
}
