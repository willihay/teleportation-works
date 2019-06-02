package org.bensam.tpworks.recipe;

import org.bensam.tpworks.util.ModConfig;
import org.bensam.tpworks.util.ModConfig.WorldSettings.CraftingDifficulty;

import com.google.gson.JsonObject;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

/**
 * @author WilliHay
 *
 */
public class RecipeTeleportationItem extends ShapedRecipes
{

    public RecipeTeleportationItem(int width, int height, NonNullList<Ingredient> ingredients, ItemStack result)
    {
        super("", width, height, ingredients, result);
    }

    /**
     * Check if a recipe matches current crafting inventory.
     */
    @Override
    public boolean matches(InventoryCrafting inv, World world)
    {
        if (inv.getWidth() == 3 && inv.getHeight() == 3)
        {
            // Check for match between crafting inventory and recipe. 
            if (this.checkMatch(inv, false))
            {
                return true;
            }
            
            // Check for match between crafting inventory and mirrored recipe. 
            if (this.checkMatch(inv, true))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks stack by stack if the crafting inventory is a match for the (possibly mirrored) recipe.
     */
    private boolean checkMatch(InventoryCrafting craftingInventory, boolean isMirrored)
    {
        NonNullList<Ingredient> ingredients = getIngredients();
        int height = 3;
        int width = 3;
        
        for (int i = 0; i < width; ++i)
        {
            for (int j = 0; j < height; ++j)
            {
                // Get the crafting item in this position.
                ItemStack craftingItem = craftingInventory.getStackInRowAndColumn(i, j);
                
                // Is it an ender pearl AND the crafting difficulty is set to HARD?
                if (craftingItem != null 
                    && craftingItem.getItem() == Items.ENDER_PEARL 
                    && ModConfig.worldSettings.craftingDifficulty == CraftingDifficulty.HARD)
                {
                    return false; // ender pearls cannot be used when the crafting difficulty is set to HARD
                }

                int stride = j * width;
                Ingredient ingredient = Ingredient.EMPTY;

                if (isMirrored)
                {
                    ingredient = ingredients.get(width - 1 - i + stride);
                }
                else
                {
                    ingredient = ingredients.get(i + stride);
                }
                
                if (!ingredient.apply(craftingItem))
                {
                    return false;
                }
            }
        }

        return true;
    }
    
    public static RecipeTeleportationItem deserialize(JsonObject json)
    {
        ShapedRecipes recipe = ShapedRecipes.deserialize(json);
        return new RecipeTeleportationItem(recipe.getRecipeWidth(), recipe.getRecipeHeight(), recipe.getIngredients(), recipe.getRecipeOutput());
    }

    public static class Factory implements IRecipeFactory
    {
        @Override
        public IRecipe parse(JsonContext context, JsonObject json)
        {
            return RecipeTeleportationItem.deserialize(json);
        }
    }
}
