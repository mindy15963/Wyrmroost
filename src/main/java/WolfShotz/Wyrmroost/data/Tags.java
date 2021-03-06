package WolfShotz.Wyrmroost.data;

import WolfShotz.Wyrmroost.registry.WRBlocks;
import WolfShotz.Wyrmroost.registry.WRItems;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.item.Items;

public class Tags
{
    public static class ItemTagsData extends ItemTagsProvider
    {
        public ItemTagsData(DataGenerator generatorIn)
        {
            super(generatorIn);
        }

        @Override
        protected void registerTags()
        {
            WRItems.Tags.ITEM_BLOCKS.forEach(this::copy);

            getBuilder(net.minecraftforge.common.Tags.Items.EGGS).add(WRItems.DRAGON_EGG.get());
            getBuilder(WRItems.Tags.GEODES).add(WRItems.BLUE_GEODE.get(), WRItems.RED_GEODE.get(), WRItems.PURPLE_GEODE.get());
            getBuilder(WRItems.Tags.MEATS).add(Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.CHICKEN, Items.COOKED_CHICKEN, Items.MUTTON, Items.COOKED_MUTTON, Items.RABBIT, Items.COOKED_RABBIT, WRItems.RAW_LOWTIER_MEAT.get(), WRItems.COOKED_LOWTIER_MEAT.get(), WRItems.RAW_COMMON_MEAT.get(), WRItems.COOKED_COMMON_MEAT.get(), WRItems.RAW_APEX_MEAT.get(), WRItems.COOKED_APEX_MEAT.get(), WRItems.RAW_BEHEMOTH_MEAT.get(), WRItems.COOKED_BEHEMOTH_MEAT.get());
            getBuilder(WRItems.Tags.PLATINUM).add(WRItems.PLATINUM_INGOT.get());
            getBuilder(WRItems.Tags.DRAGON_MEATS).add(WRItems.RAW_LOWTIER_MEAT.get(), WRItems.COOKED_LOWTIER_MEAT.get(), WRItems.RAW_COMMON_MEAT.get(), WRItems.COOKED_COMMON_MEAT.get(), WRItems.RAW_APEX_MEAT.get(), WRItems.COOKED_APEX_MEAT.get(), WRItems.RAW_BEHEMOTH_MEAT.get(), WRItems.COOKED_BEHEMOTH_MEAT.get());
        }
    }

    public static class BlockTagsData extends BlockTagsProvider
    {
        public BlockTagsData(DataGenerator generatorIn)
        {
            super(generatorIn);
        }

        @Override
        protected void registerTags()
        {
            getBuilder(WRBlocks.Tags.STORAGE_BLOCKS_GEODE).add(WRBlocks.BLUE_GEODE_BLOCK.get(), WRBlocks.RED_GEODE_BLOCK.get(), WRBlocks.PURPLE_GEODE_BLOCK.get());
//            getBuilder(net.minecraftforge.common.Tags.Blocks).add(WRBlocks.Tags.STORAGE_BLOCKS_GEODE).add(WRBlocks.PLATINUM_BLOCK.get());

//            getBuilder(WRBlocks.Tags.CANARI_LOGS).add(WRBlocks.CANARI_LOG.get(), WRBlocks.CANARI_WOOD.get(), WRBlocks.STRIPPED_CANARI.get());
//            getBuilder(WRBlocks.Tags.BLUE_CORIN_LOGS).add(WRBlocks.BLUE_CORIN_LOG.get(), WRBlocks.BLUE_CORIN_WOOD.get(), WRBlocks.STRIPPED_BLUE_CORIN_LOG.get());
//            getBuilder(WRBlocks.Tags.TEAL_CORIN_LOGS).add(WRBlocks.TEAL_CORIN_LOG.get(), WRBlocks.TEAL_CORIN_WOOD.get(), WRBlocks.STRIPPED_TEAL_CORIN_LOG.get());
//            getBuilder(WRBlocks.Tags.RED_CORIN_LOGS).add(WRBlocks.RED_CORIN_LOG.get(), WRBlocks.RED_CORIN_WOOD.get(), WRBlocks.STRIPPED_RED_CORIN_LOG.get());
//            getBuilder(BlockTags.LOGS).add(WRBlocks.Tags.CANARI_LOGS, WRBlocks.Tags.BLUE_CORIN_LOGS, WRBlocks.Tags.TEAL_CORIN_LOGS, WRBlocks.Tags.RED_CORIN_LOGS);
//            getBuilder(BlockTags.PLANKS).add(WRBlocks.CANARI_PLANKS.get(), WRBlocks.BLUE_CORIN_PLANKS.get(), WRBlocks.TEAL_CORIN_PLANKS.get(), WRBlocks.RED_CORIN_PLANKS.get());
        }
    }

//    public static class FluidTagsData extends FluidTagsProvider
//    {
//        public FluidTagsData(DataGenerator generatorIn)
//        {
//            super(generatorIn);
//        }
//
//        @Override
//        protected void registerTags()
//        {
//            getBuilder(FluidTags.WATER).add(WRFluids.CAUSTIC_WATER.getSource(), WRFluids.CAUSTIC_WATER.getFlow());
//        }
//    }
}
