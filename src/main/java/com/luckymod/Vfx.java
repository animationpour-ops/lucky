package com.luckymod;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Moteur de VFX du mod : primitives géométriques (faisceaux, hélices, anneaux, sphères, vortex, éclairs...)
 * construites avec les particules maison (voir ModParticles / client.LuckyParticle).
 */
public final class Vfx {
	private Vfx() {}

	public static final ParticleOptions DUST = new DustParticleOptions(new Vector3f(0.2f, 1.0f, 0.4f), 1.6f);
	public static final ParticleOptions DUST_BIG = new DustParticleOptions(new Vector3f(0.45f, 1.0f, 0.2f), 3.2f);
	public static final ParticleOptions DUST_FADE = new DustColorTransitionOptions(
		new Vector3f(0.7f, 1.0f, 0.6f), new Vector3f(0.0f, 0.35f, 0.1f), 2.4f);

	public static ParticleOptions spark() { return ModParticles.SPARK.get(); }
	public static ParticleOptions glow() { return ModParticles.GLOW.get(); }
	public static ParticleOptions ring() { return ModParticles.RING.get(); }
	public static ParticleOptions shard() { return ModParticles.SHARD.get(); }
	public static ParticleOptions streak() { return ModParticles.STREAK.get(); }
	public static ParticleOptions clover() { return ModParticles.CLOVER.get(); }
	public static ParticleOptions portal() { return ModParticles.PORTAL.get(); }
	public static ParticleOptions flare() { return ModParticles.FLARE.get(); }
	public static ParticleOptions ember() { return ModParticles.EMBER.get(); }

	// ------------------------------------------------------------ bas niveau
	/** Une particule avec une vitesse précise (count = 0 => la vitesse est lue telle quelle côté client). */
	public static void p(ServerLevel l, ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
		l.sendParticles(o, x, y, z, 0, vx, vy, vz, 1.0);
	}

	public static void p(ServerLevel l, ParticleOptions o, Vec3 c, Vec3 v) {
		p(l, o, c.x, c.y, c.z, v.x, v.y, v.z);
	}

	/** Particules immobiles (ou presque) autour d'un point. */
	public static void still(ServerLevel l, ParticleOptions o, Vec3 c, int count, double spread) {
		l.sendParticles(o, c.x, c.y, c.z, count, spread, spread, spread, 0.0);
	}

	/** Éclats éjectés dans toutes les directions (vitesse aléatoire). */
	public static void scatter(ServerLevel l, ParticleOptions o, Vec3 c, int count, double spread, double speed) {
		l.sendParticles(o, c.x, c.y, c.z, count, spread, spread, spread, speed);
	}

	public static Vec3 randDir(RandomSource r) {
		double x, y, z, d;
		do {
			x = r.nextDouble() * 2 - 1;
			y = r.nextDouble() * 2 - 1;
			z = r.nextDouble() * 2 - 1;
			d = x * x + y * y + z * z;
		} while (d > 1 || d < 1.0e-4);
		return new Vec3(x, y, z).normalize();
	}

	/** Base orthonormée (u, v) perpendiculaire à d. */
	public static Vec3[] basis(Vec3 d) {
		Vec3 n = d.normalize();
		Vec3 ref = Math.abs(n.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 u = n.cross(ref).normalize();
		Vec3 v = n.cross(u).normalize();
		return new Vec3[] { u, v };
	}

	// ------------------------------------------------------------ formes
	public static void line(ServerLevel l, ParticleOptions o, Vec3 a, Vec3 b, double step) {
		Vec3 d = b.subtract(a);
		double len = d.length();
		if (len < 1.0e-3) return;
		Vec3 dir = d.scale(1.0 / len);
		for (double t = 0; t <= len; t += step) {
			Vec3 pt = a.add(dir.scale(t));
			l.sendParticles(o, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
		}
	}

	/** Faisceau lumineux plein. */
	public static void beam(ServerLevel l, Vec3 a, Vec3 b, double step) {
		line(l, glow(), a, b, step);
		line(l, spark(), a, b, step * 2.5);
		line(l, DUST, a, b, step * 3);
	}

	/** Éclair zigzag. */
	public static void arc(ServerLevel l, Vec3 a, Vec3 b, double jitter) {
		Vec3 d = b.subtract(a);
		double len = d.length();
		if (len < 0.2) return;
		int seg = Math.max(3, (int) (len * 1.4));
		Vec3[] bs = basis(d);
		RandomSource r = l.getRandom();
		Vec3 prev = a;
		for (int i = 1; i <= seg; i++) {
			double t = i / (double) seg;
			double fade = Math.sin(Math.PI * t) * jitter;
			Vec3 pt = a.add(d.scale(t))
				.add(bs[0].scale((r.nextDouble() - 0.5) * 2 * fade))
				.add(bs[1].scale((r.nextDouble() - 0.5) * 2 * fade));
			if (i == seg) pt = b;
			line(l, spark(), prev, pt, 0.3);
			line(l, DUST, prev, pt, 0.5);
			line(l, glow(), prev, pt, 0.9);
			prev = pt;
		}
		p(l, flare(), b, Vec3.ZERO);
	}

	/** Double hélice autour d'un axe. */
	public static void helix(ServerLevel l, ParticleOptions o, Vec3 a, Vec3 b, double radius, double turns, int n) {
		Vec3 d = b.subtract(a);
		Vec3[] bs = basis(d);
		for (int i = 0; i < n; i++) {
			double t = i / (double) Math.max(1, n - 1);
			double ang = turns * Math.PI * 2 * t;
			for (int s = 0; s < 2; s++) {
				double aa = ang + s * Math.PI;
				Vec3 pos = a.add(d.scale(t)).add(bs[0].scale(Math.cos(aa) * radius)).add(bs[1].scale(Math.sin(aa) * radius));
				l.sendParticles(o, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
			}
		}
	}

	/** Anneau horizontal. outSpeed = vitesse d'expansion, vy = vitesse verticale. */
	public static void ring(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n, double outSpeed, double vy) {
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			double cx = Math.cos(a), cz = Math.sin(a);
			p(l, o, c.x + cx * r, c.y, c.z + cz * r, cx * outSpeed, vy, cz * outSpeed);
		}
	}

	/** Anneau dans le plan perpendiculaire à "normal". */
	public static void ringFacing(ServerLevel l, ParticleOptions o, Vec3 c, Vec3 normal, double r, int n) {
		Vec3[] bs = basis(normal);
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			Vec3 pos = c.add(bs[0].scale(Math.cos(a) * r)).add(bs[1].scale(Math.sin(a) * r));
			l.sendParticles(o, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
		}
	}

	/** Coquille sphérique (points de Fibonacci) avec vitesse radiale (négative = implosion). */
	public static void sphere(ServerLevel l, ParticleOptions o, Vec3 c, double radius, int n, double radialSpeed) {
		double golden = Math.PI * (3 - Math.sqrt(5));
		for (int i = 0; i < n; i++) {
			double y = 1 - 2 * (i + 0.5) / n;
			double rr = Math.sqrt(1 - y * y);
			double phi = golden * i;
			Vec3 dir = new Vec3(Math.cos(phi) * rr, y, Math.sin(phi) * rr);
			Vec3 pos = c.add(dir.scale(radius));
			p(l, o, pos, dir.scale(radialSpeed));
		}
	}

	/** Colonne de lumière verticale. */
	public static void pillar(ServerLevel l, Vec3 base, double height) {
		for (double y = 0; y <= height; y += 0.6) {
			Vec3 pt = base.add(0, y, 0);
			l.sendParticles(glow(), pt.x, pt.y, pt.z, 1, 0.15, 0, 0.15, 0);
			if (((int) (y / 0.6)) % 3 == 0) {
				p(l, spark(), pt, new Vec3(0, 0.25, 0));
				l.sendParticles(DUST, pt.x, pt.y, pt.z, 1, 0.4, 0, 0.4, 0);
			}
			if (((int) (y / 0.6)) % 8 == 0) {
				ring(l, ring(), pt, 0.9, 10, 0.05, 0);
			}
		}
	}

	/** Spirale ascendante en entonnoir (tornade). */
	public static void vortex(ServerLevel l, ParticleOptions o, Vec3 c, double radius, double height, double phase, int n, double swirl) {
		for (int i = 0; i < n; i++) {
			double t = i / (double) n;
			double rad = radius * (0.2 + 0.8 * t);
			double ang = phase + t * Math.PI * 7;
			double cx = Math.cos(ang), cz = Math.sin(ang);
			p(l, o, c.x + cx * rad, c.y + t * height, c.z + cz * rad, -cz * swirl, 0.12, cx * swirl);
		}
	}

	/** Onde de choc qui s'étend sur plusieurs ticks. */
	public static void shock(ServerLevel l, Vec3 c, double maxR, int ticks) {
		for (int i = 0; i < ticks; i++) {
			final double r = maxR * (i + 1) / ticks;
			Scheduler.later(i, () -> {
				int n = (int) Math.min(120, 14 + r * 6);
				ring(l, spark(), c.add(0, 0.15, 0), r, n, 0.0, 0.06);
				ring(l, DUST_BIG, c.add(0, 0.1, 0), r, n / 2, 0.0, 0.0);
				if ((int) (r * 2) % 2 == 0) ring(l, ring(), c.add(0, 0.2, 0), r, n / 3, 0.0, 0.0);
			});
		}
	}

	/** Cercle runique au sol : anneaux, pentagramme de lumière et quatre trèfles. */
	public static void rune(ServerLevel l, Vec3 c, double r) {
		Vec3 g = c.add(0, 0.08, 0);
		ring(l, spark(), g, r, (int) (r * 14), 0, 0);
		ring(l, glow(), g, r * 0.72, (int) (r * 8), 0, 0);
		Vec3[] pts = new Vec3[5];
		for (int i = 0; i < 5; i++) {
			double a = Math.PI * 2 * i / 5 - Math.PI / 2;
			pts[i] = g.add(Math.cos(a) * r, 0, Math.sin(a) * r);
		}
		for (int i = 0; i < 5; i++) line(l, DUST, pts[i], pts[(i + 2) % 5], 0.45);
		for (int i = 0; i < 4; i++) {
			double a = Math.PI / 2 * i + Math.PI / 4;
			p(l, clover(), g.add(Math.cos(a) * r * 0.55, 0.3, Math.sin(a) * r * 0.55), new Vec3(0, 0.03, 0));
		}
	}

	/** Gros flash blanc-vert. */
	public static void flash(ServerLevel l, Vec3 c, boolean emitter) {
		l.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
		for (int i = 0; i < 3; i++) p(l, flare(), c, Vec3.ZERO);
		if (emitter) l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
	}

	/** Explosion de particules maison : éclats, trèfles, éclats de cristal et anneau. */
	public static void bang(ServerLevel l, Vec3 c, double power) {
		flash(l, c, power >= 2);
		scatter(l, spark(), c, (int) (50 * power), 0.2, 0.45 * power);
		scatter(l, shard(), c, (int) (14 * power), 0.2, 0.35 * power);
		scatter(l, clover(), c, (int) (6 * power), 0.3, 0.2 * power);
		scatter(l, ember(), c, (int) (25 * power), 0.3, 0.25 * power);
		sphere(l, ring(), c, 0.5, (int) (10 * power), 0.4 * power);
		ring(l, DUST_BIG, c, 0.6, (int) (14 * power), 0.5 * power, 0.0);
	}

	/** Les particules naissent sur une sphère et convergent vers le centre. */
	public static void implode(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n, double ticksToCenter) {
		sphere(l, o, c, r, n, -r / ticksToCenter);
	}

	public static void debris(ServerLevel l, BlockState state, Vec3 c, int n) {
		l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), c.x, c.y, c.z, n, 0.5, 0.4, 0.5, 0.25);
	}

	/** Foudre purement visuelle. */
	public static void lightning(ServerLevel l, Vec3 pos) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
		if (bolt != null) {
			bolt.moveTo(pos.x, pos.y, pos.z);
			bolt.setVisualOnly(true);
			l.addFreshEntity(bolt);
		}
	}
}
