package org.bensam.tpworks.network;

import org.bensam.tpworks.capability.teleportation.ITeleportationHandler;
import org.bensam.tpworks.capability.teleportation.ITeleportationTileEntity;
import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportationHandlerCapabilityProvider;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * @author WilliHay
 * 
 * PacketRequestUpdateTeleportTileEntity - sent from client to server when client loads the TileEntity and needs to get its data
 * from the server
 */
public class PacketRequestUpdateTeleportTileEntity implements IMessage
{
    private BlockPos pos;
    private int dimension;
    
    public PacketRequestUpdateTeleportTileEntity(BlockPos pos, int dimension)
    {
        this.pos = pos;
        this.dimension = dimension;
    }
    
    public PacketRequestUpdateTeleportTileEntity(ITeleportationTileEntity te)
    {
        this(((TileEntity) te).getPos(), ((TileEntity) te).getWorld().provider.getDimension());
    }
    
    public PacketRequestUpdateTeleportTileEntity()
    {}

    public static class Handler implements IMessageHandler<PacketRequestUpdateTeleportTileEntity, PacketUpdateTeleportTileEntity>
    {
        @Override
        public PacketUpdateTeleportTileEntity onMessage(PacketRequestUpdateTeleportTileEntity message, MessageContext ctx)
        {
            EntityPlayer player = ctx.getServerHandler().player;
            World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(message.dimension);
            TileEntity te = world.getTileEntity(message.pos);
            
            if (te instanceof ITeleportationTileEntity)
            {
                ITeleportationHandler teleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);

                if (teleportationHandler != null)
                {
                    ITeleportationTileEntity teTeleportationTileEntity = (ITeleportationTileEntity) te;
                    TeleportDestination destination = teleportationHandler.getDestinationFromUUID(teTeleportationTileEntity.getUniqueID());
                    if (destination != null)
                    {
                        // Run validation on the destination found in the player's network.
                        teleportationHandler.validateDestination(player, destination);
                        
                        // Return a packet indicating the tile entity is stored. 
                        return new PacketUpdateTeleportTileEntity(message.pos, message.dimension, Boolean.TRUE, Boolean.valueOf(teTeleportationTileEntity.isSender()));
                    }
                    else
                    {
                        // Return a packet indicating the tile entity is not stored for this player. 
                        return new PacketUpdateTeleportTileEntity(message.pos, message.dimension, Boolean.FALSE, Boolean.valueOf(teTeleportationTileEntity.isSender()));
                    }
                }
            }

            return null;
        }
    }
    
    @Override
    public void fromBytes(ByteBuf buf)
    {
        pos = BlockPos.fromLong(buf.readLong());
        dimension = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeLong(pos.toLong());
        buf.writeInt(dimension);
    }
}
