package com.luckymod.items;

import com.luckymod.LuckyText;
import com.luckymod.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Brush Lucky : un vrai brush (il fonctionne sur le sable/gravier suspect des ruines) qui, à chaque bloc terminé,
 * donne des objets cheat (voir BrushTracker). Ici : les VFX pendant le brossage.
 */
public class LuckyBrushItem extends BrushItem {
	public LuckyBrushItem(Properties properties) {
		super(properties);
	}

	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
		super.onUseTick(level, user, stack, remainingUseDuration);
		if (!(level instanceof ServerLevel l) || !(user instanceof ServerPlayer p)) return;
		if (remainingUseDuration % 2 != 0) return;
		HitResult hr = p.pick(p.blockInteractionRange(), 1.0f, false);
		if (hr instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
			Vec3 c = Vec3.atCenterOf(b.getBlockPos()).add(
				b.getDirection().getStepX() * 0.55, b.getDirection().getStepY() * 0.55, b.getDirection().getStepZ() * 0.55);
			Vfx.scatter(l, Vfx.spark(), c, 3, 0.3, 0.06);
			Vfx.p(l, Vfx.clover(), c, new Vec3(0, 0.05, 0));
			if (remainingUseDuration % 6 == 0) Vfx.p(l, Vfx.portal(), c, new Vec3(0, 0.08, 0));
			if (remainingUseDuration % 10 == 0) Vfx.ring(l, Vfx.ring(), c, 0.5, 8, 0.04, 0.0);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		LuckyText.tooltip(tooltip, "lucky_brush");
		tooltip.add(Component.translatable("lucky.power.lucky_brush.desc2").withStyle(ChatFormatting.DARK_GRAY));
	}
}
