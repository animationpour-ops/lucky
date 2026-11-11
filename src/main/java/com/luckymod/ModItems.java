package com.luckymod;

import com.luckymod.items.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public final class ModItems {
	private ModItems() {}

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LuckyMod.MOD_ID);

	private static Item.Properties lucky() {
		return new Item.Properties().fireResistant().rarity(Rarity.EPIC);
	}

	public static final RegistryObject<Item> LUCKY_BRUSH = ITEMS.register("lucky_brush",
		() -> new LuckyBrushItem(lucky().durability(2000)));

	public static final RegistryObject<Item> LUCKY_SWORD = ITEMS.register("lucky_sword",
		() -> new LuckySword(LuckyTier.INSTANCE,
			lucky().attributes(SwordItem.createAttributes(LuckyTier.INSTANCE, 3, -2.4f))));

	public static final RegistryObject<Item> LUCKY_BOW = ITEMS.register("lucky_bow",
		() -> new LuckyBow(lucky().durability(3000)));

	public static final RegistryObject<Item> LUCKY_LIGHTER = ITEMS.register("lucky_lighter",
		() -> new LuckyLighter(lucky().stacksTo(1)));

	public static final RegistryObject<Item> LUCKY_PICKAXE = ITEMS.register("lucky_pickaxe",
		() -> new LuckyPickaxe(LuckyTier.INSTANCE,
			lucky().attributes(PickaxeItem.createAttributes(LuckyTier.INSTANCE, 1.0f, -2.8f))));

	public static final RegistryObject<Item> LUCKY_SHOVEL = ITEMS.register("lucky_shovel",
		() -> new LuckyShovel(LuckyTier.INSTANCE,
			lucky().attributes(ShovelItem.createAttributes(LuckyTier.INSTANCE, 1.5f, -3.0f))));

	public static final RegistryObject<Item> LUCKY_HELMET = ITEMS.register("lucky_helmet",
		() -> new LuckyArmorItem(ModArmorMaterials.LUCKY.getHolder().orElseThrow(), ArmorItem.Type.HELMET,
			lucky().durability(ArmorItem.Type.HELMET.getDurability(50))));
	public static final RegistryObject<Item> LUCKY_CHESTPLATE = ITEMS.register("lucky_chestplate",
		() -> new LuckyArmorItem(ModArmorMaterials.LUCKY.getHolder().orElseThrow(), ArmorItem.Type.CHESTPLATE,
			lucky().durability(ArmorItem.Type.CHESTPLATE.getDurability(50))));
	public static final RegistryObject<Item> LUCKY_LEGGINGS = ITEMS.register("lucky_leggings",
		() -> new LuckyArmorItem(ModArmorMaterials.LUCKY.getHolder().orElseThrow(), ArmorItem.Type.LEGGINGS,
			lucky().durability(ArmorItem.Type.LEGGINGS.getDurability(50))));
	public static final RegistryObject<Item> LUCKY_BOOTS = ITEMS.register("lucky_boots",
		() -> new LuckyArmorItem(ModArmorMaterials.LUCKY.getHolder().orElseThrow(), ArmorItem.Type.BOOTS,
			lucky().durability(ArmorItem.Type.BOOTS.getDurability(50))));

	/** Les 9 objets custom que le brush lucky distribue (un tous les 3 brushs). */
	public static List<RegistryObject<Item>> customPool() {
		return List.of(LUCKY_SWORD, LUCKY_BOW, LUCKY_PICKAXE, LUCKY_SHOVEL, LUCKY_LIGHTER,
			LUCKY_HELMET, LUCKY_CHESTPLATE, LUCKY_LEGGINGS, LUCKY_BOOTS);
	}
}
