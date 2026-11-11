package com.luckymod.client;

import com.luckymod.BrushTracker;
import com.luckymod.LuckyMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Infobulle du brush vanilla : progression vers le Brush Lucky. */
@Mod.EventBusSubscriber(modid = LuckyMod.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
	private ClientForgeEvents() {}

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (stack.is(Items.BRUSH)) {
			event.getToolTip().add(Component.translatable("lucky.brush.progress.tip",
				BrushTracker.getProgress(stack), BrushTracker.NEEDED).withStyle(ChatFormatting.GREEN));
		}
	}
}
