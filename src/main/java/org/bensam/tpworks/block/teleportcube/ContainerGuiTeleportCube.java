package org.bensam.tpworks.block.teleportcube;

import java.awt.Color;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.ModBlocks;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

/**
 * @author WilliHay
 *
 */
public class ContainerGuiTeleportCube extends GuiContainer
{
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    public static final int CUBE_INVENTORY_LABEL_XPOS = 8;
    public static final int CUBE_INVENTORY_LABEL_YPOS = 40;
    public static final int PLAYER_INVENTORY_LABEL_XPOS = 8;
    public static final int PLAYER_INVENTORY_LABEL_YPOS = 72;

    private static final ResourceLocation BACKGROUND = new ResourceLocation(TeleportationWorks.MODID, "textures/gui/teleport_cube.png");
    private String inventoryName;
    
    public ContainerGuiTeleportCube(String inventoryName, Container inventorySlots)
    {
        super(inventorySlots);
        
        xSize = WIDTH;
        ySize = HEIGHT;
        this.inventoryName = inventoryName;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        mc.getTextureManager().bindTexture(BACKGROUND);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        fontRenderer.drawString(ModBlocks.TELEPORT_CUBE.getLocalizedName(), CUBE_INVENTORY_LABEL_XPOS, CUBE_INVENTORY_LABEL_YPOS, Color.darkGray.getRGB());
        fontRenderer.drawString(inventoryName, PLAYER_INVENTORY_LABEL_XPOS, PLAYER_INVENTORY_LABEL_YPOS, Color.darkGray.getRGB());
    }
}
