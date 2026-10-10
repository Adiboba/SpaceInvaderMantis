# Level 10 with all ships

## Scope and current status

This patch adds the achievement backend, persistence, screen text and a standalone regression test. It targets Mantis commit bd98f37 and is suitable for a feature branch that merged that version without additional edits to these files.

The achievement is named Fleet Master with ID `level10_all_ships`. It reuses the existing player ship icon. No reward amount was specified, so this change awards no currency.

**Gameplay integration is not complete.** This baseline still has seven levels and placeholder ship selection. Do not raise NUM_LEVELS to 10 without actual level settings. Do not invent real ship IDs or use enemy ship types as the playable roster.

Until a complete required roster is supplied, the achievement remains locked and shows PENDING. It cannot be earned through ordinary gameplay in this baseline. The standalone test uses artificial ship IDs to verify backend behavior; it does not demonstrate a playable level 10.

## Changed files

| File | Change |
| --- | --- |
| src/engine/Achievement.java | Explicit requirement type; existing constructors remain compatible |
| src/engine/AchievementManager.java | Fleet achievement, roster configuration, completion event, progress display; enemy kills only evaluate kill achievements |
| src/engine/PlayerProfile.java | Set of distinct ship IDs that cleared level 10 |
| src/engine/FileManager.java | Save and reload level10CompletedShips; older saves default to an empty set |
| src/screen/AchievementsScreen.java | Display the correct condition and progress instead of assuming kills |
| src/engine/DrawManager.java | Use condition-aware text in its achievement rendering helper |
| tests/Level10AchievementTest.java | Standalone regression checks with isolated save handling |

## Logic

1. Configure the complete set of required playable ship model IDs once during startup.
2. On a confirmed level result, provide level number, actual ship model ID and whether the player won.
3. Ignore failed runs, levels other than 10, unknown ship IDs and events before configuration.
4. Add valid ship IDs to a set. Repeated clears with the same ship count once.
5. Unlock when the completed set contains every required ship ID. Save progress after each new valid ship clear.
6. Return the newly unlocked Achievement once, for the existing popup system. Subsequent clears return null.

The required roster must contain all models required by the assignment, including locked models. Do not configure it with only the player's currently owned ships. IDs use letters, digits, underscore, dot or hyphen, and must remain stable between runs. Once configured, a different roster is rejected for that manager instance. Agree on the roster before release; saved unlocks remain permanent if later releases add ships.

## Connection points for other teams

Ask the Ship Variety team for the complete required roster and the selected playable model ID. Ask the Level Design team for the level 10 victory event. Agree how mid-level ship switching is handled; this patch expects one unambiguous model ID credited for the clear.

The following are integration examples, not code already connected in the baseline:

```java
// requiredShipIds must come from the agreed complete ship catalogue.
Core.getAchievementManager().configureLevel10Ships(requiredShipIds);
```

In GameScreen, the existing successful-clear condition is enemyShipFormation.isEmpty() && lives > 0 inside the !levelFinished transition. Once activeShipId is provided by the ship system, invoke:

```java
// activeShipId must identify the actual model used to clear this level.
showUnlockedAchievement(Core.getAchievementManager()
        .recordLevelCompleted(this.level, activeShipId, true));
```

Do not call this just when entering level 10, incrementing the level number, quitting, or detecting game over. Call once on the clear transition. The existing display queue may not have time to show a popup before a level screen closes; end-to-end integration must test popup delivery across the transition and use a persistent queue if necessary.

## Apply in Windows Command Prompt

Close the game. Start on mantis/level10-all-ships with a clean working tree. The patch changes local files only; it does not commit, push or merge anything on GitHub.

```bat
cd /d C:\Users\Admin\SpaceInvaderMantis\SpaceInvaders-Mantis
```

```bat
git status
```

Assuming the patch was downloaded to C:\Users\Admin\Downloads:

```bat
git apply --check "C:\Users\Admin\Downloads\level10-all-ships.patch"
```

If the check produces no errors, apply it:

```bat
git apply "C:\Users\Admin\Downloads\level10-all-ships.patch"
```

If it fails, stop and inspect the error. Do not force it or overwrite newer work.

## Compile and test

Compile game and tests together into a dedicated test output directory. This does not require JUnit.

```bat
javac -encoding UTF-8 -d bin\level10-selftest src\engine\*.java src\entity\*.java src\screen\*.java src\item\*.java tests\Level10AchievementTest.java
```

```bat
java -cp "bin\level10-selftest;res" Level10AchievementTest
```

Expected final output: PASS: 23 checks. Save/load INFO messages are normal. The test uses test_alpha, test_beta and test_gamma as fixtures only. It writes a temporary player-profile next to its compiled output directory, refuses to run if a save already exists there, and cleans its own test save afterward. If it refuses, use a new output directory under a fresh parent; do not delete a real save.

The checks cover old profile loading, no unlock before configuration, the existing First Flight achievement, separation from kill events, empty/invalid roster rejection, defensive copies, incorrect levels, defeat, unknown IDs, duplicates, partial progress across restarts, final distinct-ship unlock, one-time notification, UI text, permanent saved unlock, roster-change rejection and retention of earlier achievements.

To inspect the game screen after tests:

```bat
javac -encoding UTF-8 -d bin src\engine\*.java src\entity\*.java src\screen\*.java src\item\*.java
```

```bat
java -cp "bin;res" engine.Core
```

Fleet Master should appear on the normal achievements page with its description and PENDING status. No automated GUI run was performed when preparing the patch; check it locally. Its full playable unlocking flow remains blocked by the integrations above.

## Review and commit

Review git diff, run the tests and inspect the UI. Stage only the changed files listed above, this guide and the test. Use a commit message such as `feat: track level 10 clears by ship`. If opening a PR before integration, make it a draft and describe the missing ship and level hooks. Do not claim the gameplay requirement is complete yet. DrawManager is shared code and needs the appropriate team review.
