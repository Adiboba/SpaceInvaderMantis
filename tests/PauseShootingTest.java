import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import engine.Cooldown;
import engine.Core;
import engine.GameSettings;
import engine.GameState;
import engine.InputManager;
import engine.KeyBindings;
import engine.soundEngine.SoundManager;
import entity.Bullet;
import entity.Ship;
import entity.Ship.ShipType;
import item.ItemAPI;
import item.ItemManager;
import item.ItemSystem;
import screen.GameScreen;

/** Standalone pause and firing regression checks; run with isolated save files. */
public final class PauseShootingTest {
    private static int checks;

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object get(Object target, String name) throws Exception {
        return field(target.getClass(), name).get(target);
    }

    private static void setTime(Cooldown cooldown, long time) throws Exception {
        field(Cooldown.class, "time").setLong(cooldown, time);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Cooldown shootingCooldown(Ship ship) throws Exception {
        return (Cooldown) get(ship, "shootingCooldown");
    }

    private static void checkShipRates() throws Exception {
        for (ShipType type : ShipType.values()) {
            for (double bonus : new double[] {0.0, 0.5, 2.0}) {
                Ship ship = new Ship(100, 400, type);
                ship.setItemBonuses(bonus, 0);
                int interval = (int) Math.round(1000.0 / (ship.getBaseFireRate() + bonus));
                Cooldown cooldown = shootingCooldown(ship);
                Set<Bullet> bullets = new HashSet<Bullet>();
                check(ship.shoot(bullets), type + " initial shot");
                check(bullets.size() == type.getBulletsPerShot(), type + " bullets per shot");
                check(!ship.shoot(bullets), type + " immediate shot blocked");
                check(field(Cooldown.class, "duration").getInt(cooldown) == interval,
                        type + " intended interval with bonus " + bonus);
                setTime(cooldown, System.currentTimeMillis() - interval + 100);
                check(!ship.shoot(bullets), type + " shot before interval blocked");
                setTime(cooldown, System.currentTimeMillis() - interval - 100);
                check(ship.shoot(bullets), type + " shot after interval allowed");
            }
        }

        Ship ship = new Ship(100, 400);
        Set<Bullet> bullets = new HashSet<Bullet>();
        check(ship.shoot(bullets), "Initial shot before upgrade");
        Cooldown cooldown = shootingCooldown(ship);
        long start = field(Cooldown.class, "time").getLong(cooldown);
        ship.setItemBonuses(2.0, 0);
        check(shootingCooldown(ship) == cooldown, "Upgrade keeps the cooldown");
        check(field(Cooldown.class, "time").getLong(cooldown) == start,
                "Upgrade preserves last shot time");
        check(!ship.shoot(bullets), "Upgrade does not grant an immediate shot");
        ship.setItemBonuses(0, 0);
        check(field(Cooldown.class, "time").getLong(cooldown) == start,
                "Upgrade expiry preserves last shot time");
        check(!ship.shoot(bullets), "Upgrade expiry does not grant an immediate shot");
    }

    private static void checkItemUpgrades(GameScreen screen, Ship ship,
                                          Set<Bullet> bullets) throws Exception {
        ItemSystem items = (ItemSystem) get(screen, "items");
        ItemAPI api = (ItemAPI) get(items, "api");
        items.update(ship, true);
        for (int i = 0; i < 2; i++) {
            ItemAPI.GrantResult rapid = api.tryGrant(new ItemAPI.GrantRequest(
                    "rapid-" + i, "rapid_fire", ItemAPI.GrantSource.REWARD, ItemAPI.GrantTiming.NOW));
            check(rapid.status == ItemAPI.GrantStatus.APPLIED, "Rapid Fire stack applied");
        }
        double rapidBonus = items.fireRateBonus(ship.getBaseFireRate());
        check(rapidBonus > 0.5, "Rapid Fire stacks increase firing rate");
        ItemAPI.GrantResult boost = api.tryGrant(new ItemAPI.GrantRequest(
                "boost", "boost", ItemAPI.GrantSource.REWARD, ItemAPI.GrantTiming.NOW));
        check(boost.status == ItemAPI.GrantStatus.STORED, "Boost stored");
        check(api.useSlot(boost.slotIndex) == ItemAPI.UseResult.USED, "Boost activated");
        double boostedBonus = items.fireRateBonus(ship.getBaseFireRate());
        check(boostedBonus > rapidBonus, "Boost increases firing rate on top of Rapid Fire");

        Cooldown cooldown = shootingCooldown(ship);
        setTime(cooldown, System.currentTimeMillis());
        ship.setItemBonuses(boostedBonus, 0);
        int boostedInterval = (int) Math.round(1000.0 / (ship.getBaseFireRate() + boostedBonus));
        check(field(Cooldown.class, "duration").getInt(cooldown) == boostedInterval,
                "Combined upgrades use the intended interval");
        check(!ship.shoot(bullets), "Activating upgrades does not grant a free shot");
        setTime(cooldown, System.currentTimeMillis() - boostedInterval - 100);
        check(ship.shoot(bullets), "Combined upgrades allow a shot at their faster rate");

        boolean[] keys = (boolean[]) field(InputManager.class, "keys").get(null);
        Arrays.fill(keys, false);
        keys[KeyEvent.VK_P] = true;
        keys[KeyEvent.VK_SPACE] = true;
        Cooldown pauseKey = (Cooldown) get(screen, "pauseKeyCooldown");
        Method handlePause = GameScreen.class.getDeclaredMethod("handlePauseInput");
        handlePause.setAccessible(true);
        setTime(pauseKey, 0);
        check((Boolean) handlePause.invoke(screen), "Combined upgrades can pause while firing");
        long pauseStart = System.currentTimeMillis() - 5000;
        field(GameScreen.class, "pauseStartedAt").setLong(screen, pauseStart);
        setTime(cooldown, pauseStart - 50);
        setTime(pauseKey, 0);
        check(!(Boolean) handlePause.invoke(screen), "Combined upgrades resume");
        check(!ship.shoot(bullets), "Combined upgrades cannot bypass cooldown by pausing");

        ItemManager manager = (ItemManager) get(api, "manager");
        ItemAPI.Bounds bounds = new ItemAPI.Bounds(ship.getPositionX(), ship.getPositionY(),
                ship.getWidth(), ship.getHeight());
        manager.update(api.getItemInfo("boost").durationMillis + 1,
                new ItemAPI.PlayerSnapshot(bounds, true, true));
        check(items.fireRateBonus(ship.getBaseFireRate()) == rapidBonus,
                "Boost expires while Rapid Fire remains");
        ship.setItemBonuses(rapidBonus, 0);
        check(!ship.shoot(bullets), "Boost expiry does not grant a free shot");
    }

    private static void checkReboundControlsAndQuit() throws Exception {
        KeyBindings bindings = KeyBindings.getInstance();
        bindings.resetToDefaults();
        check(bindings.bind(KeyBindings.Action.PAUSE, KeyEvent.VK_Q) == null,
                "Pause key can be rebound");
        check(bindings.bind(KeyBindings.Action.SHOOT, KeyEvent.VK_X) == null,
                "Shoot key can be rebound");
        GameScreen screen = new GameScreen(new GameState(1, 0, 3, 0, 0, 0),
                new GameSettings(5, 2, 60, 2000), false, 448, 520, 60);
        screen.initialize();
        Ship ship = (Ship) get(screen, "ship");
        Cooldown pauseKey = (Cooldown) get(screen, "pauseKeyCooldown");
        Cooldown selection = (Cooldown) get(screen, "pauseSelectionCooldown");
        boolean[] keys = (boolean[]) field(InputManager.class, "keys").get(null);
        Arrays.fill(keys, false);
        Method handlePause = GameScreen.class.getDeclaredMethod("handlePauseInput");
        handlePause.setAccessible(true);
        Set<Bullet> bullets = new HashSet<Bullet>();
        check(ship.shoot(bullets), "Rebound player fires");
        keys[KeyEvent.VK_X] = true;
        keys[KeyEvent.VK_Q] = true;
        check((Boolean) handlePause.invoke(screen), "Rebound pause key opens menu");
        check((Boolean) get(screen, "paused"), "Rebound shooting key does not resume");
        setTime(pauseKey, 0);
        check(!(Boolean) handlePause.invoke(screen), "Rebound pause key resumes");
        check(!ship.shoot(bullets), "Rebound pause cannot bypass cooldown");

        Arrays.fill(keys, false);
        keys[KeyEvent.VK_Q] = true;
        keys[KeyEvent.VK_SPACE] = true;
        setTime(pauseKey, 0);
        check((Boolean) handlePause.invoke(screen), "Held menu select can pause");
        check((Boolean) get(screen, "paused"), "Held menu select does not resume");
        keys[KeyEvent.VK_Q] = false;
        keys[KeyEvent.VK_SPACE] = false;
        check((Boolean) handlePause.invoke(screen), "Menu selection release registered");
        Class<?> optionType = Class.forName("screen.GameScreen$PauseOption");
        field(GameScreen.class, "pauseSelection").set(screen, optionType.getEnumConstants()[2]);
        keys[KeyEvent.VK_SPACE] = true;
        setTime(selection, 0);
        check((Boolean) handlePause.invoke(screen), "Quit selection keeps game paused");
        check((Boolean) get(screen, "quitConfirmOpen"), "Quit confirmation opens");
        setTime(selection, 0);
        handlePause.invoke(screen);
        check((Boolean) get(screen, "quitConfirmOpen"), "Held Space cannot confirm quit");
        keys[KeyEvent.VK_SPACE] = false;
        handlePause.invoke(screen);
        keys[KeyEvent.VK_SPACE] = true;
        handlePause.invoke(screen);
        check(!(Boolean) get(screen, "quitConfirmOpen") && (Boolean) get(screen, "paused"),
                "Default No closes confirmation and remains paused");
        bindings.resetToDefaults();
        bindings.save();
        Arrays.fill(keys, false);
    }

    public static void main(String[] args) throws Exception {
        Core.getLogger().setUseParentHandlers(false);
        SoundManager.getInstance().setMuted(true);
        checkShipRates();

        GameScreen screen = new GameScreen(new GameState(1, 0, 3, 0, 0, 0),
                new GameSettings(5, 2, 60, 2000), false, 448, 520, 60);
        screen.initialize();
        Ship ship = (Ship) get(screen, "ship");
        Cooldown shooting = shootingCooldown(ship);
        Cooldown pauseKey = (Cooldown) get(screen, "pauseKeyCooldown");
        boolean[] keys = (boolean[]) field(InputManager.class, "keys").get(null);
        Arrays.fill(keys, false);
        Method handlePause = GameScreen.class.getDeclaredMethod("handlePauseInput");
        handlePause.setAccessible(true);
        Set<Bullet> bullets = new HashSet<Bullet>();

        keys[KeyEvent.VK_SPACE] = true;
        check(!(Boolean) handlePause.invoke(screen) && ship.shoot(bullets), "Normal held Space fires");
        check(!ship.shoot(bullets), "Normal held Space respects cooldown");
        keys[KeyEvent.VK_P] = true;
        check((Boolean) handlePause.invoke(screen), "P enters pause while Space is held");
        check((Boolean) get(screen, "paused"), "Held Space does not select Resume");
        keys[KeyEvent.VK_P] = false;
        check((Boolean) handlePause.invoke(screen), "Held Space stays paused");
        check(bullets.size() == 1, "No shots created during pause input");

        long pauseStart = System.currentTimeMillis() - 5000;
        field(GameScreen.class, "pauseStartedAt").setLong(screen, pauseStart);
        setTime(shooting, pauseStart - 450);
        setTime(pauseKey, 0);
        keys[KeyEvent.VK_P] = true;
        check(!(Boolean) handlePause.invoke(screen), "P resumes");
        long elapsed = System.currentTimeMillis() - field(Cooldown.class, "time").getLong(shooting);
        check(elapsed >= 450 && elapsed < 600, "Pause preserves about 300ms remaining");
        check(!ship.shoot(bullets), "Cannot fire early after a long pause");
        setTime(shooting, System.currentTimeMillis() - 850);
        check(ship.shoot(bullets), "Shot becomes available after active cooldown");

        for (int i = 0; i < 20; i++) {
            setTime(pauseKey, 0);
            check((Boolean) handlePause.invoke(screen), "Repeated P enters pause " + i);
            check((Boolean) get(screen, "paused"), "Held Space cannot resume " + i);
            keys[KeyEvent.VK_P] = false;
            keys[KeyEvent.VK_SPACE] = false;
            check((Boolean) handlePause.invoke(screen), "Space release keeps pause open " + i);
            keys[KeyEvent.VK_SPACE] = true;
            check((Boolean) handlePause.invoke(screen), "Space menu resume consumes pause frame " + i);
            check(!(Boolean) get(screen, "paused"), "Space resumes the menu " + i);
            check(!ship.shoot(bullets), "Alternating P and Space cannot bypass cooldown " + i);
            keys[KeyEvent.VK_P] = true;
            setTime((Cooldown) get(screen, "pauseSelectionCooldown"), 0);
        }

        setTime(pauseKey, 0);
        check((Boolean) handlePause.invoke(screen), "Pause before upgrade change");
        pauseStart = System.currentTimeMillis() - 5000;
        field(GameScreen.class, "pauseStartedAt").setLong(screen, pauseStart);
        setTime(shooting, pauseStart - 50);
        ship.setItemBonuses(2.0, 0);
        setTime(pauseKey, 0);
        check(!(Boolean) handlePause.invoke(screen), "Resume after upgrade change");
        check(!ship.shoot(bullets), "Upgraded cooldown still excludes paused time");
        ship.setItemBonuses(0, 0);
        check(!ship.shoot(bullets), "Expired bonus cannot reset cooldown after pause");

        checkItemUpgrades(screen, ship, bullets);
        Arrays.fill(keys, false);
        checkReboundControlsAndQuit();
        System.out.println("PauseShootingTest: " + checks + " checks passed");
    }
}
