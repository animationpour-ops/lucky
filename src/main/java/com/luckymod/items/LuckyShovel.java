package com.luckymod.items;

import com.luckymod.Blast;
import com.luckymod.LuckyText;
import com.luckymod.Scheduler;
import com.luckymod.Sfx;
import com.luckymod.Vfx;
import com.luckymod.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pelle Lucky.
 *  1. Raz-de-Marée (clic droit) : un mur d'énergie de 9 blocs de large laboure 24 blocs de terrain en projetant tout.
 *  2. Impact Gravitationnel (accroupi) : bond dans les airs puis chute fracassante qui creuse un cratère de 7 blocs.
 */
public class LuckyShovel extends ShovelItem {
	public LuckyShovel(Tier tier, Properties properties) {
		super(tier, properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
		if (level instanceof ServerLevel l && player instanceof ServerPlayer sp) {
			if (sp.isShiftKeyDown()) {
				slam(l, sp);
				sp.getCooldowns().addCooldown(this, 180);
			} else {
				tidal(l, sp);
				sp.getCooldowns().addCooldown(this, 110);
			}
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	private static boolean diggable(BlockState s) {
		return s.is(BlockTags.MINEABLE_WITH_SHOVEL) || s.is(BlockTags.LEAVES) || s.is(BlockTags.SNOW) || s.is(BlockTags.SAND);
	}

	// ------------------------------------------------------------ pouvoir 1 : raz-de-marée
	private void tidal(ServerLevel l, ServerPlayer p) {
		Vec3 look = p.getLookAngle();
		final Vec3 fwd = new Vec3(look.x, 0, look.z).normalize();
		final Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
		final Vec3 origin = p.position();
		final Set<UUID> hit = new HashSet<>();
		LuckyText.announce(p, "tidal_wave");
		Sfx.play(l, origin, SoundEvents.ENDER_DRAGON_GROWL, 1.2f, 1.4f);
		Sfx.play(l, origin, SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.8f);

		for (int t = 0; t < 22; t++) {
			final int tt = t;
			Scheduler.later(t, () -> {
				if (!p.isAlive()) return;
				double d = 1.5 + tt * 1.1;
				Vec3 front = origin.add(fwd.scale(d));
				// Mur d'énergie : une crête de 9 blocs de large et 3,5 de haut.
				for (double w = -4.5; w <= 4.5; w += 0.5) {
					Vec3 base = front.add(side.scale(w));
					double crest = 2.0 + 1.5 * Math.cos(w * 0.35);
					for (double h = 0; h <= crest; h += 0.7) {
						Vfx.p(l, h > crest - 0.8 ? Vfx.spark() : Vfx.glow(), base.add(0, h + 0.2, 0), fwd.scale(0.35));
					}
					Vfx.still(l, Vfx.portal(), base.add(0, 0.6, 0), 1, 0.4);
					Vfx.p(l, Vfx.streak(), base.add(0, crest, 0), fwd.scale(0.6));
				}
				Vfx.line(l, Vfx.DUST_BIG, front.add(side.scale(-4.5)).add(0, 3.2, 0), front.add(side.scale(4.5)).add(0, 3.2, 0), 0.6);
				if (tt % 2 == 0) Sfx.play(l, front, SoundEvents.STONE_BREAK, 1.2f, 0.6f);

				// Labour : les 2 couches du dessus (blocs de pelle, feuilles, neige, sable).
				List<BlockPos> plow = new ArrayList<>();
				for (int w = -4; w <= 4; w++) {
					for (int s = 0; s < 2; s++) {
						Vec3 col = front.add(side.scale(w)).add(fwd.scale(s * 0.55));
						BlockPos top = BlockPos.containing(col.x, p.getY() + 3, col.z);
						int guard = 0;
						while (guard++ < 9 && (l.getBlockState(top).isAir() || !Blast.breakable(l, top))) {
							top = top.below();
						}
						for (int dy = 0; dy < 2; dy++) {
							BlockPos bp = top.below(dy);
							if (Blast.breakable(l, bp) && diggable(l.getBlockState(bp))) plow.add(bp);
						}
					}
				}
				for (BlockPos bp : plow) {
					if (!Blast.breakable(l, bp)) continue;
					Vfx.debris(l, l.getBlockState(bp), Vec3.atCenterOf(bp), 2);
					Blast.breakBlock(l, p, bp, true);
				}
				for (LivingEntity e : Blast.near(l, front.add(0, 1.2, 0), 5.5, p)) {
					if (hit.add(e.getUUID())) {
						Blast.hurt(l, e, p, 26.0f);
						Vfx.bang(l, Blast.center(e), 1.0);
					}
					e.setDeltaMovement(fwd.x * 1.4, 0.9, fwd.z * 1.4);
					e.hurtMarked = true;
				}
			});
		}
		Scheduler.later(22, () -> {
			Vec3 end = origin.add(fwd.scale(26));
			Vfx.shock(l, end, 8.0, 6);
			Vfx.bang(l, end.add(0, 1, 0), 2.5);
			Sfx.play(l, end, SoundEvents.GENERIC_EXPLODE, 1.5f, 0.9f);
		});
	}

	// ------------------------------------------------------------ pouvoir 2 : impact gravitationnel
	private void slam(ServerLevel l, ServerPlayer p) {
		LuckyText.announce(p, "gravity_slam");
		Vec3 start = p.position();
		Sfx.play(l, start, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 1.8f);
		Vfx.rune(l, start, 4.0);
		Vfx.sphere(l, Vfx.portal(), start.add(0, 1, 0), 1.6, 40, 0.25);
		p.setDeltaMovement(0, 1.7, 0);
		p.hurtMarked = true;
		ModEvents.IMPACT.add(p.getUUID());

		final boolean[] falling = { false };
		for (int t = 1; t <= 90; t++) {
			final int tt = t;
			Scheduler.later(t, () -> {
				if (!ModEvents.IMPACT.contains(p.getUUID())) return;
				if (!p.isAlive()) { ModEvents.IMPACT.remove(p.getUUID()); return; }
				Vec3 pos = p.position();
				Vfx.ringFacing(l, Vfx.spark(), pos.add(0, 0.9, 0), new Vec3(0, 1, 0), 1.3, 12);
				Vfx.p(l, Vfx.streak(), pos.add(0, 1.0, 0), new Vec3(0, falling[0] ? 1.0 : -0.6, 0));
				Vfx.scatter(l, Vfx.ember(), pos.add(0, 0.5, 0), 4, 0.4, 0.1);
				if (!falling[0] && p.getDeltaMovement().y <= 0.05 && tt > 4) {
					falling[0] = true;
					Sfx.play(l, pos, SoundEvents.TRIDENT_THUNDER, 1.4f, 0.7f);
				}
				if (falling[0] && !p.onGround()) {
					p.setDeltaMovement(0, -2.4, 0);
					p.hurtMarked = true;
					Vfx.line(l, Vfx.glow(), pos, pos.add(0, 9, 0), 0.8);
					Vfx.implode(l, Vfx.portal(), pos.add(0, 0.5, 0), 3.0, 14, 4);
				}
				if (falling[0] && p.onGround()) {
					ModEvents.IMPACT.remove(p.getUUID());
					crater(l, p, pos);
				}
			});
		}
		Scheduler.later(95, () -> ModEvents.IMPACT.remove(p.getUUID()));
	}

	private void crater(ServerLevel l, ServerPlayer p, Vec3 pos) {
		Vec3 c = pos.add(0, 0.2, 0);
		List<BlockPos> bowl = new ArrayList<>();
		for (BlockPos bp : Blast.sphere(l, c, 7.0)) {
			if (bp.getY() <= BlockPos.containing(c).getY() + 1) bowl.add(bp);
		}
		Blast.dig(l, p, bowl, 100, true);
		Vfx.bang(l, c.add(0, 1, 0), 4.0);
		Vfx.shock(l, c, 16.0, 10);
		Vfx.pillar(l, c, 22);
		Vfx.rune(l, c, 7.0);
		Vfx.sphere(l, Vfx.spark(), c.add(0, 0.5, 0), 1.0, 110, 0.7);
		Sfx.play(l, c, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.5f);
		Sfx.play(l, c, SoundEvents.ANVIL_LAND, 2.0f, 0.4f);
		for (int i = 0; i < 4; i++) {
			final double a = Math.PI / 2 * i + Math.PI / 4;
			Scheduler.later(i * 2, () -> Vfx.lightning(l, c.add(Math.cos(a) * 6, 0, Math.sin(a) * 6)));
		}
		for (LivingEntity e : Blast.near(l, c, 10.0, p)) {
			Blast.hurt(l, e, p, 45.0f);
			Blast.knock(e, c, 2.2, 0.9);
		}
		p.fallDistance = 0.0f;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		LuckyText.tooltip(tooltip, "tidal_wave", "gravity_slam");
	}
}
