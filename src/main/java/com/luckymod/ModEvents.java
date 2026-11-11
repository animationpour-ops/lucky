package com.luckymod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Événements Forge (bus principal) : tâches différées, suivi du brush, armure, chutes. */
@Mod.EventBusSubscriber(modid = LuckyMod.MOD_ID)
public final class ModEvents {
	private ModEvents() {}

	/** Joueurs en plein "Impact Gravitationnel" (pelle) : aucun dégât de chute. */
	public static final Set<UUID> IMPACT = new HashSet<>();

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Scheduler.tick();
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;
		int t = server.getTickCount();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			BrushTracker.tick(p, t);
			ArmorSet.tick(p, t);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		Scheduler.clear();
		ArmorSet.clear();
		BrushTracker.clear();
		IMPACT.clear();
	}

	/** Bottes Lucky et Impact Gravitationnel : pas de dégâts de chute. */
	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (event.getEntity() instanceof Player p
			&& (p.getItemBySlot(EquipmentSlot.FEET).is(ModItems.LUCKY_BOOTS.get()) || IMPACT.contains(p.getUUID()))) {
			event.setCanceled(true);
		}
	}
}
