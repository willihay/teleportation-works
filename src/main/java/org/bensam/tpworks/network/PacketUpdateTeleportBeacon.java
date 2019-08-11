package org.bensam.tpworks.network;

import javax.annotation.Nullable;

import org.bensam.tpworks.block.teleportbeacon.TileEntityTeleportBeacon;

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
 * PacketUpdateTeleportBeacon - sent from server to client to update the stored and sender status
 * of the teleport beacon on the client; used whenever the status changes on the server
 */
public class PacketUpdateTeleportBeacon implements IMessage
{
    private BlockPos pos;
    private int isStored; // -1: no change; 0: false; 1: true
    private int isSender; // -1: no change; 0: false; 1: true

    public PacketUpdateTeleportBeacon(BlockPos pos, @Nullable Boolean isStored, @Nullable Boolean isSender)
    {
        this.pos = pos;
        this.isStored = isStored == null ? -1 : (isStored.booleanValue() ? 1 : 0);
        this.isSender = isSender == null ? -1 : (isSender.booleanValue() ? 1 : 0);
    }
    
    /*
     * Constructor for Forge to call via reflection, which will then call fromBytes() to initialize fields
     */
    public PacketUpdateTeleportBeacon()
    {}
    
    public static class Handler implements IMessageHandler<PacketUpdateTeleportBeacon, IMessage>
    {
        @Override
        public IMessage onMessage(PacketUpdateTeleportBeacon message, MessageContext ctx)
        {
            // Update the teleport beacon in the client, scheduling this execution on the main thread instead of the Netty networking thread.
            Minecraft.getMinecraft().addScheduledTask(() ->
            {
                WorldClient world = Minecraft.getMinecraft().world;
                TileEntity te = world.getTileEntity(message.pos);
                if (te instanceof TileEntityTeleportBeacon)
                {
                    TileEntityTeleportBeacon teTeleportBeacon = (TileEntityTeleportBeacon) te;
                    
                    if (message.isStored >= 0)
                    {
                        teTeleportBeacon.setStoredByPlayer(message.isStored != 0);
                    }
                    
                    if (message.isSender >= 0)
                    {
                        teTeleportBeacon.setSender(message.isSender != 0);
                        world.markBlockRangeForRenderUpdate(message.pos, message.pos);
                    }
                }
            });
            
            return null;
        }
    }
    
    @Override
    public void fromBytes(ByteBuf buf)
    {
        pos = BlockPos.fromLong(buf.readLong());
        isStored = (int) (buf.readByte());
        isSender = (int) (buf.readByte());
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeLong(pos.toLong());
        buf.writeByte(isStored);
        buf.writeByte(isSender);
    }
}
