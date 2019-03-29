package org.bensam.tpworks.util;

import javax.annotation.Nonnull;

import org.bensam.tpworks.TeleportationWorks;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * @author Cadiboo
 * 
 * Thanks to Cadiboo for this registration code!
 *
 */
public final class ModSetup
{

    public static <T extends IForgeRegistryEntry.Impl<?>> T setRegistryNames(@Nonnull final T entry, @Nonnull final String name)
    {
        return setRegistryNames(entry, new ResourceLocation(TeleportationWorks.MODID, name));
    }
    
    public static <T extends IForgeRegistryEntry.Impl<?>> T setRegistryNames(@Nonnull final T entry, @Nonnull final ResourceLocation registryName)
    {
        return setRegistryNames(entry, registryName, registryName.getPath());
    }
    
    public static <T extends IForgeRegistryEntry.Impl<?>> T setRegistryNames(@Nonnull final T entry, @Nonnull final ResourceLocation registryName, @Nonnull final String translationKey)
    {
        entry.setRegistryName(registryName);
        
        if (entry instanceof Block) {
            ((Block) entry).setTranslationKey(translationKey);
        }
        if (entry instanceof Item) {
            ((Item) entry).setTranslationKey(translationKey);
        }
        
        return entry;
    }
    
    public static <T extends IForgeRegistryEntry.Impl<?>> T setCreativeTab(@Nonnull final T entry)
    {
        return setCreativeTab(entry, TeleportationWorks.CREATIVE_TAB);
    }
    
    public static <T extends IForgeRegistryEntry.Impl<?>> T setCreativeTab(@Nonnull final T entry, final CreativeTabs creativeTab)
    {
        if (entry instanceof Block) {
            ((Block) entry).setCreativeTab(creativeTab);
        }
        if (entry instanceof Item) {
            ((Item) entry).setCreativeTab(creativeTab);
        }
        
        return entry;
    }
}
