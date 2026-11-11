package com.luckymod.items;

import com.luckymod.Blast;
import com.luckymod.LuckyText;
import com.luckymod.Scheduler;
import com.luckymod.Sfx;
import com.luckymod.Vfx;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pioche Lucky.
 *  1. Séisme Sphérique (clic droit sur un bloc) : le terrain s'effondre vers un point, puis une sphère de 5,5 blocs est pulvérisée.
 *  2. Foreuse Lucky (accroupi) : un tunnel de 3,5 blocs de large est percé sur 48 blocs par un foret d'anneaux tournants.
 */
public class LuckyPickaxe extends PickaxeItem {
	public LuckyPickaxe(Tier tier, Properties properties) {
		super(tier, properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
		if (level instanceof ServerLevel l && player instanceof ServerPlayer sp) {
			if (sp.isShiftKeyDown()) {
				drill(l, sp);
				sp.getCooldowns().addCooldown(this, 140);
			} else if (quake(l, sp)) {
				sp.getCooldowns().addCooldown(this, 80);
			}
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	// ------------------------------------------------------------ pouvoir 1 : séisme sphérique
	private boolean quake(ServerLevel l, ServerPlayer p) {
		BlockHitResult hit = Blast.aimBlock(p, 45.0);
		if (hit == null) {
			p.displayClientMessage(Component.translatable("lucky.no_target"), true);
			return false;
		}
		Vec3 c = Vec3.atCenterOf(hit.getBlockPos()).subtract(
			hit.getDirection().getStepX() * 2.0, hit.getDirection().getStepY() * 2.0, hit.getDirection().getStepZ() * 2.0);
		LuckyText.announce(p, "spherical_quake");
		Sfx.play(l, c, SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 0.5f);
		Vfx.rune(l, Vec3.atBottomCenterOf(hit.getBlockPos()).add(0, 1, 0), 6.0);
		Vfx.implode(l, Vfx.portal(), c, 9.0, 90, 10);
		Vfx.implode(l, Vfx.spark(), c, 7.0, 60, 10);
		Vfx.implode(l, Vfx.DUST_FADE, c, 10.0, 60, 10);

		Scheduler.later(10, () -> {
			List<BlockPos> blocks = Blast.sphere(l, c, 5.5);
			Blast.dig(l, p, blocks, 90, true);
			Vfx.bang(l, c, 4.0);
			Vfx.shock(l, c, 11.0, 8);
			Vfx.sphere(l, Vfx.spark(), c, 1.0, 100, 0.8);
			Vfx.sphere(l, Vfx.clover(), c, 1.0, 14, 0.3);
			Sfx.play(l, c, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.6f);
			Sfx.play(l, c, SoundEvents.ANVIL_LAND, 1.5f, 0.5f);
			for (LivingEntity e : Blast.near(l, c, 9.0, p)) {
				Blast.hurt(l, e, p, 25.0f);
				Blast.knock(e, c, 1.0, 1.1);
			}
		});
		return true;
	}

	// ------------------------------------------------------------ pouvoir 2 : foreuse
	private void drill(ServerLevel l, ServerPlayer p) {
		final Vec3 eye = p.getEyePosition();
		final Vec3 dir = p.getLookAngle();
		final Set<UUID> hit = new HashSet<>();
		LuckyText.announce(p, "drill");
		Sfx.play(l, eye, SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.5f);

		for (int i = 0; i < 24; i++) {
			final int idx = i;
			Scheduler.later(i, () -> {
				if (!p.isAlive()) return;
				for (int k = 0; k < 2; k++) {
					double d = 1.5 + (idx * 2 + k);
					Vec3 c = eye.add(dir.scale(d));
					for (BlockPos bp : Blast.sphere(l, c, 1.75)) {
						if (Blast.breakable(l, bp)) Blast.breakBlock(l, p, bp, true);
					}
					Vfx.ringFacing(l, Vfx.spark(), c, dir, 1.9, 14);
					Vfx.ringFacing(l, Vfx.DUST_BIG, c, dir, 1.2, 8);
					Vfx.ringFacing(l, Vfx.portal(), c.subtract(dir.scale(0.6)), dir, 0.9, 6);
					Vfx.p(l, Vfx.streak(), c, dir.scale(0.9));
					Vfx.scatter(l, Vfx.ember(), c, 4, 0.8, 0.2);
					for (LivingEntity e : Blast.near(l, c, 3.2, p)) {
						if (hit.add(e.getUUID())) {
							Blast.hurt(l, e, p, 28.0f);
							Vfx.bang(l, Blast.center(e), 1.0);
						}
					}
				}
				Sfx.play(l, eye.add(dir.scale(1.5 + idx * 2)), SoundEvents.STONE_BREAK, 1.6f, 0.5f);
			});
		}
		Scheduler.later(25, () -> {
			Vec3 end = eye.add(dir.scale(49.5));
			Vfx.bang(l, end, 3.0);
			Vfx.shock(l, end, 6.0, 5);
			Vfx.beam(l, eye.add(dir.scale(1.5)), end, 0.8);
			Sfx.play(l, end, SoundEvents.GENERIC_EXPLODE, 2.0f, 0.8f);
		});
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		LuckyText.tooltip(tooltip, "spherical_quake", "drill");
	}
}
