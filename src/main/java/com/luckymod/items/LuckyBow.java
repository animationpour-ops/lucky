package com.luckymod.items;

import com.luckymod.Blast;
import com.luckymod.LuckyText;
import com.luckymod.Scheduler;
import com.luckymod.Sfx;
import com.luckymod.Vfx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Arc Lucky (à pleine puissance) :
 *  1. Rail Jackpot : rayon perforant de 110 blocs avec double hélice, qui explose à l'impact.
 *  2. Trou Noir (accroupi) : un trou noir de portail aspire tout pendant 4,5 s puis s'effondre en explosion géante.
 */
public class LuckyBow extends BowItem {
	public LuckyBow(Properties properties) {
		super(properties);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
		super.releaseUsing(stack, level, user, timeLeft);
		if (!(level instanceof ServerLevel l) || !(user instanceof ServerPlayer p)) return;
		int used = 72000 - timeLeft;
		if (BowItem.getPowerForTime(used) < 0.95f) return;
		if (p.getCooldowns().isOnCooldown(this)) return;
		if (p.isShiftKeyDown()) {
			blackHole(l, p);
			p.getCooldowns().addCooldown(this, 220);
		} else {
			rail(l, p);
			p.getCooldowns().addCooldown(this, 50);
		}
	}

	// ------------------------------------------------------------ pouvoir 1 : rail jackpot
	private void rail(ServerLevel l, ServerPlayer p) {
		Vec3 eye = p.getEyePosition();
		Vec3 dir = p.getLookAngle();
		Vec3 end = eye.add(dir.scale(110.0));
		BlockHitResult bhr = l.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		if (bhr.getType() != HitResult.Type.MISS) end = bhr.getLocation();
		final Vec3 hitPoint = end;
		Vec3 start = eye.add(dir.scale(1.2)).subtract(0, 0.25, 0);
		double len = start.distanceTo(end);

		LuckyText.announce(p, "jackpot_rail");
		Sfx.play(l, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 1.4f);
		Sfx.play(l, p.position(), SoundEvents.TRIDENT_THUNDER, 1.0f, 1.2f);

		// Faisceau + double hélice + anneaux tous les 6 blocs.
		Vfx.beam(l, start, end, 0.6);
		Vfx.helix(l, Vfx.spark(), start, end, 1.0, len / 3.0, (int) (len * 2));
		Vfx.helix(l, Vfx.DUST_BIG, start, end, 1.7, len / 5.0, (int) len);
		for (double d = 2; d < len; d += 6) {
			Vec3 pt = start.add(dir.scale(d));
			Vfx.ringFacing(l, Vfx.ring(), pt, dir, 2.0, 16);
			Vfx.ringFacing(l, Vfx.spark(), pt, dir, 1.4, 12);
			Vfx.still(l, Vfx.clover(), pt, 1, 0.5);
		}
		Vfx.bang(l, start, 1.0);

		// Tout ce qui est sur la ligne est perforé.
		AABB box = new AABB(start, end).inflate(1.5);
		List<LivingEntity> victims = l.getEntitiesOfClass(LivingEntity.class, box, e -> Blast.isTarget(e, p));
		for (LivingEntity e : victims) {
			if (e.getBoundingBox().inflate(0.9).clip(start, end).isEmpty()) continue;
			Vec3 c = Blast.center(e);
			Blast.hurt(l, e, p, 45.0f);
			Vfx.bang(l, c, 1.5);
			Vfx.arc(l, c, c.add(0, 3, 0), 0.6);
		}

		// Impact : explosion qui détruit les blocs, pilier de lumière, foudres.
		Scheduler.later(3, () -> {
			Blast.explode(l, p, hitPoint, 5.0f);
			Vfx.shock(l, hitPoint, 12.0, 8);
			Vfx.pillar(l, hitPoint, 25);
			Vfx.rune(l, hitPoint, 4.0);
			Vfx.lightning(l, hitPoint);
			for (LivingEntity e : Blast.near(l, hitPoint, 7.0, p)) {
				Blast.hurt(l, e, p, 25.0f);
				Blast.knock(e, hitPoint, 1.6, 0.7);
			}
		});
	}

	// ------------------------------------------------------------ pouvoir 2 : trou noir
	private void blackHole(ServerLevel l, ServerPlayer p) {
		final Vec3 c = Blast.aim(p, 80.0).add(0, 1.5, 0);
		LuckyText.announce(p, "black_hole");
		Sfx.play(l, c, SoundEvents.END_PORTAL_SPAWN, 2.0f, 0.8f);
		final int duration = 90;
		for (int t = 0; t < duration; t++) {
			final int tt = t;
			Scheduler.later(t, () -> {
				if (!p.isAlive()) return;
				RandomSource r = l.getRandom();
				// Noyau + jets verticaux.
				Vfx.still(l, Vfx.portal(), c, 3, 0.35);
				if (tt % 4 == 0) Vfx.p(l, Vfx.flare(), c, Vec3.ZERO);
				Vfx.p(l, Vfx.streak(), c, new Vec3(0, 0.7, 0));
				Vfx.p(l, Vfx.streak(), c, new Vec3(0, -0.7, 0));
				// Disque d'accrétion : les particules tournent en spirale vers le centre.
				double big = 5.5 + 1.5 * Math.sin(tt * 0.2);
				for (int k = 0; k < 16; k++) {
					double a = tt * 0.35 + k * Math.PI * 2 / 16;
					double rr = big * (0.3 + 0.7 * (((k * 7 + tt) % 16) / 16.0));
					double cx = Math.cos(a), cz = Math.sin(a);
					Vfx.p(l, k % 2 == 0 ? Vfx.spark() : Vfx.portal(),
						c.x + cx * rr, c.y + (r.nextDouble() - 0.5) * 0.6, c.z + cz * rr,
						-cz * 0.32 - cx * 0.14, 0.0, cx * 0.32 - cz * 0.14);
				}
				if (tt % 6 == 0) {
					Vfx.ring(l, Vfx.ring(), c, 7.0, 22, -0.55, 0.0);
					Vfx.implode(l, Vfx.DUST_FADE, c, 8.0, 40, 14);
				}
				if (tt % 12 == 0) Sfx.play(l, c, SoundEvents.PORTAL_AMBIENT, 1.5f, 0.6f + tt / 150.0f);
				// Aspiration + dégâts de marée.
				for (LivingEntity e : Blast.near(l, c, 16.0, p)) {
					Vec3 to = c.subtract(Blast.center(e));
					double dist = Math.max(1.0, to.length());
					Vec3 pull = to.normalize().scale(Math.min(1.1, 2.2 / dist + 0.15));
					e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(pull));
					e.hurtMarked = true;
					if (tt % 5 == 0 && dist < 6) Blast.hurt(l, e, p, 9.0f);
				}
			});
		}
		// Effondrement final.
		Scheduler.later(duration, () -> {
			Blast.explode(l, p, c, 8.0f);
			Vfx.bang(l, c, 5.0);
			Vfx.shock(l, c.subtract(0, 1.4, 0), 20.0, 12);
			Vfx.sphere(l, Vfx.spark(), c, 1.0, 120, 0.9);
			Vfx.sphere(l, Vfx.portal(), c, 1.0, 80, 0.6);
			Sfx.play(l, c, SoundEvents.ENDER_DRAGON_GROWL, 3.0f, 0.7f);
			Sfx.play(l, c, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
			for (int i = 0; i < 6; i++) {
				final double ang = Math.PI * 2 * i / 6;
				Scheduler.later(i * 2, () -> Vfx.lightning(l, c.add(Math.cos(ang) * 8, -1.5, Math.sin(ang) * 8)));
			}
			for (LivingEntity e : Blast.near(l, c, 14.0, p)) {
				Blast.hurt(l, e, p, 70.0f);
				Blast.knock(e, c, 2.2, 1.0);
			}
		});
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		LuckyText.tooltip(tooltip, "jackpot_rail", "black_hole");
	}
}
