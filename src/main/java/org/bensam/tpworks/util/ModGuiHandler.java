package org.bensam.tpworks.util;

import org.bensam.tpworks.block.teleportcube.ContainerGuiTeleportCube;
import org.bensam.tpworks.block.teleportcube.ContainerTeleportCube;
import org.bensam.tpworks.block.teleportcube.TileEntityTeleportCube;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * @author WilliHay
 *
 */
public final class ModGuiHandler implements IGuiHandler
{
    public static final int TELEPORT_CUBE = 1; // teleport cube's GUI ID

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        
        switch (ID)
        {
        case TELEPORT_CUBE:
            if (te instanceof TileEntityTeleportCube)
            {
                return new ContainerTeleportCube(player.inventory, (TileEntityTeleportCube) te);
            }
        default:
            return null;
        }
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        
        switch (ID)
        {
        case TELEPORT_CUBE:
            if (te instanceof TileEntityTeleportCube)
            {
                return new ContainerGuiTeleportCube(player.inventory, (TileEntityTeleportCube) te);
            }
        default:
            return null;
        }
    }
}
