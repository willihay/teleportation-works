package org.bensam.tpworks.network;

import org.bensam.tpworks.capability.teleportation.ITeleportationTileEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * @author WilliHay
 *
 * PacketUpdateTeleportIncoming - sent from server to client to notify a TileEntity that an entity is about to teleport to that position
 */
public class PacketUpdateTeleportIncoming implements IMessage
{
    private BlockPos pos;
    private int dimension;

    public PacketUpdateTeleportIncoming(BlockPos pos, int dimension)
    {
        this.pos = pos;
        this.dimension = dimension;
    }

    /*
     * Constructor for Forge to call via reflection, which will then call fromBytes() to initialize fields
     */
    public PacketUpdateTeleportIncoming()
    {}
    
    public static class Handler implements IMessageHandler<PacketUpdateTeleportIncoming, IMessage>
    {
        @Override
        public IMessage onMessage(PacketUpdateTeleportIncoming message, MessageContext ctx)
        {
            // Update the TE in the client, scheduling this execution on the main thread instead of the Netty networking thread.
            Minecraft.getMinecraft().addScheduledTask(() ->
            {
                WorldClient world = Minecraft.getMinecraft().world;
                if (message.dimension == world.provider.getDimension())
                {
                    TileEntity te = world.getTileEntity(message.pos);
                    if (te instanceof ITeleportationTileEntity)
                    {
                        ((ITeleportationTileEntity) te).setIncomingTeleportInProgress();
                    }
                }
                else
                {
                    // Ignore the message. It is meant for a tile entity in a different dimension than what this player currently has loaded.
                }
            });
            
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
