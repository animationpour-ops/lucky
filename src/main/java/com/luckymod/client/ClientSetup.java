package com.luckymod.client;

import com.luckymod.LuckyMod;
import com.luckymod.ModItems;
import com.luckymod.ModParticles;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Côté client (bus du mod) : animation de l'arc + enregistrement des particules maison. */
@Mod.EventBusSubscriber(modid = LuckyMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
	private ClientSetup() {}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ItemProperties.register(ModItems.LUCKY_BOW.get(), ResourceLocation.withDefaultNamespace("pull"),
				(stack, level, entity, seed) -> {
					if (entity == null || !entity.isUsingItem() || entity.getUseItem() != stack) return 0.0f;
					return entity.getTicksUsingItem() / 20.0f;
				});
			ItemProperties.register(ModItems.LUCKY_BOW.get(), ResourceLocation.withDefaultNamespace("pulling"),
				(stack, level, entity, seed) ->
					entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
		});
	}

	@SubscribeEvent
	public static void onParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(ModParticles.SPARK.get(), s -> new LuckyParticle.Provider(s, LuckyParticle.Kind.SPARK));
		event.registerSpriteSet(ModParticles.GLOW.get(), s -> new LuckyParticle.Provider(s, LuckyParticle.Kind.GLOW));
		event.registerSpriteSet(ModParticles.RING.get(), s -> new LuckyParticle.Provider(s, LuckyParticle.Kind.RING));
		event.registerSpriteSet(ModParticles.SHARD.get(), s -> new LuckyParticle.Provider(s, LuckyParticle.Kind.SHARD));
		event.registerSpriteSet(ModParticles.STREAK.get(), s -> new LuckyParticle.Provider(s, LuckyParticle.Kind.STREAK));
		event.registerSpriteSet(ModParticles.CLOVER.get(), s -> new LuckyParticle.Provider(s, LuckyParticle.Kind.CLOVER));
		event.registerSpriteSet(ModParticles.PORTAL.get(), s -> new LuckyParticle.Provider(s, LuckyParticle.Kind.PORTAL));
		event.registerSpriteSet(ModParticles.FLARE.get(), s -> new LuckyParticle.Provider(s, LuckyParticle.Kind.FLARE));
		event.registerSpriteSet(ModParticles.EMBER.get(), s -> new LuckyParticle.Provider(s, LuckyParticle.Kind.EMBER));
	}
}
