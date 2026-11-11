package com.luckymod;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Matériau des outils Lucky : base Netherite, en plus rapide. Réparation à l'émeraude. */
public enum LuckyTier implements Tier {
	INSTANCE;

	@Override
	public int getUses() {
		return 4000;
	}

	@Override
	public float getSpeed() {
		return 16.0f;
	}

	@Override
	public float getAttackDamageBonus() {
		return 6.0f;
	}

	@Override
	public TagKey<Block> getIncorrectBlocksForDrops() {
		return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
	}

	@Override
	public int getEnchantmentValue() {
		return 30;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.of(Items.EMERALD);
	}
}
