package org.bensam.tpworks.block.teleportcube;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * @author WilliHay
 *
 */
public class ContainerTeleportCube extends Container
{
    public static final int CUBE_SLOT_XPOS = 8;
    public static final int CUBE_SLOT_YPOS = 52;
    public static final int INVENTORY_SLOT_XPOS = 8;
    public static final int INVENTORY_SLOT_YPOS = 84;
    public static final int HOTBAR_SLOT_XPOS = 8;
    public static final int HOTBAR_SLOT_YPOS = 142;
    public static final int SLOT_XY_SPACING = 18;
    
    private TileEntityTeleportCube te;
    
    public ContainerTeleportCube(IInventory playerInventory, TileEntityTeleportCube te)
    {
        this.te = te;

        addCubeSlots();
        addPlayerSlots(playerInventory);
    }

    private void addCubeSlots() 
    {
        IItemHandler itemHandler = this.te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        int x = CUBE_SLOT_XPOS;
        int y = CUBE_SLOT_YPOS;

        for (int slotIndex = 0; slotIndex < itemHandler.getSlots(); ++slotIndex) 
        {
            addSlotToContainer(new SlotItemHandler(itemHandler, slotIndex, x, y));
            x += SLOT_XY_SPACING;
        }
    }

    private void addPlayerSlots(IInventory playerInventory) 
    {
        int slotIndex = 0;

        // Add slots for the player hotbar.
        int x = HOTBAR_SLOT_XPOS;
        int y = HOTBAR_SLOT_YPOS;
        for (int col = 0; col < 9; ++col) 
        {
            addSlotToContainer(new Slot(playerInventory, slotIndex, x, y));
            slotIndex++;
            x += SLOT_XY_SPACING;
        }

        // Add slots for the main player inventory.
        x = INVENTORY_SLOT_XPOS;
        y = INVENTORY_SLOT_YPOS;
        for (int row = 0; row < 3; ++row) 
        {
            for (int col = 0; col < 9; ++col) 
            {
                addSlotToContainer(new Slot(playerInventory, slotIndex, x, y));
                slotIndex++;
                x += SLOT_XY_SPACING;
            }
            x = INVENTORY_SLOT_XPOS;
            y += SLOT_XY_SPACING;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) 
    {
        return te.canInteractWith(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) 
    {
        ItemStack sourceCopy = ItemStack.EMPTY;
        Slot sourceSlot = this.inventorySlots.get(index);

        if (sourceSlot != null && sourceSlot.getHasStack()) 
        {
            ItemStack sourceStack = sourceSlot.getStack();
            sourceCopy = sourceStack.copy();

            if (index < TileEntityTeleportCube.INVENTORY_SIZE) 
            {
                // Merge stack from TE inventory to player inventory, using vanilla container logic... 
                // Try merging to hotbar first.
                if (!this.mergeItemStack(sourceStack, TileEntityTeleportCube.INVENTORY_SIZE, TileEntityTeleportCube.INVENTORY_SIZE + 9, true)) 
                {
                    // If hotbar was full, try merging to player pack inventory.
                    if (!this.mergeItemStack(sourceStack, TileEntityTeleportCube.INVENTORY_SIZE + 9, this.inventorySlots.size(), true))
                    {
                        return ItemStack.EMPTY;
                    }
                }
            } 
            else
            {
                // Merge stack from player inventory to TE inventory.
                if (!this.mergeItemStack(sourceStack, 0, TileEntityTeleportCube.INVENTORY_SIZE, false)) 
                {
                    return ItemStack.EMPTY;
                }
            }

            if (sourceStack.isEmpty()) 
            {
                sourceSlot.putStack(ItemStack.EMPTY);
            } 

            sourceSlot.onTake(player, sourceStack); // also calls onSlotChanged()
        }
        
        return sourceCopy;
    }
}
