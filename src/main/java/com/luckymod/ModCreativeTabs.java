package com.luckymod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
	private ModCreativeTabs() {}

	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LuckyMod.MOD_ID);

	public static final RegistryObject<CreativeModeTab> LUCKY = TABS.register("lucky",
		() -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.luckymod"))
			.icon(() -> new ItemStack(ModItems.LUCKY_BRUSH.get()))
			.displayItems((params, output) -> {
				output.accept(ModItems.LUCKY_BRUSH.get());
				output.accept(ModItems.LUCKY_SWORD.get());
				output.accept(ModItems.LUCKY_BOW.get());
				output.accept(ModItems.LUCKY_LIGHTER.get());
				output.accept(ModItems.LUCKY_PICKAXE.get());
				output.accept(ModItems.LUCKY_SHOVEL.get());
				output.accept(ModItems.LUCKY_HELMET.get());
				output.accept(ModItems.LUCKY_CHESTPLATE.get());
				output.accept(ModItems.LUCKY_LEGGINGS.get());
				output.accept(ModItems.LUCKY_BOOTS.get());
			})
			.build());
}
