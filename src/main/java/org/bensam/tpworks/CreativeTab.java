package org.bensam.tpworks;

import org.bensam.tpworks.item.ModItems;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WilliHay
 * 
 * Put all mod items in a new Creative Tab.
 */
public class CreativeTab extends CreativeTabs
{

    public CreativeTab()
    {
        super(TeleportationWorks.MODID);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack createIcon()
    {
        return new ItemStack(ModItems.TELEPORTATION_WAND);
    }
}
