package org.bensam.tpworks.network;

import org.bensam.tpworks.block.teleportrail.TileEntityTeleportRail;
import org.bensam.tpworks.capability.teleportation.ITeleportationHandler;
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
 * PacketRequestUpdateTeleportRail - sent from client to server when client loads the TileEntity and needs to get its data
 * from the server
 */
public class PacketRequestUpdateTeleportRail implements IMessage
{
    private BlockPos pos;
    private int dimension;
    
    public PacketRequestUpdateTeleportRail(BlockPos pos, int dimension)
    {
        this.pos = pos;
        this.dimension = dimension;
    }
    
    public PacketRequestUpdateTeleportRail(TileEntityTeleportRail te)
    {
        this(te.getPos(), te.getWorld().provider.getDimension());
    }
    
    public PacketRequestUpdateTeleportRail()
    {}

    public static class Handler implements IMessageHandler<PacketRequestUpdateTeleportRail, PacketUpdateTeleportRail>
    {
        @Override
        public PacketUpdateTeleportRail onMessage(PacketRequestUpdateTeleportRail message, MessageContext ctx)
        {
            EntityPlayer player = ctx.getServerHandler().player;
            World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(message.dimension);
            TileEntity te = world.getTileEntity(message.pos);
            
            if (te instanceof TileEntityTeleportRail)
            {
                ITeleportationHandler teleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);

                if (teleportationHandler != null)
                {
                    TileEntityTeleportRail teTeleportRail = (TileEntityTeleportRail) te;
                    TeleportDestination destination = teleportationHandler.getDestinationFromUUID(teTeleportRail.getUniqueID());
                    if (destination != null)
                    {
                        // Run validation on the destination found in the player's network.
                        teleportationHandler.validateDestination(player, destination);
                        
                        // Return a packet indicating the rail is stored. 
                        return new PacketUpdateTeleportRail(message.pos, message.dimension, Boolean.TRUE, Boolean.valueOf(teTeleportRail.isSender()));
                    }
                    else
                    {
                        // Return a packet indicating the rail is not stored for this player. 
                        return new PacketUpdateTeleportRail(message.pos, message.dimension, Boolean.FALSE, Boolean.valueOf(teTeleportRail.isSender()));
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
