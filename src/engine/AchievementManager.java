package engine;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import engine.DrawManager.SpriteType;

/** Manages achievement progress and persistence. */
public class AchievementManager {

	/** Most achievements a single page of the achievements screen shows. */
	public static final int ACHIEVEMENTS_PER_PAGE = 5;

	/** Number of player kills required for First Flight. */
	private static final int THREE_KILLS_TARGET = 3;
	/** The id we use for the weakest ship. */
	public static final String STARTER_SHIP_ID = "starter";
	/** Level that must be cleared to unlock Endless Mode. */
	private static final int ENDLESS_UNLOCK_LEVEL = 10;
	/** Identifier of the Infinity Void achievement. */
	private static final String INFINITY_VOID_ID = "infinity_void";

	/** Persistent player profile. */
	private PlayerProfile playerProfile;
	/** Normal achievements, shown on page 1 of the achievements screen. */
	private List<Achievement> normalAchievements;
	/** Tier achievements, shown on page 2 of the achievements screen. */
	private List<Achievement> tierAchievements;

	/** Creates the manager and loads the saved player profile. */
	public AchievementManager() {
		try {
			this.playerProfile = Core.getFileManager().loadPlayerProfile();
		} catch (IOException | NumberFormatException e) {
			Core.getLogger().warning("Couldn't load player profile.");
			this.playerProfile = new PlayerProfile();
		}

		this.normalAchievements = new ArrayList<Achievement>();
		this.tierAchievements = new ArrayList<Achievement>();

		// Page 1: normal achievements. Add new ones below.
		addFirstKillAchievement();
		addStarterShipWinAchievement();
		addInfinityVoidAchievement();

		// Page 2: tier achievements. The tier team adds theirs below,
		// using addTierAchievement(...).
	}

	/** Adds the First Flight achievement. */
	private void addFirstKillAchievement() {
		addNormalAchievement(new Achievement("first_kill", "First Flight",
				"Welcome to Invaders.", THREE_KILLS_TARGET,
				SpriteType.FirstFlight, this.playerProfile
				.isAchievementUnlocked("first_kill")));
	}

	/** Adds the Humble Beginnings achievement. */
	private void addStarterShipWinAchievement() {
		addNormalAchievement(new Achievement("starter_ship_win",
				"Humble Beginnings", "Beat the game with the starter ship.", 0,
				SpriteType.Weakestship, this.playerProfile
				.isAchievementUnlocked("starter_ship_win"), Color.RED));
	}

	/**
	 * Adds the Infinity Void achievement. It has no kill requirement (0);
	 * recordLevelCompleted() unlocks it.
	 */
	private void addInfinityVoidAchievement() {
		addNormalAchievement(new Achievement(INFINITY_VOID_ID,
				"Infinity Void", "Clear level " + ENDLESS_UNLOCK_LEVEL
						+ " to unlock Endless Mode.", 0,
				SpriteType.InfinityVoid, this.playerProfile
						.isAchievementUnlocked(INFINITY_VOID_ID),
				new Color(160, 32, 240)));
	}

	/**
	 * Adds an achievement to page 1 (normal achievements).
	 *
	 * @param achievement Achievement to add.
	 */
	private void addNormalAchievement(final Achievement achievement) {
		addToPage(this.normalAchievements, achievement, "normal");
	}

	/**
	 * Adds an achievement to page 2 (tier achievements).
	 *
	 * @param achievement Achievement to add.
	 */
	@SuppressWarnings("unused")
	private void addTierAchievement(final Achievement achievement) {
		addToPage(this.tierAchievements, achievement, "tier");
	}

	/**
	 * Adds an achievement to a page, refusing it when the page is full.
	 *
	 * @param page        Page list to add to.
	 * @param achievement Achievement to add.
	 * @param pageName    Page name, used in the log message.
	 */
	private void addToPage(final List<Achievement> page,
						   final Achievement achievement, final String pageName) {
		if (page.size() >= ACHIEVEMENTS_PER_PAGE) {
			Core.getLogger().warning("The " + pageName + " achievement page "
					+ "is full, skipping " + achievement.getId() + ".");
			return;
		}
		page.add(achievement);
	}

	/**
	 * Records one confirmed enemy defeat and saves the resulting progress.
	 *
	 * @return Newly unlocked achievement, or null when nothing unlocks.
	 */
	public final Achievement recordEnemyDefeated() {
		this.playerProfile.recordEnemyDefeated();
		Achievement unlockedAchievement = null;

		for (Achievement achievement : getAchievements())
			if (!achievement.isUnlocked()
					&& achievement.getRequiredEnemyKills() > 0
					&& this.playerProfile.getTotalEnemiesKilled()
					>= achievement.getRequiredEnemyKills()) {
				achievement.unlock();
				this.playerProfile.unlockAchievement(achievement.getId());
				// TODO Connect the shared CurrencyManager reward here when its API is available.
				unlockedAchievement = achievement;
			}

		saveProfile();
		return unlockedAchievement;
	}

	/**
	 * Called when the player beats the game.
	 *
	 * @param shipId Ship used for this run.
	 * @return Newly unlocked achievement, or null.
	 */
	public final Achievement recordGameWon(final String shipId) {
		Achievement unlockedAchievement = null;
		for (Achievement achievement : getAchievements())
			if (!achievement.isUnlocked()
					&& achievement.getId().equals("starter_ship_win")
					&& STARTER_SHIP_ID.equals(shipId)) {
				achievement.unlock();
				this.playerProfile.unlockAchievement(achievement.getId());
				unlockedAchievement = achievement;
			}
		if (unlockedAchievement != null)
			saveProfile();
		return unlockedAchievement;
	}

	/**
	 * Records that the player cleared a level.
	 *
	 * @param level Number of the level just cleared.
	 * @return Newly unlocked achievement, or null when nothing unlocks.
	 */
	public final Achievement recordLevelCompleted(final int level) {
		if (level >= ENDLESS_UNLOCK_LEVEL)
			return unlockById(INFINITY_VOID_ID);
		return null;
	}

	/**
	 * Unlocks one achievement by identifier and saves the progress.
	 *
	 * @param id Identifier of the achievement to unlock.
	 * @return The achievement if it was just unlocked, otherwise null.
	 */
	private Achievement unlockById(final String id) {
		for (Achievement achievement : getAchievements())
			if (achievement.getId().equals(id) && !achievement.isUnlocked()) {
				achievement.unlock();
				this.playerProfile.unlockAchievement(id);
				saveProfile();
				return achievement;
			}
		return null;
	}

	/** Saves the player profile, retaining progress after restarting. */
	private void saveProfile() {
		try {
			Core.getFileManager().savePlayerProfile(this.playerProfile);
		} catch (IOException e) {
			Core.getLogger().warning("Couldn't save player profile.");
		}
	}

	/** @return Read-only list of every achievement, normal then tier. */
	public final List<Achievement> getAchievements() {
		List<Achievement> all = new ArrayList<Achievement>(
				this.normalAchievements);
		all.addAll(this.tierAchievements);
		return Collections.unmodifiableList(all);
	}

	/** @return Read-only list of normal achievements (page 1). */
	public final List<Achievement> getNormalAchievements() {
		return Collections.unmodifiableList(this.normalAchievements);
	}

	/** @return Read-only list of tier achievements (page 2). */
	public final List<Achievement> getTierAchievements() {
		return Collections.unmodifiableList(this.tierAchievements);
	}

	/** @return Total enemies defeated across all games. */
	public final int getTotalEnemiesKilled() {
		return this.playerProfile.getTotalEnemiesKilled();
	}

}