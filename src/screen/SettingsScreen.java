package screen;

import engine.Cooldown;
import engine.Core;
import engine.KeyBindings;
import java.awt.event.KeyEvent;
import java.awt.Color;
import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;
import engine.soundEngine.MenuSounds;

/**
 * Implements the settings screen.
 */
public class SettingsScreen extends Screen {
	/** Milliseconds between selection steps. */
	private static final int SELECTION_TIME = 200;
	/** Spacing between menu items. */
	private static final int SPACING = 30;
	/** Cooldown for menu selection and volume changes. */
	private final Cooldown selectionCooldown;
	/** get volume from soundManager */
	private final SoundManager soundManager = SoundManager.getInstance();
	/** Current selected menu item. */
	private int currentMenuItem = 0;
	/** Vertical distance between the BGM and SFX sliders. */
	private static final int SLIDER_ROW_SPACING = 80;
	/** Slider currently being dragged, or -1 when none is active. */
	private int draggingSlider = -1;
	/** Whether the left mouse button was pressed on the previous frame. */
	private boolean mouseWasPressed;

	/** Rows on the main page: BGM slider, SFX slider, Controls. */
	private static final int MAIN_ROWS = 3;
	/** Main-page row that opens the Controls page. */
	private static final int CONTROLS_ROW = 2;
	/** Actions listed on the Controls page, in order. */
	private static final KeyBindings.Action[] ACTIONS =
			KeyBindings.Action.values();
	/** Controls-page row below the actions that resets every key. */
	private static final int RESET_ROW = ACTIONS.length;
	/** Vertical distance between rows on the Controls page. */
	private static final int CONTROL_ROW_SPACING = 30;
	/** How long a Controls-page message stays on screen, in ms. */
	private static final int MESSAGE_TIME = 2000;
	/** Highest key code the screen looks at when waiting for a key. */
	private static final int MAX_KEY_CODE = 255;

	/** Keys the player chose for gameplay (SCRUM-19). */
	private final KeyBindings keyBindings = KeyBindings.getInstance();
	/** Whether the Controls page is shown instead of the main page. */
	private boolean controlsPage;
	/** Selected row on the Controls page: an action, or RESET_ROW. */
	private int controlsSelection;
	/** Action waiting for its new key, or null when not rebinding. */
	private KeyBindings.Action waitingFor;
	/**
	 * Set after a page change or a key choice, so the same press isn't read
	 * twice. Input is ignored until every key is released.
	 */
	private boolean waitForRelease;
	/** Result of the last change on the Controls page. */
	private String message = "";
	/** Time the current message stays on screen. */
	private final Cooldown messageTimer;

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 */
	public SettingsScreen(final int width, final int height, final int fps) {
		super(width, height, fps);
		this.returnCode = 1;
		this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
		this.selectionCooldown.reset();
		this.messageTimer = Core.getCooldown(MESSAGE_TIME);

		this.mouseWasPressed = this.inputManager.isLeftMousePressed();
	}

	/** Returns the current volume as a percentage. */
	private int getVolumePercent(final Sound.Category category) {
		return Math.round(this.soundManager.getVolume(category) * 100f);
	}

	/** Adjusts the selected category by the given percentage points. */
	private void adjustSelectedVolume(final int delta) {
		Sound.Category category = this.currentMenuItem == 0
				? Sound.Category.BGM
				: Sound.Category.SFX;

		int currentPercent = getVolumePercent(category);
		int percent = Math.max(0,
				Math.min(100, currentPercent + delta));

		if (percent != currentPercent)
			MenuSounds.playMove();
		this.soundManager.setVolume(category, percent / 100f);
	}
	/**
	 * Starts the action.
	 *
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();
		return this.returnCode;
	}

	/**
	 * Updates keyboard input, slider dragging and screen drawing.
	 */
	@Override
	protected final void update() {
		super.update();

		boolean mousePressed = this.inputManager.isLeftMousePressed();

		if (this.inputDelay.checkFinished()) {
			if (this.waitForRelease) {
				if (firstKeyDown() < 0)
					this.waitForRelease = false;
			} else if (this.waitingFor != null) {
				updateWaitingForKey();
			} else if (this.controlsPage) {
				updateControlsPage();
			} else {
				updateMainPage(mousePressed);
			}
		}

		this.mouseWasPressed = mousePressed;
		draw();
	}

	/**
	 * Main page: volume sliders and the Controls row. ESC leaves Settings.
	 *
	 * @param mousePressed
	 *            Whether the left mouse button is currently held down.
	 */
	private void updateMainPage(final boolean mousePressed) {
		if (this.inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
			this.draggingSlider = -1;
			this.isRunning = false;
			return;
		}
		updateMouseVolume(mousePressed);

		// Keyboard changes are paused while a slider is being dragged.
		if (this.draggingSlider != -1
				|| !this.selectionCooldown.checkFinished())
			return;

		if (isUpDown()) {
			this.currentMenuItem =
					(this.currentMenuItem + MAIN_ROWS - 1) % MAIN_ROWS;
			this.selectionCooldown.reset();
		} else if (isDownDown()) {
			this.currentMenuItem = (this.currentMenuItem + 1) % MAIN_ROWS;
			this.selectionCooldown.reset();
		} else if (this.currentMenuItem == CONTROLS_ROW) {
			if (isSelectDown()) {
				this.controlsPage = true;
				this.controlsSelection = 0;
				this.message = "";
				this.waitForRelease = true;
			}
		} else if (this.inputManager.isKeyDown(KeyEvent.VK_LEFT)) {
			adjustSelectedVolume(-10);
			this.selectionCooldown.reset();
		} else if (this.inputManager.isKeyDown(KeyEvent.VK_RIGHT)) {
			adjustSelectedVolume(10);
			this.selectionCooldown.reset();
		}
	}

	/**
	 * Controls page: pick an action to rebind, or reset every key. ESC goes
	 * back to the main page.
	 */
	private void updateControlsPage() {
		if (!this.selectionCooldown.checkFinished())
			return;

		if (this.inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
			this.controlsPage = false;
			this.currentMenuItem = CONTROLS_ROW;
			this.waitForRelease = true;
		} else if (isUpDown()) {
			this.controlsSelection =
					(this.controlsSelection + RESET_ROW) % (RESET_ROW + 1);
			this.selectionCooldown.reset();
		} else if (isDownDown()) {
			this.controlsSelection =
					(this.controlsSelection + 1) % (RESET_ROW + 1);
			this.selectionCooldown.reset();
		} else if (isSelectDown()) {
			if (this.controlsSelection == RESET_ROW) {
				this.keyBindings.resetToDefaults();
				this.keyBindings.save();
				showMessage("Keys reset to defaults");
			} else {
				this.waitingFor = ACTIONS[this.controlsSelection];
				this.message = "";
			}
			this.waitForRelease = true;
		}
	}

	/**
	 * Waits for the next key press and binds it to the chosen action. ESC
	 * cancels; a key another action uses is rejected (SCRUM-19).
	 */
	private void updateWaitingForKey() {
		int key = firstKeyDown();
		if (key < 0)
			return;

		KeyBindings.Action action = this.waitingFor;
		this.waitingFor = null;
		this.waitForRelease = true;

		if (key == KeyEvent.VK_ESCAPE) {
			showMessage("Cancelled");
		} else if (!KeyBindings.isAllowedFor(action, key)) {
			showMessage(KeyBindings.nameOf(key) + " can't be used for "
					+ action.getLabel());
		} else {
			KeyBindings.Action owner = this.keyBindings.bind(action, key);
			if (owner != null)
				showMessage("Already used by " + owner.getLabel());
			else
				showMessage(action.getLabel() + " set to "
						+ KeyBindings.nameOf(key));
		}
	}

	/**
	 * Shows a short message under the Controls page rows.
	 *
	 * @param text
	 *            Message to show.
	 */
	private void showMessage(final String text) {
		this.message = text;
		this.messageTimer.reset();
	}

	/**
	 * Returns the first key currently held down.
	 *
	 * @return Key code, or -1 if no key is down.
	 */
	private int firstKeyDown() {
		for (int key = 1; key <= MAX_KEY_CODE; key++)
			if (this.inputManager.isKeyDown(key))
				return key;
		return -1;
	}

	/** @return Whether up (W or the up arrow) is held. */
	private boolean isUpDown() {
		return this.inputManager.isKeyDown(KeyEvent.VK_UP)
				|| this.inputManager.isKeyDown(KeyEvent.VK_W);
	}

	/** @return Whether down (S or the down arrow) is held. */
	private boolean isDownDown() {
		return this.inputManager.isKeyDown(KeyEvent.VK_DOWN)
				|| this.inputManager.isKeyDown(KeyEvent.VK_S);
	}

	/** @return Whether a select key (Space or Enter) is held. */
	private boolean isSelectDown() {
		return this.inputManager.isKeyDown(KeyEvent.VK_SPACE)
				|| this.inputManager.isKeyDown(KeyEvent.VK_ENTER);
	}

	/**
	 * Adjusts volume while dragging and updates mouse hover selection.
	 *
	 * @param mousePressed
	 *            Whether the left mouse button is currently held down.
	 */
	private void updateMouseVolume(final boolean mousePressed) {
		int mouseX = this.inputManager.getMouseX();
		int mouseY = this.inputManager.getMouseY();
		boolean moved = this.inputManager.isMouseMoved();

		// Start dragging only when a new press occurs on a slider.
		if (mousePressed && !this.mouseWasPressed) {
			for (int index = 0; index < 2; index++) {
				boolean onTrack =
						mouseX >= sliderLeft() - 6
								&& mouseX <= sliderLeft() + sliderWidth() + 6
								&& Math.abs(mouseY - sliderY(index)) <= 14;

				if (onTrack) {
					if (this.currentMenuItem != index)
						MenuSounds.playMove();
					this.draggingSlider = index;
					this.currentMenuItem = index;
					break;
				}
			}
		}

		// Keep adjusting the original slider until the button is released.
		if (this.draggingSlider != -1) {
			int percent = Math.round(
					(mouseX - sliderLeft()) * 100f / sliderWidth());

			percent = Math.max(0, Math.min(100, percent));

			Sound.Category category = this.draggingSlider == 0
					? Sound.Category.BGM
					: Sound.Category.SFX;

			if (percent != getVolumePercent(category))
				MenuSounds.playMove();
			this.soundManager.setVolume(category, percent / 100f);

			if (!mousePressed)
				this.draggingSlider = -1;
		} else if (moved && !mousePressed) {
			for (int index = 0; index < 2; index++) {
				boolean insideControl =
						mouseX >= sliderLeft() - 6
								&& mouseX <= sliderLeft() + sliderWidth() + 6
								&& mouseY >= sliderY(index) - 36
								&& mouseY <= sliderY(index) + 14;

				if (insideControl) {
					if (this.currentMenuItem != index) {
						this.currentMenuItem = index;
						MenuSounds.playMove();
					}
					break;
				}
			}
		}
	}

	/**
	 * Returns the horizontal starting position of the volume sliders.
	 *
	 * @return Left edge of the slider tracks.
	 */
	private int sliderLeft() {
		return this.getWidth() / 5;
	}

	/**
	 * Returns the distance between the 0% and 100% positions.
	 *
	 * @return Slider track width in pixels.
	 */
	private int sliderWidth() {
		return Math.max(1, this.getWidth() * 3 / 5);
	}

	/**
	 * Returns the vertical centre of a volume slider.
	 *
	 * @param index
	 *            Slider index: 0 for BGM, 1 for SFX.
	 * @return Vertical centre of the slider track.
	 */
	private int sliderY(final int index) {
		return this.getHeight() / 3 + index * SLIDER_ROW_SPACING;
	}

	/**
	 * Returns the baseline of the Controls row below the sliders.
	 *
	 * @return Vertical position of the text baseline.
	 */
	private int controlsRowY() {
		return sliderY(1) + 2 * SPACING;
	}

	/**
	 * Returns the baseline of a row on the Controls page.
	 *
	 * @param row
	 *            Row index: an action, or RESET_ROW.
	 * @return Vertical position of the text baseline.
	 */
	private int controlRowY(final int row) {
		return this.getHeight() / 4 + CONTROL_ROW_SPACING * row;
	}

	/**
	 * Draws a horizontal volume slider with its label and percentage.
	 *
	 * @param index
	 *            Slider index: 0 for BGM, 1 for SFX.
	 * @param label
	 *            Name displayed above the slider.
	 * @param category
	 *            Sound category whose volume is displayed.
	 */
	private void drawVolumeSlider(final int index, final String label,
								  final Sound.Category category) {

		int left = sliderLeft();
		int width = sliderWidth();
		int centerY = sliderY(index);
		int percent = getVolumePercent(category);
		int thumbX = left + Math.round(width * percent / 100f);

		Color color = this.currentMenuItem == index
				? Color.GREEN
				: Color.WHITE;

		// Draw the label and percentage using the shared selection style.
		this.drawManager.drawMenuRow(
				this, label + "  " + percent + "%",
				centerY - 22, this.currentMenuItem == index);

		// Unfilled track.
		this.drawManager.fillBox(
				left, centerY - 3, width + 1, 6, Color.DARK_GRAY);

		// Filled portion showing the current volume.
		this.drawManager.fillBox(
				left, centerY - 3, thumbX - left, 6, color);

		// Slider handle.
		this.drawManager.fillBox(
				thumbX - 5, centerY - 9, 11, 18, Color.WHITE);
	}

	/**
	 * Draws the current page and its footer.
	 */
	private void draw() {
		this.drawManager.initDrawing(this);
		if (this.controlsPage)
			drawControlsPage();
		else
			drawMainPage();
		this.drawManager.completeDrawing(this);
	}

	/**
	 * Draws the volume sliders and the Controls row.
	 */
	private void drawMainPage() {
		this.drawManager.drawScreenTitle(
				this, MenuItem.SETTINGS.getTitle());

		drawVolumeSlider(0, "BGM", Sound.Category.BGM);
		drawVolumeSlider(1, "SFX", Sound.Category.SFX);

		this.drawManager.drawMenuRow(this, "Controls", controlsRowY(),
				this.currentMenuItem == CONTROLS_ROW);

		this.drawManager.drawKeyHints(this, this.currentMenuItem == CONTROLS_ROW
				? "space to open, esc back"
				: "drag volume, arrows adjust, esc back");
	}

	/**
	 * Draws every action with its key, the reset row and the last message.
	 */
	private void drawControlsPage() {
		this.drawManager.drawScreenTitle(this, "Controls");

		for (int row = 0; row < ACTIONS.length; row++) {
			KeyBindings.Action action = ACTIONS[row];
			String key = this.waitingFor == action
					? "?"
					: this.keyBindings.keyName(action);
			this.drawManager.drawCompactMenuRow(this,
					action.getLabel() + ": " + key, controlRowY(row),
					this.controlsSelection == row);
		}
		this.drawManager.drawCompactMenuRow(this, "Reset to defaults",
				controlRowY(RESET_ROW),
				this.controlsSelection == RESET_ROW);

		if (!this.message.isEmpty() && !this.messageTimer.checkFinished())
			this.drawManager.drawCompactMenuRow(this, this.message,
					controlRowY(RESET_ROW) + CONTROL_ROW_SPACING * 3 / 2,
					false);

		this.drawManager.drawKeyHints(this, this.waitingFor != null
				? "press the new key, esc to cancel"
				: "w+s / arrows to move, space to change, esc back");
	}
}
