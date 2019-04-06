package org.bensam.tpworks.potion;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.item.ModItems;

import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * @author WilliHay
 *
 */
@ObjectHolder(TeleportationWorks.MODID)
public class ModPotions
{
    public static final PotionTeleportation TELEPORTATION_POTION = null;

    public static void register(IForgeRegistry<Potion> registry)
    {
        registry.register(new PotionTeleportation().setup("teleportation_potion"));
        
        registerRecipes();
    }
    
    private static void registerRecipes()
    {
        // Brew an awkward potion with an ender eye shard to get a splash potion of teleportation.
        ItemStack inputPotion = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.AWKWARD);
        ItemStack ingredient = new ItemStack(ModItems.ENDER_EYE_SHARD);
        ItemStack outputPotion = PotionUtils.addPotionToItemStack(new ItemStack(ModItems.TELEPORTATION_SPLASH_POTION), PotionTypes.EMPTY);
        BrewingRecipeRegistry.addRecipe(new CustomBrewingRecipe(inputPotion, ingredient, outputPotion));
    }
}

class CustomBrewingRecipe extends BrewingRecipe
{
    public CustomBrewingRecipe(ItemStack input, ItemStack ingredient, ItemStack output)
    {
        super(input, ingredient, output);
    }

    @Override
    public boolean isInput(ItemStack stack)
    {
        // We only want to match input potions that have the PotionType specified in the brewing recipe. For potions, that means comparing NBT data.
        return ItemStack.areItemStacksEqualUsingNBTShareTag(stack, getInput());
    }
}
