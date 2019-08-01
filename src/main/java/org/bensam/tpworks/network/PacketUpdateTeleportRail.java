package org.bensam.tpworks.network;

import org.bensam.tpworks.block.teleportrail.TileEntityTeleportRail;
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
 * PacketUpdateTeleportRail - sent from server to client to update the active status stored in the teleport rail on the client,
 * used whenever the status changes on the server
 */
public class PacketUpdateTeleportRail implements IMessage
{
    private BlockPos pos;
    private int isStored;
    private int teleportDirection;
    
    public PacketUpdateTeleportRail(BlockPos pos, boolean isStored)
    {
        this.pos = pos;
        this.isStored = isStored ? 1 : 0;
        this.teleportDirection = -1; // indicates no change is being made to direction
    }

    public PacketUpdateTeleportRail(BlockPos pos, TeleportDirection teleportDirection)
    {
        this.pos = pos;
        this.isStored = -1; // indicates no change is being made to storage
        this.teleportDirection = teleportDirection.getTeleportDirectionValue();
    }

    public PacketUpdateTeleportRail(BlockPos pos, boolean isStored, TeleportDirection teleportDirection)
    {
        this.pos = pos;
        this.isStored = isStored ? 1 : 0;
        this.teleportDirection = teleportDirection.getTeleportDirectionValue();
    }
    
    /*
     * Constructor for Forge to call via reflection, which will then call fromBytes() to initialize fields
     */
    public PacketUpdateTeleportRail()
    {}
    
    public static class Handler implements IMessageHandler<PacketUpdateTeleportRail, IMessage>
    {
        @Override
        public IMessage onMessage(PacketUpdateTeleportRail message, MessageContext ctx)
        {
            // Update the teleport beacon in the client, scheduling this execution on the main thread instead of the Netty networking thread.
            Minecraft.getMinecraft().addScheduledTask(() ->
            {
                WorldClient world = Minecraft.getMinecraft().world;
                TileEntity te = world.getTileEntity(message.pos);
                if (te instanceof TileEntityTeleportRail)
                {
                    TileEntityTeleportRail teTeleportRail = (TileEntityTeleportRail) te;
                    
                    if (message.isStored >= 0)
                    {
                        teTeleportRail.setStoredByPlayer(message.isStored != 0);
                    }
                    
                    if (message.teleportDirection >= 0)
                    {
                        teTeleportRail.setTeleportDirection(TeleportDirection.values()[message.teleportDirection]);
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
        isStored = buf.readInt();
        teleportDirection = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeLong(pos.toLong());
        buf.writeInt(isStored);
        buf.writeInt(teleportDirection);
    }
}
