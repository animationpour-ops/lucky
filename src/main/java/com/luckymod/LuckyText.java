package com.luckymod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Textes (infobulles, barre d'action) : tout passe par les clés de langue "lucky.power.<id>". */
public final class LuckyText {
	private LuckyText() {}

	public static MutableComponent name(String id) {
		return Component.translatable("lucky.power." + id).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
	}

	public static void tooltip(List<Component> tip, String... ids) {
		tip.add(Component.translatable("lucky.tier").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
		for (String id : ids) {
			tip.add(name(id));
			tip.add(Component.translatable("lucky.power." + id + ".desc").withStyle(ChatFormatting.GRAY));
		}
	}

	public static void announce(ServerPlayer p, String id) {
		p.displayClientMessage(Component.literal("☘ ").withStyle(ChatFormatting.GREEN)
			.append(name(id)).append(Component.literal(" ☘").withStyle(ChatFormatting.GREEN)), true);
	}
}
