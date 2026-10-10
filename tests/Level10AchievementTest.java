import engine.Achievement;
import engine.AchievementManager;
import engine.Core;
import engine.PlayerProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Standalone regression test. Compile into a separate test output directory. */
public final class Level10AchievementTest {
    private static int checks;
    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }
    private static Set<String> roster() {
        // Test fixtures only. These are not real game ship IDs.
        return new HashSet<String>(Arrays.asList("test_alpha", "test_beta", "test_gamma"));
    }
    private static Achievement fleet(AchievementManager manager) {
        for (Achievement a : manager.getAchievements())
            if (a.getId().equals(AchievementManager.LEVEL10_ALL_SHIPS_ID)) return a;
        throw new AssertionError("Fleet achievement missing");
    }
    private static Achievement achievement(AchievementManager manager, String id) {
        for (Achievement a : manager.getAchievements())
            if (a.getId().equals(id)) return a;
        throw new AssertionError("Achievement missing: " + id);
    }
    public static void main(String[] args) throws Exception {
        Path output = Paths.get(Core.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path save = output.getParent().resolve("player-profile");
        if (Files.exists(save))
            throw new IllegalStateException("Refusing to touch existing save: " + save
                    + ". Use a fresh isolated test output directory.");
        try {
            // Older saves have no level10CompletedShips property.
            Files.write(save, "totalEnemiesKilled=2\nunlockedAchievements=\n".getBytes(StandardCharsets.UTF_8));
            AchievementManager manager = new AchievementManager();
            check(manager.getTotalEnemiesKilled() == 2, "Old kill progress loads");
            check(manager.getProgressText(fleet(manager)).equals("PENDING"), "No roster means pending");
            check(manager.recordLevelCompleted(10, "test_alpha", true) == null, "No roster cannot unlock");
            Achievement first = manager.recordEnemyDefeated();
            check(first != null && first.getId().equals("first_kill"), "Existing kill achievement works");
            check(!fleet(manager).isUnlocked(), "Kills must not unlock fleet");
            boolean rejected = false;
            try { manager.configureLevel10Ships(new HashSet<String>()); }
            catch (IllegalArgumentException e) { rejected = true; }
            check(rejected, "Empty roster rejected");
            rejected = false;
            try { manager.configureLevel10Ships(new HashSet<String>(Arrays.asList("bad,id"))); }
            catch (IllegalArgumentException e) { rejected = true; }
            check(rejected, "Comma in ID rejected for safe persistence");
            check(manager.recordFlawlessLevelCompleted(true).isEmpty(), "Damaged level gives no survival progress");
            List<Achievement> survivalUnlocks = manager.recordFlawlessLevelCompleted(false);
            check(survivalUnlocks.size() == 1 && survivalUnlocks.get(0).getId().equals("bronze_survivor"), "First flawless level unlocks Bronze Survivor");
            for (int i = 0; i < 3; i++) manager.recordFlawlessLevelCompleted(false);
            check(achievement(manager, "silver_survivor").isUnlocked(), "Four flawless levels unlock Silver Survivor");
            for (int i = 0; i < 6; i++) manager.recordFlawlessLevelCompleted(false);
            check(achievement(manager, "gold_survivor").isUnlocked(), "Ten flawless levels unlock Gold Survivor");
            Set<String> required = roster();
            manager.configureLevel10Ships(required);
            required.clear();
            check(manager.getProgressText(fleet(manager)).equals("0/3"), "Roster defensively copied");
            manager.recordLevelCompleted(9, "test_alpha", true);
            manager.recordLevelCompleted(11, "test_alpha", true);
            manager.recordLevelCompleted(10, "test_alpha", false);
            manager.recordLevelCompleted(10, "unknown", true);
            manager.recordLevelCompleted(10, null, true);
            check(manager.getLevel10CompletedShipCount() == 0, "Invalid or failed completions ignored");
            check(manager.recordLevelCompleted(10, "test_alpha", true) == null, "First ship does not unlock");
            manager.recordLevelCompleted(10, "test_alpha", true);
            check(manager.getLevel10CompletedShipCount() == 1, "Duplicate ship counted once");
            AchievementManager restarted = new AchievementManager();
            restarted.configureLevel10Ships(roster());
            check(restarted.getLevel10CompletedShipCount() == 1, "Partial progress survives restart");
            check(restarted.getTotalEnemiesKilled() == 3, "Kills survive fleet save");
            check(restarted.getProgressText(achievement(restarted, "gold_survivor")).equals("UNLOCKED"), "Survival unlocks survive restart");
            check(restarted.recordLevelCompleted(10, "test_beta", true) == null, "Two ships still locked");
            Achievement unlocked = restarted.recordLevelCompleted(10, "test_gamma", true);
            check(unlocked != null && unlocked.getId().equals(AchievementManager.LEVEL10_ALL_SHIPS_ID), "Last distinct ship unlocks");
            check(restarted.recordLevelCompleted(10, "test_gamma", true) == null, "Unlock emitted once");
            check(restarted.getProgressText(fleet(restarted)).equals("UNLOCKED"), "UI unlock status");
            check(restarted.getRequirementText(fleet(restarted)).equals("Clear level 10 with every ship."), "UI shows correct condition");
            AchievementManager again = new AchievementManager();
            again.configureLevel10Ships(roster());
            check(fleet(again).isUnlocked(), "Unlocked state survives restart");
            check(again.recordLevelCompleted(10, "test_alpha", true) == null, "No repeat unlock after restart");
            rejected = false;
            try { again.configureLevel10Ships(new HashSet<String>(Arrays.asList("test_alpha"))); }
            catch (IllegalStateException e) { rejected = true; }
            check(rejected, "Cannot shrink configured roster mid session");
            PlayerProfile profile = Core.getFileManager().loadPlayerProfile();
            profile.getLevel10CompletedShips().clear();
            check(profile.getLevel10CompletedShips().size() == 3, "Profile returns defensive copy");
            check(profile.isAchievementUnlocked("first_kill"), "Previous unlock retained");
            System.out.println("PASS: " + checks + " checks");
        } finally {
            Files.deleteIfExists(save);
        }
    }
}
