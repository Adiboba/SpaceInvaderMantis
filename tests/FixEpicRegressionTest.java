import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import engine.Cooldown;
import engine.Core;
import engine.CurrencyManager;
import engine.DiamondManager;
import engine.GameSettings;
import engine.GameState;
import engine.InputManager;
import engine.PurchaseManager;
import engine.WarpIn;
import engine.soundEngine.SoundManager;
import entity.Bullet;
import entity.EnemyShip;
import entity.EnemyShipFormation;
import entity.Ship.ShipType;
import screen.GameScreen;
import screen.Screen;
import shop.Currency;
import shop.ShopCatalog;
import shop.ShopCategory;
import shop.ShopItem;
import shop.Skin;

/** Standalone regression checks; run with an isolated classes/save directory. */
public final class FixEpicRegressionTest {
    private static final GameSettings SETTINGS = new GameSettings(5, 2, 60, 2000);
    private static int checks;

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object get(Object target, String name) throws Exception {
        return field(target.getClass(), name).get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        field(target.getClass(), name).set(target, value);
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... args)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object call(Object target, String name) throws Exception {
        return call(target, name, new Class<?>[0]);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static GameScreen screen(int level, int score) {
        GameScreen screen = new GameScreen(new GameState(level, score, 3, 0, 0, 0),
                SETTINGS, false, 448, 520, 60);
        screen.initialize();
        return screen;
    }

    private static long time(Cooldown cooldown) throws Exception {
        return field(Cooldown.class, "time").getLong(cooldown);
    }

    private static void setTime(Cooldown cooldown, long time) throws Exception {
        field(Cooldown.class, "time").setLong(cooldown, time);
    }

    @SuppressWarnings("unchecked")
    private static Set<Bullet> bullets(GameScreen screen) throws Exception {
        return (Set<Bullet>) get(screen, "bullets");
    }

    private static void testScoreAndPurchases() throws Exception {
        GameScreen screen = screen(1, 0);
        EnemyShipFormation formation = (EnemyShipFormation) get(screen, "enemyShipFormation");
        EnemyShip enemy = formation.iterator().next();
        bullets(screen).add(new Bullet(enemy.getPositionX(), enemy.getPositionY(), -6));
        call(screen, "manageCollisions");
        check(screen.getGameState().getScore() == enemy.getPointValue(),
                "Defeating an enemy adds its points");

        for (int lives = 0; lives <= 3; lives++) {
            set(screen, "score", 0);
            set(screen, "lives", lives);
            call(screen, "awardLifeBonus");
            check(screen.getGameState().getScore() == (lives <= 1 ? 0 : (lives - 1) * 100),
                    "Life bonus for " + lives + " lives");
        }
        set(screen, "score", Integer.MAX_VALUE - 10);
        call(screen, "addScore", new Class<?>[] {long.class}, 20L);
        check(screen.getGameState().getScore() == Integer.MAX_VALUE, "Earned points cannot overflow");

        set(screen, "score", 125);
        call(screen, "restartLevel");
        check(screen.getGameState().getScore() == 0, "Restart restores the level-start score");
        set(screen, "score", 125);
        GameState previous = screen.getGameState();
        GameScreen next = screen(2, previous.getScore());
        check(next.getGameState().getScore() == 125, "Score carries into the next level");
        set(next, "score", 225);
        call(next, "restartLevel");
        check(next.getGameState().getScore() == 125, "Restart retains score from prior levels");

        CurrencyManager coins = CurrencyManager.getInstance();
        DiamondManager diamonds = DiamondManager.getInstance();
        coins.reset();
        diamonds.reset();
        PurchaseManager purchases = PurchaseManager.getInstance();
        ShopItem ship = ShopCatalog.findByRef(ShopCategory.SHIP, ShipType.FAST_MOVE.name());
        int price = purchases.getPrice(ship, Currency.COINS);
        check(purchases.purchase(ship, Currency.COINS) == PurchaseManager.PurchaseResult.INSUFFICIENT_FUNDS,
                "Purchase with no coins fails");
        coins.addCoins(price - 1);
        for (int i = 0; i < 3; i++)
            check(purchases.purchase(ship, Currency.COINS) == PurchaseManager.PurchaseResult.INSUFFICIENT_FUNDS
                    && coins.getCoins() == price - 1, "Repeated insufficient purchases do not deduct");
        coins.addCoins(1);
        check(purchases.purchase(ship, Currency.COINS) == PurchaseManager.PurchaseResult.SUCCESS
                && coins.getCoins() == 0, "Sufficient funds purchase succeeds");
        check(purchases.purchase(ship, Currency.COINS) == PurchaseManager.PurchaseResult.ALREADY_OWNED
                && coins.getCoins() == 0, "Repeated purchase is not charged");
        ShopItem skin = ShopCatalog.findByRef(ShopCategory.SKIN, Skin.CRIMSON_HULL.name());
        int diamondPrice = purchases.getPrice(skin, Currency.DIAMONDS);
        diamonds.addDiamonds(diamondPrice - 1);
        check(purchases.purchase(skin, Currency.DIAMONDS) == PurchaseManager.PurchaseResult.INSUFFICIENT_FUNDS
                && diamonds.getDiamonds() == diamondPrice - 1, "Insufficient diamonds are not deducted");
        diamonds.addDiamonds(1);
        check(purchases.purchase(skin, Currency.DIAMONDS) == PurchaseManager.PurchaseResult.SUCCESS
                && diamonds.getDiamonds() == 0, "Sufficient diamonds purchase succeeds");
        check(next.getGameState().getScore() == 125 && coins.getCoins() == 0,
                "Purchases do not spend score or the other currency");
    }

    private static void testCountdown() throws Exception {
        GameScreen screen = screen(1, 0);
        Cooldown inputDelay = (Cooldown) field(Screen.class, "inputDelay").get(screen);
        Cooldown spawn = (Cooldown) get(screen, "enemyShipSpecialCooldown");
        Cooldown pauseKey = (Cooldown) get(screen, "pauseKeyCooldown");
        WarpIn warp = new WarpIn();
        warp.startLevel(6000);
        set(screen, "warpIn", warp);
        boolean[] keys = (boolean[]) field(InputManager.class, "keys").get(null);
        Arrays.fill(keys, false);
        check(!inputDelay.checkFinished(), "Countdown starts active");

        for (int i = 0; i < 4; i++) {
            setTime(pauseKey, 0);
            keys[KeyEvent.VK_P] = true;
            keys[KeyEvent.VK_SPACE] = true;
            check((Boolean) call(screen, "handlePauseInput"), "P pauses during countdown");
            check((Boolean) get(screen, "paused"), "Held Space cannot resume the countdown");
            long countdownBefore = time(inputDelay);
            long spawnBefore = time(spawn);
            long startBefore = (Long) get(screen, "gameStartTime");
            long warpBefore = (Long) get(warp, "startTime");
            long simulatedPause = 12000;
            set(screen, "pauseStartedAt", (Long) get(screen, "pauseStartedAt") - simulatedPause);
            setTime(inputDelay, countdownBefore - simulatedPause);
            setTime(spawn, spawnBefore - simulatedPause);
            set(screen, "gameStartTime", startBefore - simulatedPause);
            set(warp, "startTime", warpBefore - simulatedPause);
            setTime(pauseKey, 0);
            check(!(Boolean) call(screen, "handlePauseInput"), "P resumes during countdown");
            check(!inputDelay.checkFinished(), "Paused time cannot complete countdown");
            check(Math.abs(time(inputDelay) - countdownBefore) < 1000, "Input countdown is preserved");
            check(Math.abs(time(spawn) - spawnBefore) < 1000, "Bonus spawn timer is preserved");
            check(Math.abs((Long) get(screen, "gameStartTime") - startBefore) < 1000,
                    "Countdown display remains synchronized");
            check(Math.abs((Long) get(warp, "startTime") - warpBefore) < 1000,
                    "Warp animation remains synchronized");
        }
        setTime(pauseKey, 0);
        check((Boolean) call(screen, "handlePauseInput"), "Pause before menu resume");
        long startBeforeMenuResume = time(inputDelay);
        set(screen, "pauseStartedAt", (Long) get(screen, "pauseStartedAt") - 12000);
        setTime(inputDelay, startBeforeMenuResume - 12000);
        keys[KeyEvent.VK_P] = false;
        keys[KeyEvent.VK_SPACE] = false;
        check((Boolean) call(screen, "handlePauseInput"), "Space release keeps menu paused");
        keys[KeyEvent.VK_SPACE] = true;
        check((Boolean) call(screen, "handlePauseInput"), "Menu resume consumes the pause frame");
        check(!(Boolean) get(screen, "paused") && !inputDelay.checkFinished()
                && Math.abs(time(inputDelay) - startBeforeMenuResume) < 1000,
                "Menu resume preserves the countdown");
        setTime(inputDelay, System.currentTimeMillis() - 7000);
        check(inputDelay.checkFinished(), "Countdown completes after active time");
        setTime(pauseKey, 0);
        keys[KeyEvent.VK_P] = true;
        check((Boolean) call(screen, "handlePauseInput"), "Pause works after countdown");
        long finishedTime = time(inputDelay);
        set(screen, "pauseStartedAt", (Long) get(screen, "pauseStartedAt") - 12000);
        setTime(pauseKey, 0);
        check(!(Boolean) call(screen, "handlePauseInput"), "Resume works after countdown");
        check(time(inputDelay) == finishedTime && inputDelay.checkFinished(),
                "A completed countdown is not restarted");
        Arrays.fill(keys, false);
    }

    private static void testSpecialRestart() throws Exception {
        GameScreen screen = screen(1, 0);
        call(screen, "restartLevel");
        check(get(screen, "enemyShipSpecial") == null, "Restart before spawn starts empty");
        EnemyShip special = new EnemyShip();
        special.move(100, 0);
        set(screen, "enemyShipSpecial", special);
        Cooldown oldSpawn = (Cooldown) get(screen, "enemyShipSpecialCooldown");
        call(screen, "restartLevel");
        check(get(screen, "enemyShipSpecial") == null, "Restart discards a live bonus ship");
        Cooldown newSpawn = (Cooldown) get(screen, "enemyShipSpecialCooldown");
        check(newSpawn != oldSpawn && !newSpawn.checkFinished(), "Restart starts a fresh spawn timer");

        special = new EnemyShip();
        special.setPositionX(150);
        set(screen, "enemyShipSpecial", special);
        bullets(screen).add(new Bullet(150, special.getPositionY(), -6));
        call(screen, "manageCollisions");
        check(special.isDestroyed() && screen.getGameState().getScore() == 100,
                "Bonus ship is defeated in one hit for 100 points");
        call(screen, "restartLevel");
        check(get(screen, "enemyShipSpecial") == null && screen.getGameState().getScore() == 0,
                "Restart discards the explosion and attempt score");
        special = new EnemyShip();
        check(!special.isDestroyed() && special.getHealth() == 1 && special.getPositionX() == -32,
                "Next bonus ship has fresh health and position");
        GameScreen newRun = screen(1, 0);
        check(get(newRun, "enemyShipSpecial") == null,
                "A new game after game over begins without a bonus ship");
    }

    public static void main(String[] args) throws Exception {
        Core.getLogger().setUseParentHandlers(false);
        SoundManager.getInstance().setMuted(true);
        testScoreAndPurchases();
        testCountdown();
        testSpecialRestart();
        System.out.println("FixEpicRegressionTest: " + checks + " checks passed");
    }
}
