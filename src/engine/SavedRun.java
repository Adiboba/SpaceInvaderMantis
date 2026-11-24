package engine;

/**
 * The run the player left with "Quit to menu", kept so the title screen can
 * offer "Continue" (SCRUM-18).
 *
 * Continue restarts the level that was quit, with the lives the player had
 * when quitting. Score, stats and pending diamonds go back to how that level
 * started, the same way Restart resets them, and the coins picked up in it
 * were already taken back when quitting. Items in the slots and run-wide
 * upgrades are not stored here: they stay in ItemSystem's current run, which
 * Continue keeps instead of starting a new one.
 *
 * The saved run lives in memory only, so closing the game ends it.
 */
public final class SavedRun {

	/** State to restart the quit level with, or null when nothing is saved. */
	private static GameState state;

	/**
	 * Not instantiable.
	 */
	private SavedRun() {
	}

	/**
	 * Tells whether a run is waiting to be continued.
	 *
	 * @return True if Continue has something to continue.
	 */
	public static boolean exists() {
		return state != null;
	}

	/**
	 * Keeps a run for Continue, replacing any run saved before.
	 *
	 * @param run
	 *            State to restart the quit level with.
	 */
	public static void save(final GameState run) {
		state = run;
		Core.getLogger().info("Saved run for Continue: level "
				+ run.getLevel() + ", " + run.getLivesRemaining()
				+ " lives, score " + run.getScore() + ".");
	}

	/**
	 * Hands the saved run to Continue and forgets it, so it can only be
	 * continued once.
	 *
	 * @return The saved run, or null if there is none.
	 */
	public static GameState take() {
		GameState run = state;
		state = null;
		return run;
	}

	/**
	 * Deletes the saved run, when a new run starts instead.
	 */
	public static void clear() {
		if (state != null)
			Core.getLogger().info("Deleted the saved run.");
		state = null;
	}
}
