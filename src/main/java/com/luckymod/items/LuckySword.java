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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Épée Lucky.
 *  1. Foudre Jackpot (chaque coup) : un éclair vert saute de monstre en monstre (6 rebonds) ; 1 chance sur 5 de JACKPOT (dégâts x3 + pluie de trèfles).
 *  2. Dash Fantôme (clic droit) : charge fulgurante dans un tunnel d'anneaux lumineux qui lacère tout sur le passage.
 */
public class LuckySword extends SwordItem {
	public LuckySword(Tier tier, Properties properties) {
		super(tier, properties);
	}

	// ------------------------------------------------------------ pouvoir 1 : foudre jackpot
	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		if (!(attacker instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel l)) return result;

		boolean jackpot = l.getRandom().nextInt(5) == 0;
		Vec3 first = Blast.center(target);
		Vec3 hand = p.getEyePosition().subtract(0, 0.45, 0).add(p.getLookAngle().scale(0.9));
		Vfx.arc(l, hand, first, 0.5);
		Vfx.bang(l, first, jackpot ? 2.4 : 0.9);
		Blast.hurt(l, target, p, jackpot ? 45.0f : 15.0f);
		Sfx.play(l, first, SoundEvents.TRIDENT_THUNDER, 0.8f, 1.6f);
		if (jackpot) {
			Vfx.pillar(l, target.position(), 14);
			Vfx.rune(l, target.position(), 3.0);
			Vfx.scatter(l, Vfx.clover(), first.add(0, 2, 0), 30, 1.5, 0.15);
			Vfx.lightning(l, target.position());
			Sfx.play(l, first, SoundEvents.PLAYER_LEVELUP, 1.2f, 1.2f);
			LuckyText.announce(p, "jackpot");
		}

		// Rebonds : on prépare la chaîne (voisin le plus proche à chaque saut).
		Set<UUID> hit = new HashSet<>();
		hit.add(target.getUUID());
		List<LivingEntity> chain = new ArrayList<>();
		LivingEntity cur = target;
		for (int i = 0; i < 6; i++) {
			LivingEntity best = null;
			double bd = Double.MAX_VALUE;
			for (LivingEntity e : Blast.near(l, Blast.center(cur), 10.0, p)) {
				if (hit.contains(e.getUUID())) continue;
				double d = e.distanceToSqr(cur);
				if (d < bd) { bd = d; best = e; }
			}
			if (best == null) break;
			hit.add(best.getUUID());
			chain.add(best);
			cur = best;
		}
		Vec3 prev = first;
		int idx = 1;
		for (LivingEntity e : chain) {
			final Vec3 a = prev;
			final int delay = idx++ * 2;
			prev = Blast.center(e);
			Scheduler.later(delay, () -> {
				if (!e.isAlive()) return;
				Vec3 b = Blast.center(e);
				Vfx.arc(l, a, b, 0.7);
				Vfx.bang(l, b, 0.8);
				Blast.hurt(l, e, p, 12.0f);
				Sfx.play(l, b, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 1.9f);
			});
		}
		return result;
	}

	// ------------------------------------------------------------ pouvoir 2 : dash fantôme
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
		if (level instanceof ServerLevel l && player instanceof ServerPlayer sp) {
			dash(l, sp);
			player.getCooldowns().addCooldown(this, 70);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	private void dash(ServerLevel l, ServerPlayer p) {
		Vec3 look = p.getLookAngle();
		Vec3 dir = new Vec3(look.x, Math.max(-0.25, Math.min(0.35, look.y)), look.z).normalize();
		p.setDeltaMovement(dir.scale(3.1));
		p.hurtMarked = true;
		p.fallDistance = 0.0f;
		p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4, false, false, false));
		LuckyText.announce(p, "phantom_dash");
		Sfx.play(l, p.position(), SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.7f);
		Sfx.play(l, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
		Vec3 start = p.position();
		Vfx.shock(l, start, 4.0, 4);
		Vfx.sphere(l, Vfx.portal(), start.add(0, 1, 0), 1.5, 30, -0.12);

		Set<UUID> hit = new HashSet<>();
		for (int t = 0; t < 12; t++) {
			Scheduler.later(t, () -> {
				if (!p.isAlive()) return;
				p.fallDistance = 0.0f;
				Vec3 pos = p.position().add(0, 1.0, 0);
				Vec3 mv = p.getDeltaMovement();
				Vec3 n = mv.lengthSqr() > 0.01 ? mv.normalize() : dir;
				Vfx.ringFacing(l, Vfx.spark(), pos, n, 1.7, 22);
				Vfx.ringFacing(l, Vfx.portal(), pos.subtract(n.scale(0.7)), n, 1.2, 8);
				Vfx.ringFacing(l, Vfx.DUST, pos.subtract(n.scale(1.4)), n, 2.2, 16);
				Vfx.ringFacing(l, Vfx.ring(), pos.subtract(n.scale(2.1)), n, 2.8, 6);
				Vfx.still(l, Vfx.clover(), pos.subtract(n.scale(1.0)), 1, 0.7);
				Vfx.scatter(l, Vfx.ember(), pos, 6, 0.5, 0.1);
				for (LivingEntity e : Blast.near(l, pos, 3.4, p)) {
					if (!hit.add(e.getUUID())) continue;
					Vec3 c = Blast.center(e);
					Vfx.arc(l, pos, c, 0.4);
					Vfx.bang(l, c, 1.1);
					Blast.hurt(l, e, p, 30.0f);
					Blast.knock(e, pos, 1.3, 0.6);
				}
			});
		}
		Scheduler.later(12, () -> {
			if (!p.isAlive()) return;
			Vec3 end = p.position();
			p.fallDistance = 0.0f;
			Vfx.shock(l, end, 7.0, 6);
			Vfx.bang(l, end.add(0, 1, 0), 1.6);
			Sfx.play(l, end, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.6f);
		});
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		LuckyText.tooltip(tooltip, "chain_lightning", "phantom_dash");
	}
}
