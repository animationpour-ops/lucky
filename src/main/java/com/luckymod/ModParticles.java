package com.luckymod;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Particules maison (rendues côté client par client/LuckyParticle). */
public final class ModParticles {
	private ModParticles() {}

	public static final DeferredRegister<ParticleType<?>> PARTICLES =
		DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, LuckyMod.MOD_ID);

	private static RegistryObject<SimpleParticleType> reg(String name) {
		return PARTICLES.register(name, () -> new SimpleParticleType(true));
	}

	public static final RegistryObject<SimpleParticleType> SPARK = reg("lucky_spark");
	public static final RegistryObject<SimpleParticleType> GLOW = reg("lucky_glow");
	public static final RegistryObject<SimpleParticleType> RING = reg("lucky_ring");
	public static final RegistryObject<SimpleParticleType> SHARD = reg("lucky_shard");
	public static final RegistryObject<SimpleParticleType> STREAK = reg("lucky_streak");
	public static final RegistryObject<SimpleParticleType> CLOVER = reg("lucky_clover");
	public static final RegistryObject<SimpleParticleType> PORTAL = reg("lucky_portal");
	public static final RegistryObject<SimpleParticleType> FLARE = reg("lucky_flare");
	public static final RegistryObject<SimpleParticleType> EMBER = reg("lucky_ember");
}
