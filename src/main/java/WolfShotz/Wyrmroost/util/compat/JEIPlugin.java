package WolfShotz.Wyrmroost.util.compat;

import WolfShotz.Wyrmroost.event.SetupItems;
import WolfShotz.Wyrmroost.util.utils.ModUtils;
import WolfShotz.Wyrmroost.util.utils.TranslationUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

@JeiPlugin
public class JEIPlugin implements IModPlugin
{
    @Override
    public ResourceLocation getPluginUid() { return ModUtils.location("info"); }
    
    @Override
    public void registerRecipes(IRecipeRegistration registry) {
        // Soul Crystal
        registry.addIngredientInfo(new ItemStack(SetupItems.soulCrystal, 1), VanillaTypes.ITEM,
                                   new TranslationTextComponent("item.wyrmroost.soul_crystal.jeidesc",
                                                                TranslationUtils.stringTranslation("dsabgi", TextFormatting.OBFUSCATED).getFormattedText())
                                           .getFormattedText());
        
        registry.addIngredientInfo(new ItemStack(SetupItems.dragonEgg, 1), VanillaTypes.ITEM, TranslationUtils.stringTranslation("item.wyrmroost.dragon_egg.jeidesc").getFormattedText());
    }
}
