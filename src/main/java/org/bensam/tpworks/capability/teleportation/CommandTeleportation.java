package org.bensam.tpworks.capability.teleportation;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.capability.teleportation.TeleportDestination.DestinationType;
import org.bensam.tpworks.network.PacketUpdateTeleportBeacon;
import org.bensam.tpworks.network.PacketUpdateTeleportRail;

import com.google.common.collect.Lists;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * @author WilliHay
 *
 */
public class CommandTeleportation extends CommandBase
{
    private enum TargetType
    {
        NONE,
        INVALID,
        BEACONS,
        RAILS
    }
    
    /**
     * Gets the name of the command.
     */
    @Override
    public String getName()
    {
        return "td";
    }

    /**
     * Gets the usage string for the command.
     */
    @Override
    public String getUsage(ICommandSender sender)
    {
        return "command.td.usage";
    }

    /**
     * Callback for when the command is executed.
     */
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length <= 0)
            throw new WrongUsageException("command.td.usage", new Object[0]);

        String cmd = args[0];
        int index = -1;
        boolean affectAll = false;
        TargetType targetType = TargetType.NONE;

        // Get the second parameter indicating target destination, if any.
        if (args.length >= 2)
        {
            if (args[1].equals("*") || args[1].equalsIgnoreCase("all"))
            {
                affectAll = true;
            }
            else if (args[1].equalsIgnoreCase("invalid"))
            {
                targetType = TargetType.INVALID;
            }
            else if (args[1].equalsIgnoreCase("beacons"))
            {
                targetType = TargetType.BEACONS;
            }
            else if (args[1].equalsIgnoreCase("rails"))
            {
                targetType = TargetType.RAILS;
            }
            else
            {
                index = parseInt(args[1], 1);
            }
        }

        if (StringUtils.isNumeric(cmd))
        {
            index = parseInt(args[0], 1);
            executeSetDestinationCommand(sender, --index); // use zero-based index
        }
        else if (cmd.equalsIgnoreCase("list"))
        {
            index--; // use zero-based index
            executeListCommand(sender, index, targetType);
        }
        else if (cmd.equalsIgnoreCase("delete"))
        {
            if (args.length < 2)
                throw new CommandException("command.td.destination.missing");

            if (affectAll)
            {
                executeDeleteAllCommand(sender, false);
            }
            else
            {
                index--; // use zero-based index
                executeDeleteCommand(sender, index, targetType);
            }
        }
        else if (cmd.equalsIgnoreCase("prev"))
        {
            executeNextCommand(sender, index == -1 ? index : -index);
        }
        else if (cmd.equalsIgnoreCase("next"))
        {
            executeNextCommand(sender, index == -1 ? 1 : index);
        }
        else
            throw new WrongUsageException("command.td.usage", new Object[0]);
    }

    public void executeSetDestinationCommand(ICommandSender sender, int destinationIndex) throws CommandException
    {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ITeleportationHandler teleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY,
                null);

        if (teleportationHandler != null)
        {
            int destinationCount = teleportationHandler.getDestinationCount();
            if (destinationCount == 0)
            {
                player.sendMessage(new TextComponentTranslation("command.td.destination.none"));
                return;
            }

            if (destinationIndex >= destinationCount)
                throw new CommandException("command.td.destination.notFound", new Object[] { (destinationIndex + 1) });

            TeleportDestination destination = teleportationHandler.setActiveDestination(destinationIndex);

            player.sendMessage(new TextComponentTranslation("command.td.active.confirmation",
                    new Object[] { teleportationHandler.getShortFormattedName(player, destination) }));
        }
    }

    public void executeListCommand(ICommandSender sender, int destinationIndex, TargetType targetType) throws CommandException
    {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ITeleportationHandler teleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY,
                null);

        if (teleportationHandler != null)
        {
            int destinationCount = teleportationHandler.getDestinationCount();
            if (destinationCount == 0)
            {
                player.sendMessage(new TextComponentTranslation("command.td.destination.none"));
                return;
            }

            if (targetType != TargetType.NONE)
            {
                // List all the teleport destinations of type targetType in this player's network.
                TeleportDestination activeDestination = teleportationHandler.getActiveDestination();
                DestinationType destinationType = null;
                
                if (targetType == TargetType.BEACONS)
                    destinationType = DestinationType.BEACON;
                else if (targetType == TargetType.RAILS)
                    destinationType = DestinationType.RAIL;
                
                TeleportDestination destination = null;
                boolean foundOne = false;
                do
                {
                     destination = TeleportationHelper.getNextDestination(player, destinationType, destination, null);
                     if (destination != null)
                     {
                         if (targetType != TargetType.INVALID || !teleportationHandler.validateDestination(player, destination))
                         {
                             foundOne = true;
                             TextFormatting destinationFormat = (destination.equals(activeDestination)) ? TextFormatting.GOLD : TextFormatting.RESET;
                             player.sendMessage(new TextComponentString(destinationFormat + teleportationHandler.getLongFormattedName(player, destination, destinationFormat)));
                         }
                     }
                } while (destination != null);
                
                if (!foundOne)
                {
                    player.sendMessage(new TextComponentTranslation("command.td.destination.none_of_type", new Object[] { targetType }));
                }
            }
            else if (destinationIndex < 0)
            {
                // List all the teleport destinations in this player's network.
                int activeDestinationIndex = teleportationHandler.getActiveDestinationIndex();
                for (int i = 0; i < destinationCount; i++)
                {
                    TeleportDestination destination = teleportationHandler.getDestinationFromIndex(i);
                    TextFormatting destinationFormat = (i == activeDestinationIndex) ? TextFormatting.GOLD : TextFormatting.RESET;
                    player.sendMessage(new TextComponentString(destinationFormat + Integer.toString(i + 1) + ") "
                            + teleportationHandler.getLongFormattedName(player, destination, destinationFormat)));
                }
            }
            else
            {
                // Display the name of the selected teleport destination.
                TeleportDestination destination = teleportationHandler.getDestinationFromIndex(destinationIndex);
                if (destination == null)
                    throw new CommandException("command.td.destination.notFound", new Object[] { (destinationIndex + 1) });

                player.sendMessage(new TextComponentString(teleportationHandler.getLongFormattedName(player, destination)));
            }
        }
    }

    public void executeDeleteCommand(ICommandSender sender, int destinationIndex, TargetType targetType) throws CommandException
    {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ITeleportationHandler teleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY,
                null);

        if (teleportationHandler != null)
        {
            int destinationCount = teleportationHandler.getDestinationCount();
            if (destinationCount == 0)
            {
                player.sendMessage(new TextComponentTranslation("command.td.destination.none"));
                return;
            }

            if (targetType != TargetType.NONE)
            {
                // Delete all the teleport destinations of type targetType in this player's network.
                boolean foundOne = false;

                DestinationType destinationType = null;
                if (targetType == TargetType.BEACONS)
                    destinationType = DestinationType.BEACON;
                else if (targetType == TargetType.RAILS)
                    destinationType = DestinationType.RAIL;

                TeleportDestination prevDestination = null;
                TeleportDestination destination = TeleportationHelper.getNextDestination(player, destinationType, null, null);
                while (destination != null)
                {
                    if (targetType != TargetType.INVALID || !teleportationHandler.validateDestination(player, destination))
                    {
                        // Remove the destination.
                        removeDestination(player, teleportationHandler, destination);
                        foundOne = true;
                    }
                    else
                    {
                        prevDestination = destination;
                    }
                    
                    destination = TeleportationHelper.getNextDestination(player, destinationType, prevDestination, null);
                }
                
                if (!foundOne)
                {
                    player.sendMessage(new TextComponentTranslation("command.td.destination.none_of_type", new Object[] { targetType }));
                }
            }
            else
            {
                // Get the TeleportDestination.
                TeleportDestination destination = teleportationHandler.getDestinationFromIndex(destinationIndex);
                if (destination == null)
                    throw new CommandException("command.td.destination.notFound", new Object[] { (destinationIndex + 1) });

                // Check destination - we don't want to remove an Overworld spawn bed from the network because this is supposed to be a fixed destination.
                if (destination.destinationType == DestinationType.SPAWNBED && destination.dimension == 0)
                    throw new CommandException("command.td.delete.spawnBed.invalid");
                
                // Remove the destination.
                removeDestination(player, teleportationHandler, destination);
            }

        }
    }

    private void removeDestination(EntityPlayerMP player, ITeleportationHandler teleportationHandler, TeleportDestination destination)
    {
        // Notify the player what's getting removed.
        player.sendMessage(new TextComponentTranslation("command.td.delete.confirmation",
                new Object[] { TextFormatting.DARK_GREEN + destination.friendlyName + TextFormatting.RESET }));

        // Only need to send a packet update to the client if we can still find the destination in the world.
        if (teleportationHandler.validateDestination(player, destination))
        {
            if (destination.destinationType == DestinationType.BEACON)
            {
                TeleportationWorks.network.sendTo(new PacketUpdateTeleportBeacon(destination.position, destination.dimension, Boolean.FALSE, null), player);
            }
            else if (destination.destinationType == DestinationType.RAIL)
            {
                TeleportationWorks.network.sendTo(new PacketUpdateTeleportRail(destination.position, destination.dimension, Boolean.FALSE, null), player);
            }
        }

        // Finally, remove the destination from the player's network.
        teleportationHandler.removeDestination(destination.getUUID());
    }
    
    public void executeDeleteAllCommand(ICommandSender sender, boolean forceDeleteSpawnBed) throws CommandException
    {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ITeleportationHandler teleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY,
                null);

        if (teleportationHandler != null)
        {
            int destinationCount = teleportationHandler.getDestinationCount();
            if (destinationCount == 0)
            {
                player.sendMessage(new TextComponentTranslation("command.td.destination.none"));
                return;
            }

            // Remove the destinations, sending packet updates to the client as needed.
            teleportationHandler.removeAllDestinations(player, forceDeleteSpawnBed);

            // Notify the player.
            player.sendMessage(new TextComponentTranslation("command.td.deleteAll.confirmation"));
        }
    }

    private void executeNextCommand(ICommandSender sender, int count) throws CommandException
    {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ITeleportationHandler teleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY,
                null);

        if (teleportationHandler != null)
        {
            int destinationCount = teleportationHandler.getDestinationCount();
            if (destinationCount == 0)
            {
                player.sendMessage(new TextComponentTranslation("command.td.destination.none"));
                return;
            }

            TeleportDestination destination = teleportationHandler.getActiveDestination();
            if (count < 0)
            {
                count = Math.abs(count);
                for (int i = 1; i <= count; ++i)
                {
                    destination = teleportationHandler.setActiveDestinationToPrevious();
                }
            }
            else
            {
                for (int i = 1; i <= count; ++i)
                {
                    destination = teleportationHandler.setActiveDestinationToNext();
                }
            }

            player.sendMessage(new TextComponentTranslation("command.td.active.confirmation",
                    new Object[] { teleportationHandler.getShortFormattedName(player, destination) }));
        }
    }

    /**
     * Check if the given ICommandSender has permission to execute this command.
     */
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return true;
    }

    /**
     * Get a list of options for when the user presses the TAB key
     */
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos)
    {
        if (args.length == 1)
            return getListOfStringsMatchingLastWord(args, new String[] { "delete", "list", "next", "prev", "<num>" });
        else if (args.length == 2 && !StringUtils.isNumeric(args[0]))
        {
            if (args[0].equalsIgnoreCase("delete"))
            {
                if (args[1].isEmpty())
                    return Lists.newArrayList("2", "all", "beacons", "rails", "invalid");
                else
                    return getListOfStringsMatchingLastWord(args, new String[] { "all", "beacons", "rails", "invalid" });
            }
            else
            {
                if (args[1].isEmpty())
                    return Lists.newArrayList("2", "beacons", "rails", "invalid");
                else
                    return getListOfStringsMatchingLastWord(args, new String[] { "beacons", "rails", "invalid" });
            }
        }

        return Collections.<String>emptyList();
    }

}
