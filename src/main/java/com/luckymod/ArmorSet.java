package com.luckymod;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Armure Lucky (bonus de pièces + bonus d'ensemble).
 *  1. Aura Porte-Bonheur (passif) : effets cumulés par pièce, set complet = Force II + Résistance + trèfles en orbite.
 *  2. Nova Jackpot (rester accroupi 2 s avec l'ensemble complet) : onde de choc de 24 blocs, foudres, surpuissance 10 s.
 */
public final class ArmorSet {
	private ArmorSet() {}

	private static final Map<UUID, Integer> CHARGE = new HashMap<>();
	private static final Map<UUID, Long> READY_AT = new HashMap<>();
	private static final int CHARGE_TICKS = 40;
	private static final int COOLDOWN = 400;

	public static boolean has(ServerPlayer p, EquipmentSlot slot, net.minecraft.world.item.Item item) {
		return p.getItemBySlot(slot).is(item);
	}

	public static boolean full(ServerPlayer p) {
		return has(p, EquipmentSlot.HEAD, ModItems.LUCKY_HELMET.get())
			&& has(p, EquipmentSlot.CHEST, ModItems.LUCKY_CHESTPLATE.get())
			&& has(p, EquipmentSlot.LEGS, ModItems.LUCKY_LEGGINGS.get())
			&& has(p, EquipmentSlot.FEET, ModItems.LUCKY_BOOTS.get());
	}

	public static void tick(ServerPlayer p, int t) {
		boolean head = has(p, EquipmentSlot.HEAD, ModItems.LUCKY_HELMET.get());
		boolean chest = has(p, EquipmentSlot.CHEST, ModItems.LUCKY_CHESTPLATE.get());
		boolean legs = has(p, EquipmentSlot.LEGS, ModItems.LUCKY_LEGGINGS.get());
		boolean feet = has(p, EquipmentSlot.FEET, ModItems.LUCKY_BOOTS.get());
		boolean all = head && chest && legs && feet;

		if (t % 10 == 0) {
			if (head) {
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, false));
				p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, false));
			}
			if (chest) {
				p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, true, false, false));
				p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, false, false));
				if (t % 100 == 0) p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 3, true, false, false));
			}
			if (legs) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, true, false, false));
			if (feet) p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 3, true, false, false));
			if (all) {
				p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, true, false, false));
				p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, false));
			}
		}

		if (!all) {
			CHARGE.remove(p.getUUID());
			return;
		}
		if (!(p.level() instanceof ServerLevel l)) return;
		if (t % 3 == 0) aura(l, p, t);
		charge(l, p);
	}

	private static void aura(ServerLevel l, ServerPlayer p, int t) {
		Vec3 pos = p.position();
		for (int k = 0; k < 3; k++) {
			double a = t * 0.12 + k * Math.PI * 2 / 3;
			double cx = Math.cos(a), cz = Math.sin(a);
			Vfx.p(l, Vfx.clover(), pos.x + cx * 1.25, pos.y + 0.9 + Math.sin(t * 0.2 + k) * 0.5, pos.z + cz * 1.25, -cz * 0.1, 0.0, cx * 0.1);
		}
		Vfx.scatter(l, Vfx.ember(), pos.add(0, 0.2, 0), 2, 0.45, 0.04);
		if (t % 12 == 0) Vfx.ring(l, Vfx.ring(), pos.add(0, 0.1, 0), 1.0, 10, 0.06, 0.0);
		if (!p.onGround()) Vfx.p(l, Vfx.streak(), pos.add(0, 0.1, 0), new Vec3(0, -0.5, 0));
	}

	// ------------------------------------------------------------ pouvoir 2 : nova jackpot
	private static void charge(ServerLevel l, ServerPlayer p) {
		UUID id = p.getUUID();
		if (!p.isShiftKeyDown() || !p.onGround()) {
			CHARGE.remove(id);
			return;
		}
		int c = CHARGE.getOrDefault(id, 0);
		if (c < 0) return;           // déjà déclenchée : relâcher l'accroupissement pour réarmer
		c++;
		CHARGE.put(id, c);
		Vec3 pos = p.position();
		double prog = c / (double) CHARGE_TICKS;
		if (c > 3) {
			Vfx.implode(l, Vfx.portal(), pos.add(0, 1, 0), 5.0 - 3.0 * prog, 6, 3);
			Vfx.ring(l, Vfx.spark(), pos.add(0, 0.1, 0), 2.4 - 1.4 * prog, 14, 0, 0.25);
			if (c % 5 == 0) Vfx.rune(l, pos, 1.5 + 2.0 * prog);
		}
		if (c % 5 == 0) {
			p.displayClientMessage(Component.translatable("lucky.charging", (int) (prog * 100)).withStyle(ChatFormatting.GREEN), true);
		}
		if (c >= CHARGE_TICKS) {
			CHARGE.put(id, -1);
			long now = l.getGameTime();
			long ready = READY_AT.getOrDefault(id, 0L);
			if (now < ready) {
				p.displayClientMessage(Component.translatable("lucky.cooldown", (int) ((ready - now) / 20 + 1)).withStyle(ChatFormatting.RED), true);
				return;
			}
			READY_AT.put(id, now + COOLDOWN);
			nova(l, p);
		}
	}

	private static void nova(ServerLevel l, ServerPlayer p) {
		final Vec3 c = p.position();
		LuckyText.announce(p, "jackpot_nova");
		Sfx.play(l, c, SoundEvents.ENDER_DRAGON_GROWL, 3.0f, 0.6f);
		Sfx.play(l, c, SoundEvents.TOTEM_USE, 2.0f, 0.7f);
		Sfx.play(l, c, SoundEvents.BEACON_ACTIVATE, 2.0f, 1.5f);
		Vfx.bang(l, c.add(0, 1, 0), 5.0);
		Vfx.pillar(l, c, 40);
		Vfx.rune(l, c, 8.0);
		Vfx.sphere(l, Vfx.spark(), c.add(0, 1, 0), 1.0, 150, 1.0);
		Vfx.sphere(l, Vfx.clover(), c.add(0, 1, 0), 1.0, 30, 0.5);
		p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 3, false, true, true));
		p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 3, false, true, true));
		p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 3, false, true, true));
		p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 3, false, true, true));

		final Set<UUID> hit = new HashSet<>();
		for (int i = 1; i <= 20; i++) {
			final int step = i;
			final double r = 24.0 * i / 20.0;
			Scheduler.later(i, () -> {
				int n = (int) Math.min(170, 20 + r * 6);
				Vfx.ring(l, Vfx.spark(), c.add(0, 0.3, 0), r, n, 0, 0.1);
				Vfx.ring(l, Vfx.DUST_BIG, c.add(0, 0.2, 0), r, n / 2, 0, 0);
				Vfx.ring(l, Vfx.portal(), c.add(0, 1.0, 0), r, n / 3, 0, 0);
				Vfx.ring(l, Vfx.ring(), c.add(0, 0.5, 0), r, 30, 0, 0);
				for (int k = 0; k < 10; k++) {
					double a = l.getRandom().nextDouble() * Math.PI * 2;
					Vfx.p(l, Vfx.streak(), c.x + Math.cos(a) * r, c.y + 1.0, c.z + Math.sin(a) * r, 0, 0.6, 0);
				}
				AABB box = new AABB(c, c).inflate(r + 2, 8, r + 2);
				for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box, x -> Blast.isTarget(x, p))) {
					double dx = e.getX() - c.x, dz = e.getZ() - c.z;
					if (Math.sqrt(dx * dx + dz * dz) <= r + 1.5 && hit.add(e.getUUID())) {
						Vec3 ec = Blast.center(e);
						Vfx.arc(l, c.add(0, 1.5, 0), ec, 1.0);
						Vfx.bang(l, ec, 1.4);
						Blast.hurt(l, e, p, 60.0f);
						Blast.knock(e, c, 2.5, 1.0);
					}
				}
				if (step % 3 == 0) {
					double a = l.getRandom().nextDouble() * Math.PI * 2;
					Vfx.lightning(l, c.add(Math.cos(a) * r * 0.8, 0, Math.sin(a) * r * 0.8));
				}
			});
		}
	}

	public static void clear() {
		CHARGE.clear();
		READY_AT.clear();
	}
}
