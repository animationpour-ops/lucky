package com.luckymod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Particule maison du mod : une seule classe, 9 comportements (voir Kind).
 * Toujours en pleine luminosité (effet "lueur"), mise à l'échelle / fondu / rotation selon le type.
 */
public class LuckyParticle extends TextureSheetParticle {
	public enum Kind { SPARK, GLOW, RING, SHARD, STREAK, CLOVER, PORTAL, FLARE, EMBER }

	private final Kind kind;
	private final float baseSize;
	private final float spin;

	protected LuckyParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
							SpriteSet sprites, Kind kind) {
		super(level, x, y, z);
		this.kind = kind;
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;
		this.hasPhysics = false;
		this.gravity = 0.0f;
		this.friction = 0.92f;

		RandomSource r = level.random;
		float size = 0.2f;
		int life = 20;
		float rot = 0.0f;
		switch (kind) {
			case SPARK -> { size = 0.16f + r.nextFloat() * 0.14f; life = 10 + r.nextInt(10); this.friction = 0.9f; }
			case GLOW -> { size = 0.35f + r.nextFloat() * 0.3f; life = 14 + r.nextInt(10); this.friction = 0.95f; }
			case RING -> { size = 0.25f + r.nextFloat() * 0.1f; life = 8 + r.nextInt(4); this.friction = 0.96f; }
			case SHARD -> { size = 0.2f + r.nextFloat() * 0.15f; life = 16 + r.nextInt(10); this.gravity = 0.25f; rot = (r.nextFloat() - 0.5f) * 0.5f; }
			case STREAK -> { size = 0.5f + r.nextFloat() * 0.3f; life = 6 + r.nextInt(4); this.friction = 1.0f; }
			case CLOVER -> { size = 0.28f + r.nextFloat() * 0.12f; life = 30 + r.nextInt(20); this.friction = 0.94f; rot = (r.nextFloat() - 0.5f) * 0.3f; }
			case PORTAL -> { size = 0.25f + r.nextFloat() * 0.2f; life = 20 + r.nextInt(20); this.friction = 0.97f; rot = (r.nextFloat() - 0.5f) * 0.2f; }
			case FLARE -> { size = 1.2f + r.nextFloat() * 0.6f; life = 6 + r.nextInt(4); this.friction = 1.0f; }
			case EMBER -> { size = 0.1f + r.nextFloat() * 0.1f; life = 18 + r.nextInt(14); this.friction = 0.94f; }
		}
		this.baseSize = size;
		this.quadSize = size;
		this.lifetime = life;
		this.spin = rot;
		if (rot != 0.0f) {
			this.roll = r.nextFloat() * 6.2831f;
			this.oRoll = this.roll;
		}
		this.pickSprite(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.isAlive()) return;
		float t = (float) this.age / (float) this.lifetime;
		switch (this.kind) {
			case SPARK -> { this.alpha = 1.0f - t * t; this.quadSize = this.baseSize * (1.0f - 0.55f * t); }
			case GLOW -> { this.alpha = 0.85f * (1.0f - t); this.quadSize = this.baseSize * (1.0f + 0.7f * t); }
			case RING -> { this.alpha = 1.0f - t; this.quadSize = this.baseSize * (1.0f + 2.5f * t); }
			case SHARD -> { this.alpha = 1.0f - t * t * t; this.oRoll = this.roll; this.roll += this.spin; }
			case STREAK -> this.alpha = 1.0f - t;
			case CLOVER -> {
				this.alpha = t < 0.75f ? 1.0f : (1.0f - t) * 4.0f;
				this.oRoll = this.roll;
				this.roll += this.spin;
			}
			case PORTAL -> {
				this.alpha = t < 0.15f ? t / 0.15f : 1.0f - t;
				this.quadSize = this.baseSize * (1.0f - 0.4f * t);
				this.oRoll = this.roll;
				this.roll += this.spin;
			}
			case FLARE -> { this.alpha = 0.9f * (1.0f - t); this.quadSize = this.baseSize * (1.0f + 1.2f * t); }
			case EMBER -> { this.alpha = 1.0f - t; this.quadSize = this.baseSize * (1.0f - 0.7f * t); this.yd += 0.004; }
		}
	}

	@Override
	public int getLightColor(float partialTick) {
		return 15728880;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;
		private final Kind kind;

		public Provider(SpriteSet sprites, Kind kind) {
			this.sprites = sprites;
			this.kind = kind;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
									   double xSpeed, double ySpeed, double zSpeed) {
			return new LuckyParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, this.kind);
		}
	}
}
