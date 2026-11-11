package com.luckymod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.List;

/** Armure Lucky : mêmes valeurs que la Netherite (et donc le même modèle 3D), texture portail vert. */
public final class ModArmorMaterials {
	private ModArmorMaterials() {}

	public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, LuckyMod.MOD_ID);

	public static final RegistryObject<ArmorMaterial> LUCKY = MATERIALS.register("lucky", () -> {
		EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		defense.put(ArmorItem.Type.BOOTS, 3);
		defense.put(ArmorItem.Type.LEGGINGS, 6);
		defense.put(ArmorItem.Type.CHESTPLATE, 8);
		defense.put(ArmorItem.Type.HELMET, 3);
		defense.put(ArmorItem.Type.BODY, 11);
		return new ArmorMaterial(
			defense,
			30,
			SoundEvents.ARMOR_EQUIP_NETHERITE,
			() -> Ingredient.of(Items.EMERALD),
			// textures/models/armor/lucky_layer_1.png et lucky_layer_2.png
			List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(LuckyMod.MOD_ID, "lucky"))),
			4.0f,
			0.1f
		);
	});
}
