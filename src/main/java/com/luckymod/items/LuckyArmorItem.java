package com.luckymod.items;

import com.luckymod.LuckyText;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Pièce d'armure Lucky (modèle de la Netherite). Les pouvoirs sont gérés dans ArmorSet. */
public class LuckyArmorItem extends ArmorItem {
	public LuckyArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
		super(material, type, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		LuckyText.tooltip(tooltip, "lucky_aura", "jackpot_nova");
	}
}
