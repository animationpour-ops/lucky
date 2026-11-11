package com.luckymod.items;

import com.luckymod.Blast;
import com.luckymod.LuckyText;
import com.luckymod.Scheduler;
import com.luckymod.Sfx;
import com.luckymod.Vfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Briquet Lucky.
 *  1. Tornade de Feu Vert (clic droit) : un cyclone de flammes vertes avance, aspire, enflamme et arrache le décor.
 *  2. Pluie de Météores (accroupi) : 16 météores verts s'abattent sur la zone visée.
 */
public class LuckyLighter extends Item {
	public LuckyLighter(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
		if (level instanceof ServerLevel l && player instanceof ServerPlayer sp) {
			if (sp.isShiftKeyDown()) {
				meteors(l, sp);
				sp.getCooldowns().addCooldown(this, 260);
			} else {
				tornado(l, sp);
				sp.getCooldowns().addCooldown(this, 170);
			}
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	/** Pose le point sur le sol (descend dans l'air, remonte hors d'un bloc). */
	private static Vec3 ground(ServerLevel l, Vec3 v) {
		BlockPos bp = BlockPos.containing(v);
		int guard = 0;
		while (bp.getY() > l.getMinBuildHeight() && l.getBlockState(bp.below()).isAir() && guard++ < 40) bp = bp.below();
		guard = 0;
		while (!l.getBlockState(bp).isAir() && guard++ < 10) bp = bp.above();
		return Vec3.atBottomCenterOf(bp);
	}

	// ------------------------------------------------------------ pouvoir 1 : tornade de feu vert
	private void tornado(ServerLevel l, ServerPlayer p) {
		Vec3 look = p.getLookAngle();
		final Vec3 dir = new Vec3(look.x, 0, look.z).normalize();
		final Vec3[] pos = { ground(l, Blast.aim(p, 30.0)) };
		LuckyText.announce(p, "green_tornado");
		Sfx.play(l, pos[0], SoundEvents.BLAZE_SHOOT, 2.0f, 0.5f);
		Sfx.play(l, pos[0], SoundEvents.ENDER_DRAGON_GROWL, 1.2f, 1.6f);
		Vfx.rune(l, pos[0], 5.0);

		for (int t = 0; t < 110; t++) {
			final int tt = t;
			Scheduler.later(t, () -> {
				if (!p.isAlive()) return;
				RandomSource r = l.getRandom();
				Vec3 b = ground(l, pos[0].add(dir.scale(0.14)));
				pos[0] = b;
				double ph = tt * 0.5;
				Vfx.vortex(l, Vfx.spark(), b, 4.5, 14, ph, 36, 0.35);
				Vfx.vortex(l, Vfx.DUST_BIG, b, 4.0, 14, ph + 1.0, 28, 0.3);
				Vfx.vortex(l, Vfx.ember(), b, 3.5, 12, ph + 2.0, 20, 0.3);
				Vfx.vortex(l, Vfx.portal(), b, 3.0, 10, ph + 3.0, 10, 0.25);
				Vfx.still(l, Vfx.flare(), b.add(0, 0.8, 0), 1, 0.5);
				if (tt % 3 == 0) Vfx.ring(l, Vfx.glow(), b.add(0, 0.2, 0), 4.5, 16, 0.2, 0.05);
				if (tt % 8 == 0) Sfx.play(l, b, SoundEvents.FIRECHARGE_USE, 1.2f, 0.5f);

				AABB column = new AABB(b.x - 5, b.y - 1, b.z - 5, b.x + 5, b.y + 14, b.z + 5);
				for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, column, x -> Blast.isTarget(x, p))) {
					Vec3 rel = e.position().subtract(b);
					Vec3 swirl = new Vec3(-rel.z, 0, rel.x).normalize().scale(0.5);
					e.setDeltaMovement(swirl.x - rel.x * 0.08, 0.45, swirl.z - rel.z * 0.08);
					e.hurtMarked = true;
					e.setRemainingFireTicks(160);
					if (tt % 5 == 0) Blast.hurt(l, e, p, 7.0f);
				}
				// Feu au sol + blocs arrachés.
				if (tt % 2 == 0) {
					BlockPos fp = BlockPos.containing(b.x + (r.nextDouble() - 0.5) * 10, b.y + 0.5, b.z + (r.nextDouble() - 0.5) * 10);
					if (l.isEmptyBlock(fp) && l.getBlockState(fp.below()).isFaceSturdy(l, fp.below(), Direction.UP)) {
						l.setBlock(fp, Blocks.FIRE.defaultBlockState(), 3);
					}
				}
				if (tt % 3 == 0) {
					BlockPos bp = BlockPos.containing(b.x + (r.nextDouble() - 0.5) * 7, b.y + r.nextInt(3) - 1, b.z + (r.nextDouble() - 0.5) * 7);
					if (Blast.breakable(l, bp) && l.getBlockState(bp).getDestroySpeed(l, bp) <= 3.0f) {
						Vfx.debris(l, l.getBlockState(bp), Vec3.atCenterOf(bp), 6);
						Vfx.p(l, Vfx.shard(), Vec3.atCenterOf(bp), new Vec3(0, 0.5, 0));
						Blast.breakBlock(l, p, bp, true);
					}
				}
			});
		}
		Scheduler.later(110, () -> {
			Vec3 b = pos[0];
			Vfx.bang(l, b.add(0, 2, 0), 2.5);
			Vfx.shock(l, b, 9.0, 6);
			Vfx.pillar(l, b, 18);
			Sfx.play(l, b, SoundEvents.GENERIC_EXPLODE, 2.0f, 0.8f);
		});
	}

	// ------------------------------------------------------------ pouvoir 2 : pluie de météores
	private void meteors(ServerLevel l, ServerPlayer p) {
		final Vec3 target = Blast.aim(p, 70.0);
		LuckyText.announce(p, "meteor_rain");
		Sfx.play(l, p.position(), SoundEvents.WITHER_SPAWN, 0.7f, 1.6f);
		Vfx.rune(l, target, 9.0);
		Vfx.shock(l, target, 11.0, 6);
		RandomSource r = l.getRandom();
		for (int i = 0; i < 16; i++) {
			double ang = r.nextDouble() * Math.PI * 2;
			double dist = Math.sqrt(r.nextDouble()) * 10.0;
			final Vec3 impact = ground(l, target.add(Math.cos(ang) * dist, 8, Math.sin(ang) * dist));
			final Vec3 from = impact.add(14, 36, 8);
			Scheduler.later(8 + i * 3, () -> meteor(l, p, from, impact));
		}
	}

	private void meteor(ServerLevel l, ServerPlayer p, Vec3 from, Vec3 to) {
		final int steps = 10;
		Sfx.play(l, to, SoundEvents.FIRECHARGE_USE, 2.0f, 0.5f);
		Vfx.ring(l, Vfx.spark(), to.add(0, 0.1, 0), 2.5, 18, 0, 0);
		Vec3 back = from.subtract(to).normalize();
		for (int s = 0; s <= steps; s++) {
			final double t = s / (double) steps;
			Scheduler.later(s, () -> {
				Vec3 pos = from.lerp(to, t);
				Vfx.still(l, Vfx.flare(), pos, 1, 0.2);
				Vfx.still(l, Vfx.portal(), pos, 3, 0.5);
				Vfx.still(l, Vfx.DUST_BIG, pos, 4, 0.6);
				Vfx.line(l, Vfx.glow(), pos, pos.add(back.scale(8)), 0.9);
				Vfx.line(l, Vfx.ember(), pos, pos.add(back.scale(6)), 0.7);
			});
		}
		Scheduler.later(steps + 1, () -> {
			Vec3 c = to.add(0, 0.5, 0);
			Blast.explode(l, p, c, 3.2f);
			Vfx.shock(l, to, 6.0, 4);
			Vfx.scatter(l, Vfx.ember(), c, 40, 0.5, 0.5);
			for (LivingEntity e : Blast.near(l, c, 4.5, p)) {
				e.setRemainingFireTicks(120);
				Blast.hurt(l, e, p, 35.0f);
				Blast.knock(e, c, 1.2, 0.8);
			}
		});
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		LuckyText.tooltip(tooltip, "green_tornado", "meteor_rain");
	}
}
