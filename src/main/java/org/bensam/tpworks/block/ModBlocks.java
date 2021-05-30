package org.bensam.tpworks.block;

import java.util.Arrays;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.teleportbeacon.BlockTeleportBeacon;
import org.bensam.tpworks.block.teleportbeacon.TileEntityTeleportBeacon;
import org.bensam.tpworks.block.teleportcube.BlockTeleportCube;
import org.bensam.tpworks.block.teleportcube.TileEntityTeleportCube;
import org.bensam.tpworks.block.teleportrail.BlockTeleportRail;
import org.bensam.tpworks.block.teleportrail.TileEntityTeleportRail;
import org.bensam.tpworks.util.ModSetup;

import com.google.common.base.Preconditions;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * @author WilliHay
 * 
 * Thanks to Cadiboo for the registration code examples!
 *
 */
@ObjectHolder(TeleportationWorks.MODID)
public class ModBlocks
{
    public static final BlockTeleportBeacon TELEPORT_BEACON = null;
    public static final BlockTeleportCube TELEPORT_CUBE = null;
    public static final BlockTeleportRail TELEPORT_RAIL = null;
    
    public static void register(IForgeRegistry<Block> registry)
    {
        registry.register(new BlockTeleportBeacon("teleport_beacon"));
        registry.register(new BlockTeleportCube("teleport_cube"));
        registry.register(new BlockTeleportRail("teleport_rail"));

        GameRegistry.registerTileEntity(TileEntityTeleportBeacon.class, new ResourceLocation(TeleportationWorks.MODID, "teleport_beacon"));
        GameRegistry.registerTileEntity(TileEntityTeleportCube.class, new ResourceLocation(TeleportationWorks.MODID, "teleport_cube"));
        GameRegistry.registerTileEntity(TileEntityTeleportRail.class, new ResourceLocation(TeleportationWorks.MODID, "teleport_rail"));
    }

    // @formatter:off
    public static void registerItemBlocks(IForgeRegistry<Item> registry)
    {
        Arrays.stream(new Block[]
                {
                    TELEPORT_BEACON,
                    TELEPORT_CUBE,
                    TELEPORT_RAIL
                }).forEach(block -> 
                {
                    registry.register(
                            ModSetup.setRegistryNames(
                                    new ItemBlock(block),
                                    block.getRegistryName()));
                });
    }

    public static void registerItemBlockModels()
    {
        Arrays.stream(new Block[]
                {
                    TELEPORT_BEACON,
                    TELEPORT_CUBE,
                    TELEPORT_RAIL
                }).forEach(block -> 
                {
                    Preconditions.checkNotNull(block, "Block cannot be null!");
                    ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, 
                            new ModelResourceLocation(block.getRegistryName(), "inventory"));
                });
    }
    // @formatter:on

    public static void registerOreDictionaryEntries()
    {}
}
