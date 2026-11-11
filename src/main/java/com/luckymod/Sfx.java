package com.luckymod;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Sons : accepte aussi bien un SoundEvent qu'un Holder<SoundEvent> (le type varie selon les constantes vanilla). */
public final class Sfx {
	private Sfx() {}

	public static void play(Level level, double x, double y, double z, Object sound, float volume, float pitch) {
		SoundEvent ev = sound instanceof Holder<?> h ? (SoundEvent) h.value() : (SoundEvent) sound;
		level.playSound(null, x, y, z, ev, SoundSource.PLAYERS, volume, pitch);
	}

	public static void play(Level level, Vec3 p, Object sound, float volume, float pitch) {
		play(level, p.x, p.y, p.z, sound, volume, pitch);
	}
}
