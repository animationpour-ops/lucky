package com.luckymod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le système de brush.
 *  - Brush normal : chaque bloc suspect terminé compte (compteur dans le brush). À 10 il devient un Brush Lucky.
 *  - Brush Lucky : chaque bloc terminé donne un objet cheat vanilla ; tous les 3 blocs, un objet Lucky custom.
 * Détection : on suit le bloc suspect visé pendant le brossage ; quand il se transforme en sable/gravier normal, le brossage est terminé.
 */
public final class BrushTracker {
	private BrushTracker() {}

	public static final int NEEDED = 10;
	private static final String K_COUNT = "lucky_brushes";
	private static final String K_LUCKY = "lucky_count";
	private static final String K_MASK = "lucky_mask";

	private static final class State {
		BlockPos pos;
		ResourceKey<Level> dim;
		InteractionHand hand;
		int tick;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	private record Loot(Item item, int min, int max, int weight) {}

	private static List<Loot> table() {
		return List.of(
			new Loot(Items.BLAZE_ROD, 3, 8, 10), new Loot(Items.ENDER_PEARL, 4, 12, 10),
			new Loot(Items.DIAMOND, 4, 12, 10), new Loot(Items.DIAMOND_BLOCK, 1, 3, 4),
			new Loot(Items.NETHERITE_INGOT, 1, 3, 4), new Loot(Items.GOLDEN_APPLE, 4, 10, 8),
			new Loot(Items.ENCHANTED_GOLDEN_APPLE, 1, 3, 3), new Loot(Items.TOTEM_OF_UNDYING, 1, 1, 3),
			new Loot(Items.DIAMOND_SWORD, 1, 1, 4), new Loot(Items.DIAMOND_PICKAXE, 1, 1, 4),
			new Loot(Items.DIAMOND_AXE, 1, 1, 3), new Loot(Items.DIAMOND_HELMET, 1, 1, 3),
			new Loot(Items.DIAMOND_CHESTPLATE, 1, 1, 3), new Loot(Items.DIAMOND_LEGGINGS, 1, 1, 3),
			new Loot(Items.DIAMOND_BOOTS, 1, 1, 3), new Loot(Items.ELYTRA, 1, 1, 2),
			new Loot(Items.TNT, 8, 24, 6), new Loot(Items.ENDER_EYE, 4, 10, 5),
			new Loot(Items.OBSIDIAN, 16, 48, 4), new Loot(Items.EXPERIENCE_BOTTLE, 16, 48, 6),
			new Loot(Items.FIREWORK_ROCKET, 16, 48, 4), new Loot(Items.TRIDENT, 1, 1, 2),
			new Loot(Items.ANCIENT_DEBRIS, 2, 6, 3), new Loot(Items.NETHER_STAR, 1, 1, 1),
			new Loot(Items.END_CRYSTAL, 2, 6, 3), new Loot(Items.MACE, 1, 1, 1),
			new Loot(Items.WIND_CHARGE, 16, 48, 4), new Loot(Items.NETHERITE_SWORD, 1, 1, 2),
			new Loot(Items.NETHERITE_PICKAXE, 1, 1, 2), new Loot(Items.GOLDEN_CARROT, 16, 48, 4),
			new Loot(Items.BEACON, 1, 1, 1), new Loot(Items.ENDER_CHEST, 1, 1, 2));
	}

	private static ItemStack rollCheat(RandomSource r) {
		List<Loot> t = table();
		int total = 0;
		for (Loot x : t) total += x.weight();
		int roll = r.nextInt(total);
		for (Loot x : t) {
			roll -= x.weight();
			if (roll < 0) return new ItemStack(x.item(), x.min() + r.nextInt(x.max() - x.min() + 1));
		}
		return new ItemStack(Items.DIAMOND, 4);
	}

	// ------------------------------------------------------------ suivi du brossage
	private static boolean isBrush(ItemStack s) {
		return s.is(Items.BRUSH) || s.is(ModItems.LUCKY_BRUSH.get());
	}

	public static void tick(ServerPlayer p, int t) {
		if (!(p.level() instanceof ServerLevel l)) return;
		UUID id = p.getUUID();
		if (p.isUsingItem() && isBrush(p.getUseItem())) {
			HitResult hr = p.pick(p.blockInteractionRange(), 1.0f, false);
			if (hr instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK
				&& l.getBlockState(b.getBlockPos()).getBlock() instanceof BrushableBlock) {
				State s = STATES.computeIfAbsent(id, k -> new State());
				s.pos = b.getBlockPos().immutable();
				s.dim = l.dimension();
				s.hand = p.getUsedItemHand();
				s.tick = t;
				return;
			}
		}
		State s = STATES.get(id);
		if (s == null) return;
		if (s.dim != l.dimension()) {
			STATES.remove(id);
			return;
		}
		BlockState bs = l.getBlockState(s.pos);
		if (!(bs.getBlock() instanceof BrushableBlock)) {
			STATES.remove(id);
			if (t - s.tick <= 6 && !bs.isAir()) complete(l, p, s);
		} else if (t - s.tick > 80) {
			STATES.remove(id);
		}
	}

	public static void clear() {
		STATES.clear();
	}

	// ------------------------------------------------------------ données du brush
	private static int getInt(ItemStack stack, String key) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(key);
	}

	private static void setInt(ItemStack stack, String key, int v) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(key, v));
	}

	public static int getProgress(ItemStack stack) {
		return getInt(stack, K_COUNT);
	}

	// ------------------------------------------------------------ bloc terminé
	private static void complete(ServerLevel l, ServerPlayer p, State s) {
		ItemStack stack = p.getItemInHand(s.hand);
		Vec3 c = Vec3.atCenterOf(s.pos).add(0, 0.6, 0);
		if (stack.is(Items.BRUSH)) {
			normal(l, p, s.hand, stack, c);
		} else if (stack.is(ModItems.LUCKY_BRUSH.get())) {
			lucky(l, p, stack, c);
		}
	}

	private static void normal(ServerLevel l, ServerPlayer p, InteractionHand hand, ItemStack stack, Vec3 c) {
		int n = getInt(stack, K_COUNT) + 1;
		if (n >= NEEDED) {
			p.setItemInHand(hand, new ItemStack(ModItems.LUCKY_BRUSH.get()));
			transform(l, p);
		} else {
			setInt(stack, K_COUNT, n);
			p.displayClientMessage(Component.translatable("lucky.brush.progress", n, NEEDED).withStyle(ChatFormatting.GREEN), true);
			Vfx.scatter(l, Vfx.spark(), c, 12, 0.3, 0.2);
			Vfx.scatter(l, Vfx.clover(), c, 2, 0.2, 0.1);
			Sfx.play(l, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 1.0f + n * 0.08f);
		}
	}

	/** Cérémonie : le brush devient Lucky. */
	private static void transform(ServerLevel l, ServerPlayer p) {
		Vec3 pos = p.position();
		Vfx.pillar(l, pos, 30);
		Vfx.rune(l, pos, 5.0);
		Vfx.bang(l, pos.add(0, 1.5, 0), 4.0);
		Vfx.shock(l, pos, 14.0, 10);
		Vfx.sphere(l, Vfx.clover(), pos.add(0, 1.5, 0), 1.0, 40, 0.5);
		Vfx.sphere(l, Vfx.spark(), pos.add(0, 1.5, 0), 1.0, 120, 0.9);
		for (int i = 0; i < 4; i++) {
			final double a = Math.PI / 2 * i;
			Scheduler.later(i * 3, () -> Vfx.lightning(l, pos.add(Math.cos(a) * 4, 0, Math.sin(a) * 4)));
		}
		Sfx.play(l, pos, SoundEvents.TOTEM_USE, 1.5f, 1.0f);
		Sfx.play(l, pos, SoundEvents.PLAYER_LEVELUP, 1.5f, 0.8f);
		Sfx.play(l, pos, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.4f);
		p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 60, 20));
		p.connection.send(new ClientboundSetTitleTextPacket(
			Component.translatable("lucky.brush.title").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));
		p.connection.send(new ClientboundSetSubtitleTextPacket(
			Component.translatable("lucky.brush.subtitle").withStyle(ChatFormatting.DARK_GREEN)));
	}

	private static void lucky(ServerLevel l, ServerPlayer p, ItemStack brush, Vec3 c) {
		RandomSource r = l.getRandom();
		int n = getInt(brush, K_LUCKY) + 1;
		setInt(brush, K_LUCKY, n);
		boolean custom = n % 3 == 0;

		if (!custom) {
			Vfx.bang(l, c, 1.6);
			Vfx.line(l, Vfx.glow(), c, c.add(0, 7, 0), 0.7);
			Vfx.rune(l, c.subtract(0, 0.5, 0), 1.8);
			Sfx.play(l, c, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.5f, 0.7f);
			Sfx.play(l, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, 1.4f);
			spawn(l, c, rollCheat(r), false);
			if (r.nextInt(100) < 35) spawn(l, c, rollCheat(r), false);
			p.displayClientMessage(Component.translatable("lucky.brush.next", 3 - (n % 3)).withStyle(ChatFormatting.GREEN), true);
			return;
		}

		// Objet Lucky custom : on ne redonne pas deux fois le même avant d'avoir tout eu.
		List<RegistryObject<Item>> pool = ModItems.customPool();
		int full = (1 << pool.size()) - 1;
		int mask = getInt(brush, K_MASK) & full;
		List<Integer> free = new ArrayList<>();
		for (int i = 0; i < pool.size(); i++) if ((mask & (1 << i)) == 0) free.add(i);
		if (free.isEmpty()) {
			mask = 0;
			for (int i = 0; i < pool.size(); i++) free.add(i);
		}
		int pick = free.get(r.nextInt(free.size()));
		mask |= 1 << pick;
		setInt(brush, K_MASK, mask == full ? 0 : mask);

		ItemStack drop = new ItemStack(pool.get(pick).get());
		spawn(l, c, drop, true);
		Vec3 base = Vec3.atBottomCenterOf(BlockPos.containing(c));
		Vfx.pillar(l, base, 26);
		Vfx.rune(l, base, 4.5);
		Vfx.bang(l, c, 4.0);
		Vfx.shock(l, base, 10.0, 8);
		Vfx.sphere(l, Vfx.clover(), c, 1.0, 30, 0.4);
		Vfx.lightning(l, base);
		Sfx.play(l, c, SoundEvents.TOTEM_USE, 1.5f, 1.3f);
		Sfx.play(l, c, SoundEvents.PLAYER_LEVELUP, 1.5f, 1.0f);
		p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 45, 15));
		p.connection.send(new ClientboundSetTitleTextPacket(
			Component.translatable("lucky.brush.custom_title").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));
		p.connection.send(new ClientboundSetSubtitleTextPacket(drop.getHoverName().copy().withStyle(ChatFormatting.YELLOW)));
	}

	private static void spawn(ServerLevel l, Vec3 c, ItemStack st, boolean fancy) {
		RandomSource r = l.getRandom();
		ItemEntity ie = new ItemEntity(l, c.x, c.y, c.z, st, (r.nextDouble() - 0.5) * 0.2, 0.35, (r.nextDouble() - 0.5) * 0.2);
		ie.setDefaultPickUpDelay();
		if (fancy) {
			ie.setGlowingTag(true);
			ie.setUnlimitedLifetime();
		}
		l.addFreshEntity(ie);
	}
}
