package engine;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Keys the player can change for gameplay actions. Menus keep their own
 * fixed keys (arrows, W/S, Space, Enter, ESC); only the actions below can
 * be rebound. Each action has exactly one key, and no key is used by two
 * actions. Bindings are saved by FileManager and loaded on first use.
 */
public final class KeyBindings {

	/** Gameplay actions that can be rebound, with their default keys. */
	public enum Action {
		/** Moves the ship left. */
		MOVE_LEFT("Move Left", KeyEvent.VK_LEFT),
		/** Moves the ship right. */
		MOVE_RIGHT("Move Right", KeyEvent.VK_RIGHT),
		/** Fires a bullet. */
		SHOOT("Shoot", KeyEvent.VK_SPACE),
		/** Opens and closes the pause menu. */
		PAUSE("Pause", KeyEvent.VK_P),
		/** Uses the item in slot 1. */
		ITEM_1("Item 1", KeyEvent.VK_1),
		/** Uses the item in slot 2. */
		ITEM_2("Item 2", KeyEvent.VK_2),
		/** Uses the item in slot 3. */
		ITEM_3("Item 3", KeyEvent.VK_3),
		/** Uses the item in slot 4 (only with the extra slot equip). */
		ITEM_4("Item 4", KeyEvent.VK_4);

		/** Name shown in the settings screen. */
		private final String label;
		/** Key used when nothing is saved. */
		private final int defaultKey;

		Action(final String label, final int defaultKey) {
			this.label = label;
			this.defaultKey = defaultKey;
		}

		/** @return Name shown in the settings screen. */
		public String getLabel() {
			return this.label;
		}

		/** @return Key used when nothing is saved. */
		public int getDefaultKey() {
			return this.defaultKey;
		}
	}

	/** Item actions in slot order: slot 0 uses ITEMS[0], and so on. */
	private static final Action[] ITEMS = { Action.ITEM_1, Action.ITEM_2,
			Action.ITEM_3, Action.ITEM_4 };

	/** Highest key code InputManager keeps track of. */
	private static final int MAX_KEY_CODE = 255;

	/**
	 * Keys the pause menu uses to move and select. Pause can't be bound to
	 * them, or one press would both close the menu and act in it.
	 */
	private static final int[] PAUSE_MENU_KEYS = { KeyEvent.VK_SPACE,
			KeyEvent.VK_ENTER, KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_UP,
			KeyEvent.VK_DOWN };

	/** Singleton instance. */
	private static KeyBindings instance;

	/** Current key of every action. */
	private final Map<Action, Integer> keys =
			new EnumMap<Action, Integer>(Action.class);

	/** Application logger. */
	private final Logger logger = Core.getLogger();

	/** Starts from the defaults, then applies the saved bindings if valid. */
	private KeyBindings() {
		resetToDefaults();
		try {
			Map<Action, Integer> saved = Core.getFileManager()
					.loadKeyBindings();
			if (saved.isEmpty())
				return;
			// Actions missing from an older file keep their default key.
			Map<Action, Integer> merged =
					new EnumMap<Action, Integer>(this.keys);
			merged.putAll(saved);
			if (isValidSet(merged)) {
				this.keys.putAll(merged);
				this.logger.info("Loaded key bindings.");
			} else {
				this.logger.warning("Saved key bindings are invalid or "
						+ "use a key twice, using defaults.");
			}
		} catch (IOException e) {
			this.logger.warning("Could not load key bindings, using "
					+ "defaults.");
		}
	}

	/**
	 * Returns the shared instance, loading saved bindings on first use.
	 *
	 * @return The key bindings.
	 */
	public static synchronized KeyBindings getInstance() {
		if (instance == null)
			instance = new KeyBindings();
		return instance;
	}

	/**
	 * Returns the key bound to an action.
	 *
	 * @param action
	 *            Action to look up.
	 * @return Key code, as in KeyEvent.VK_*.
	 */
	public int getKey(final Action action) {
		return this.keys.get(action);
	}

	/**
	 * Returns the key that uses the item in a slot.
	 *
	 * @param slot
	 *            Slot index, starting at 0.
	 * @return Key code, or -1 if that slot has no key.
	 */
	public int getItemKey(final int slot) {
		if (slot < 0 || slot >= ITEMS.length)
			return -1;
		return getKey(ITEMS[slot]);
	}

	/**
	 * Returns the action that currently uses a key.
	 *
	 * @param keyCode
	 *            Key code to look up.
	 * @return The action, or null if no action uses the key.
	 */
	public Action findAction(final int keyCode) {
		for (Map.Entry<Action, Integer> entry : this.keys.entrySet())
			if (entry.getValue() == keyCode)
				return entry.getKey();
		return null;
	}

	/**
	 * Tells whether a key may be bound at all. ESC is reserved for going
	 * back and cancelling.
	 *
	 * @param keyCode
	 *            Key code to check.
	 * @return True if the key can be bound.
	 */
	public static boolean isAllowed(final int keyCode) {
		return keyCode > 0 && keyCode <= MAX_KEY_CODE
				&& keyCode != KeyEvent.VK_ESCAPE;
	}

	/**
	 * Tells whether a key may be bound to a specific action. On top of
	 * isAllowed, Pause can't use a key the pause menu needs.
	 *
	 * @param action
	 *            Action to bind.
	 * @param keyCode
	 *            Key code to check.
	 * @return True if the key can be bound to this action.
	 */
	public static boolean isAllowedFor(final Action action,
			final int keyCode) {
		if (!isAllowed(keyCode))
			return false;
		if (action == Action.PAUSE)
			for (int menuKey : PAUSE_MENU_KEYS)
				if (keyCode == menuKey)
					return false;
		return true;
	}

	/**
	 * Binds a key to an action and saves the bindings. A key that another
	 * action already uses is rejected, and nothing changes.
	 *
	 * @param action
	 *            Action to change.
	 * @param keyCode
	 *            New key, as in KeyEvent.VK_*.
	 * @return Null if the key was bound, or the action that already uses
	 *         the key.
	 * @throws IllegalArgumentException
	 *             If the key is not allowed (see isAllowedFor).
	 */
	public Action bind(final Action action, final int keyCode) {
		if (!isAllowedFor(action, keyCode))
			throw new IllegalArgumentException("Key " + keyCode
					+ " cannot be bound.");
		Action owner = findAction(keyCode);
		if (owner != null && owner != action)
			return owner;
		this.keys.put(action, keyCode);
		save();
		return null;
	}

	/** Puts every action back on its default key, without saving. */
	public void resetToDefaults() {
		for (Action action : Action.values())
			this.keys.put(action, action.getDefaultKey());
	}

	/** Saves the current bindings. Failure is logged, not thrown. */
	public void save() {
		try {
			Core.getFileManager().saveKeyBindings(this.keys);
		} catch (IOException e) {
			this.logger.warning("Could not save key bindings.");
		}
	}

	/**
	 * Returns the on-screen name of the key bound to an action.
	 *
	 * @param action
	 *            Action to look up.
	 * @return Short key name, e.g. "Left", "Space", "P".
	 */
	public String keyName(final Action action) {
		return nameOf(getKey(action));
	}

	/**
	 * Returns a short on-screen name for a key. Uses fixed English names
	 * instead of KeyEvent.getKeyText, which follows the system language
	 * and could return text the game font cannot draw.
	 *
	 * @param keyCode
	 *            Key code, as in KeyEvent.VK_*.
	 * @return Short key name.
	 */
	public static String nameOf(final int keyCode) {
		if ((keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z)
				|| (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9))
			return String.valueOf((char) keyCode);
		if (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9)
			return "Num " + (keyCode - KeyEvent.VK_NUMPAD0);
		switch (keyCode) {
		case KeyEvent.VK_LEFT:
			return "Left";
		case KeyEvent.VK_RIGHT:
			return "Right";
		case KeyEvent.VK_UP:
			return "Up";
		case KeyEvent.VK_DOWN:
			return "Down";
		case KeyEvent.VK_SPACE:
			return "Space";
		case KeyEvent.VK_ENTER:
			return "Enter";
		case KeyEvent.VK_SHIFT:
			return "Shift";
		case KeyEvent.VK_CONTROL:
			return "Ctrl";
		case KeyEvent.VK_ALT:
			return "Alt";
		case KeyEvent.VK_TAB:
			return "Tab";
		case KeyEvent.VK_BACK_SPACE:
			return "Backspace";
		case KeyEvent.VK_COMMA:
			return ",";
		case KeyEvent.VK_PERIOD:
			return ".";
		case KeyEvent.VK_SLASH:
			return "/";
		case KeyEvent.VK_SEMICOLON:
			return ";";
		default:
			return "Key " + keyCode;
		}
	}

	/**
	 * Checks a full set of bindings: every action present, every key
	 * allowed, and no key used twice.
	 *
	 * @param candidate
	 *            Bindings to check.
	 * @return True if the bindings can be used.
	 */
	private static boolean isValidSet(final Map<Action, Integer> candidate) {
		Set<Integer> used = new HashSet<Integer>();
		for (Action action : Action.values()) {
			Integer key = candidate.get(action);
			if (key == null || !isAllowedFor(action, key) || !used.add(key))
				return false;
		}
		return true;
	}
}
