package com.luckymod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Dégâts de zone, ciblage et destruction de blocs partagés par tous les pouvoirs. */
public final class Blast {
	private Blast() {}

	// ------------------------------------------------------------ cibles
	public static boolean isTarget(LivingEntity e, Player owner) {
		if (e == owner || !e.isAlive() || e.isSpectator()) return false;
		if (e instanceof TamableAnimal t && t.isOwnedBy(owner)) return false;
		return true;
	}

	public static List<LivingEntity> near(ServerLevel l, Vec3 c, double r, Player owner) {
		AABB box = new AABB(c, c).inflate(r);
		double r2 = r * r;
		return l.getEntitiesOfClass(LivingEntity.class, box, e -> isTarget(e, owner) && e.position().add(0, e.getBbHeight() / 2, 0).distanceToSqr(c) <= r2);
	}

	/** Dégâts magiques qui ignorent l'armure et les frames d'invulnérabilité. */
	public static void hurt(ServerLevel l, LivingEntity target, Player src, float dmg) {
		target.invulnerableTime = 0;
		target.hurt(l.damageSources().indirectMagic(src, src), dmg);
	}

	public static void knock(Entity e, Vec3 from, double power, double up) {
		Vec3 d = e.position().subtract(from);
		double h = Math.sqrt(d.x * d.x + d.z * d.z);
		double dx = h < 1.0e-3 ? 0 : d.x / h, dz = h < 1.0e-3 ? 0 : d.z / h;
		e.setDeltaMovement(dx * power, up, dz * power);
		e.hurtMarked = true;
	}

	public static Vec3 center(Entity e) {
		return e.position().add(0, e.getBbHeight() / 2.0, 0);
	}

	/** Entité vivante visée par le joueur (portée en blocs), ou null. */
	public static LivingEntity raycastLiving(LivingEntity user, double range) {
		Vec3 eye = user.getEyePosition();
		Vec3 look = user.getViewVector(1.0f);
		Vec3 end = eye.add(look.scale(range));
		AABB box = user.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(user, eye, end, box,
			e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != user, range * range);
		return hit == null ? null : (LivingEntity) hit.getEntity();
	}

	/** Point visé par le joueur : bloc, entité vivante ou bout de portée. */
	public static Vec3 aim(ServerPlayer p, double range) {
		LivingEntity le = raycastLiving(p, range);
		Vec3 eye = p.getEyePosition();
		Vec3 end = eye.add(p.getLookAngle().scale(range));
		BlockHitResult bhr = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		if (le != null) {
			Vec3 ec = center(le);
			if (bhr.getType() == HitResult.Type.MISS || ec.distanceToSqr(eye) < bhr.getLocation().distanceToSqr(eye)) return ec;
		}
		return bhr.getType() == HitResult.Type.MISS ? end : bhr.getLocation();
	}

	/** Bloc visé (ou null). */
	public static BlockHitResult aimBlock(ServerPlayer p, double range) {
		Vec3 eye = p.getEyePosition();
		Vec3 end = eye.add(p.getLookAngle().scale(range));
		BlockHitResult bhr = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		return bhr.getType() == HitResult.Type.BLOCK ? bhr : null;
	}

	// ------------------------------------------------------------ blocs
	public static boolean breakable(ServerLevel l, BlockPos pos) {
		BlockState s = l.getBlockState(pos);
		if (s.isAir()) return false;
		if (s.getDestroySpeed(l, pos) < 0) return false;
		return l.getBlockEntity(pos) == null;
	}

	public static void give(ServerPlayer p, ItemStack st) {
		if (st.isEmpty()) return;
		p.getInventory().add(st);
		// Inventaire plein : on ne jette que les objets précieux (pas les blocs communs) pour éviter des centaines d'entités.
		if (!st.isEmpty() && !(st.getItem() instanceof BlockItem)) p.drop(st, false);
	}

	public static void breakBlock(ServerLevel l, ServerPlayer p, BlockPos pos, boolean drops) {
		BlockState s = l.getBlockState(pos);
		if (drops) {
			for (ItemStack st : Block.getDrops(s, l, pos, null, p, p.getMainHandItem())) give(p, st);
		}
		l.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	/** Liste des blocs destructibles dans une sphère, triés du centre vers l'extérieur. */
	public static List<BlockPos> sphere(ServerLevel l, Vec3 c, double r) {
		List<BlockPos> out = new ArrayList<>();
		int ir = (int) Math.ceil(r);
		BlockPos cp = BlockPos.containing(c);
		for (int x = -ir; x <= ir; x++) {
			for (int y = -ir; y <= ir; y++) {
				for (int z = -ir; z <= ir; z++) {
					if (x * x + y * y + z * z > r * r) continue;
					BlockPos bp = cp.offset(x, y, z);
					if (bp.getY() < l.getMinBuildHeight() || bp.getY() >= l.getMaxBuildHeight()) continue;
					if (breakable(l, bp)) out.add(bp);
				}
			}
		}
		out.sort(Comparator.comparingDouble(bp -> bp.distToCenterSqr(c)));
		return out;
	}

	/** Détruit les blocs par paquets (étalé sur plusieurs ticks pour ne pas faire laguer le serveur). */
	public static void dig(ServerLevel l, ServerPlayer p, List<BlockPos> blocks, int perTick, boolean drops) {
		int total = blocks.size();
		int ticks = Math.max(1, (total + perTick - 1) / perTick);
		for (int t = 0; t < ticks; t++) {
			final int from = t * perTick, to = Math.min(total, from + perTick);
			Scheduler.later(t, () -> {
				for (int i = from; i < to; i++) {
					BlockPos bp = blocks.get(i);
					if (!breakable(l, bp)) continue;
					if (i % 4 == 0) {
						Vfx.debris(l, l.getBlockState(bp), Vec3.atCenterOf(bp), 3);
						Vfx.still(l, Vfx.spark(), Vec3.atCenterOf(bp), 1, 0.3);
					}
					breakBlock(l, p, bp, drops);
				}
			});
		}
	}

	/** Explosion qui détruit les blocs (visuel vanilla + VFX maison). */
	public static void explode(ServerLevel l, Player src, Vec3 c, float power) {
		l.explode(src, c.x, c.y, c.z, power, Level.ExplosionInteraction.TNT);
		Vfx.bang(l, c, Math.max(1.0, power / 2.0));
	}
}
