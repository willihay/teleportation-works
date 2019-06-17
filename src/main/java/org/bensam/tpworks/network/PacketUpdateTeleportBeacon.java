package org.bensam.tpworks.network;

import org.bensam.tpworks.block.teleportbeacon.TileEntityTeleportBeacon;
import org.bensam.tpworks.capability.teleportation.ITeleportationBlock.TeleportDirection;

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
 * PacketUpdateTeleportBeacon - sent from server to client to update the active status stored in the teleport beacon on the client,
 * used whenever the status changes on the server
 */
public class PacketUpdateTeleportBeacon implements IMessage
{
    private BlockPos pos;
    private boolean isStored;
    private int teleportDirection;

    public PacketUpdateTeleportBeacon(BlockPos pos, boolean isStored)
    {
        this.pos = pos;
        this.isStored = isStored;
        this.teleportDirection = -1; // indicates no change is being made to direction
    }

    public PacketUpdateTeleportBeacon(BlockPos pos, boolean isStored, TeleportDirection teleportDirection)
    {
        this.pos = pos;
        this.isStored = isStored;
        this.teleportDirection = teleportDirection.getTeleportDirectionValue();
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
                    teTeleportBeacon.isStored = message.isStored;
                    if (message.teleportDirection >= 0)
                    {
                        teTeleportBeacon.setTeleportDirection(TeleportDirection.values()[message.teleportDirection]);
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
        isStored = buf.readBoolean();
        teleportDirection = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeLong(pos.toLong());
        buf.writeBoolean(isStored);
        buf.writeInt(teleportDirection);
    }

}
