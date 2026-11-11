package com.luckymod;

import java.util.ArrayList;
import java.util.List;

/** Mini planificateur de tâches différées (en ticks serveur). Utilisé par les capacités à retardement. */
public final class Scheduler {
	private Scheduler() {}

	private static final class Task {
		int delay;
		final Runnable action;
		Task(int delay, Runnable action) { this.delay = delay; this.action = action; }
	}

	private static final List<Task> TASKS = new ArrayList<>();

	public static void later(int ticks, Runnable action) {
		TASKS.add(new Task(Math.max(0, ticks), action));
	}

	public static void tick() {
		if (TASKS.isEmpty()) return;
		List<Task> snapshot = new ArrayList<>(TASKS);
		for (Task t : snapshot) {
			if (--t.delay <= 0) {
				TASKS.remove(t);
				try {
					t.action.run();
				} catch (Exception e) {
					LuckyMod.LOGGER.error("Erreur dans une tâche différée", e);
				}
			}
		}
	}

	public static void clear() {
		TASKS.clear();
	}
}
