/**
 * CreativeTab - provides a new tab in Creative mode
 */
package org.bensam.tpworks;

import org.bensam.tpworks.item.ModItems;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WilliHay
 *
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
