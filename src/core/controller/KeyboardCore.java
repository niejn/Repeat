package core.controller;

import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import core.config.Config;
import core.userDefinedTask.Tools;
import utilities.Function;
import utilities.OSIdentifier;

/**
 * Class to provide API to control keyboard.
 * @author HP Truong
 *
 */
public class KeyboardCore {

	// In OSX we somehow need to have a delay between pressing combinations.
	private static int OSX_KEY_FLAG_DELAY_MS = 70;
	// In non OSX we need to have a delay after pressing Shift + Insert and re-setting the keyboard.
	private static int NON_OSX_PASTE_DELAY_MS = 70;
	private static final Set<Integer> OSX_FLAG_KEYS;

	private static final HashMap<Character, Function<KeyboardCore, Void>> charShiftType;
	private static final Toolkit toolkit = Toolkit.getDefaultToolkit();

	static {
		OSX_FLAG_KEYS = new HashSet<>();
		OSX_FLAG_KEYS.add(KeyEvent.VK_CONTROL);
		OSX_FLAG_KEYS.add(KeyEvent.VK_ALT);
		OSX_FLAG_KEYS.add(KeyEvent.VK_META);
		OSX_FLAG_KEYS.add(KeyEvent.VK_SHIFT);

		charShiftType = new HashMap<>();

		final int[] keys = new int[] { KeyEvent.VK_1, KeyEvent.VK_2,
				KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6,
				KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0,
				KeyEvent.VK_MINUS, KeyEvent.VK_EQUALS, KeyEvent.VK_COMMA,
				KeyEvent.VK_PERIOD, KeyEvent.VK_SLASH, KeyEvent.VK_BACK_SLASH,
				KeyEvent.VK_OPEN_BRACKET, KeyEvent.VK_CLOSE_BRACKET,
				KeyEvent.VK_SEMICOLON, KeyEvent.VK_QUOTE, KeyEvent.VK_BACK_QUOTE };

		Character[] inputs = new Character[] { '!', '@', '#', '$', '%', '^',
				'&', '*', '(', ')', '_', '+', '<', '>', '?', '|', '{', '}',
				':', '"' , '~'};

		for (int i = 0; i < inputs.length; i++) {
			final int index = i;
			charShiftType.put(inputs[index], new Function<KeyboardCore, Void>() {
				@Override
				public Void apply(KeyboardCore c) {
					c.press(KeyEvent.VK_SHIFT);
					c.press(keys[index]);
					c.release(KeyEvent.VK_SHIFT);
					c.release(keys[index]);
					return null;
				}
			});
		}
	}

	public static final int TYPE_DURATION_MS = 20;
	private final Config config;
	private final Robot controller;

	protected KeyboardCore(Config config, Robot controller) {
		this.config = config;
		this.controller = controller;
	}

	/**
	 * Simulate keyboard type to type out a string. This types upper case letter by using SHIFT + lower case letter.
	 * Almost every typeable character on ANSI keyboard is supported.
	 *
	 * If config use clipboard is enabled, this puts the string into clipboard and uses
	 * SHIFT + INSERT/COMMAND + V to paste the content instead of typing out.
	 * Note that this will preserve the text clipboard.
	 *
	 * @param string string to be typed.
	 */
	public void type(String string) {
		if (config.isUseClipboardToTypeString()) {
			String existing = Tools.getClipboard();

			Tools.setClipboard(string);
			if (OSIdentifier.IS_OSX) {
				combination(KeyEvent.VK_META, KeyEvent.VK_V);
			} else {
				combination(KeyEvent.VK_SHIFT, KeyEvent.VK_INSERT);
				controller.delay(NON_OSX_PASTE_DELAY_MS);
			}

			if (!existing.isEmpty()) {
				Tools.setClipboard(existing);
			}
			return;
		}

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			type(c);
		}
	}

	/**
	 * Type out a series of strings using {@link #type(String)}.
	 * @param strings array of strings to be typed.
	 */
	public void type(String...strings) {
		for (String s : strings) {
			type(s);
		}
	}

	/**
	 * Simulate keyboard type to type out a character. This types upper case letter by using SHIFT + lower case letter.
	 * Almost every typeable character on ANSI keyboard is supported.
	 * @param c character to be typed
	 */
	public void type(char c) {
		if (Character.isAlphabetic(c)) {
			typeAlphabetic(c);
		} else if (charShiftType.containsKey(c)) {
			charShiftType.get(c).apply(this);
		} else if (!typeSpecialChar(c)) {
			typeUnknown(c);
		}
	}

	/**
	 * Simulate keyboard to type out a special character. There are only several special characters supported.
	 * @param c the special character to be typed out
	 * @return if the character is supported by this method
	 */
	private boolean typeSpecialChar(char c) {
		switch (c) {
		case '\t':
			press(KeyEvent.VK_TAB);
			release(KeyEvent.VK_TAB);
			return true;
		case '\n':
			press(KeyEvent.VK_ENTER);
			release(KeyEvent.VK_ENTER);
			return true;
		default:
			return false;
		}
	}

	/**
	 * Type an alphabetic latin character
	 * @param c character to be typed
	 */
	private void typeAlphabetic(char c) {
		if (Character.isUpperCase(c)) {
			press(KeyEvent.VK_SHIFT);
		}
		controller.keyPress(Character.toUpperCase(c));
		controller.keyRelease(Character.toUpperCase(c));

		if (Character.isUpperCase(c)) {
			release(KeyEvent.VK_SHIFT);
		}
	}


	/**
	 * Type a character that is neither an alphabetic character and not in list of known characters (see list defined in this class)
	 * @param c character to be typed
	 */
	private void typeUnknown(char c) {
		int converted = KeyEvent.getExtendedKeyCodeForChar(c);
		controller.keyPress(converted);
		controller.keyRelease(converted);
	}

	/**
	 * Type a key on the keyboard. Key integers are as specified in {@link java.awt.event.KeyEvent} class
	 * @param key integer representing the key as specified in java.awt.events.KeyEvent class
	 * @throws InterruptedException
	 */
	public void type(int key) throws InterruptedException {
		hold(key, TYPE_DURATION_MS);
	}

	/**
	 * Type a series of keys on the keyboard
	 * @param keys array of keys representing the keys as specified in {@link java.awt.event.KeyEvent} class
	 * @throws InterruptedException
	 */
	public void type(int...keys) throws InterruptedException {
		for (int key : keys) {
			type(key);
		}
	}

	/**
	 * Type a sequence of keys sequentially multiple times
	 * @param count number of times to repeat the typing
	 * @param key integers representing the keys as specified in java.awt.events.KeyEvent class
	 * @throws InterruptedException
	 */
	public void repeat(int count, int...keys) throws InterruptedException {
		if (count <= 0) {
			return;
		}

		for (int i = 0; i < count; i++) {
			type(keys);
		}
	}

	/**
	 * Type a key multiple times
	 * @deprecated use {@link #repeat(int, int...)} instead
	 * @param key integer representing the key as specified in java.awt.events.KeyEvent class
	 * @param count number of times to repeat the typing
	 * @throws InterruptedException
	 */
	@Deprecated
	public void typeRepeat(int key, int count) throws InterruptedException {
		if (count <= 0) {
			return;
		}

		for (int i = 0; i < count; i++) {
			type(key);
		}
	}

	/**
	 * Type a combination of keys. E.g. control + C, control + alt + delete
	 * @param keys the array of keys that form the combination in the order. Key integers are as specified in {@link java.awt.event.KeyEvent} class
	 */
	public void combination(int...keys) {
		press(keys);

		for (int i = keys.length - 1; i >= 0; i--) {
			release(keys[i]);
		}
	}

	/**
	 * Hold a key for a certain duration
	 * @param key the integer representing the key to be held. See {@link java.awt.event.KeyEvent} class for these integers
	 * @param duration duration to hold key in milliseconds
	 * @throws InterruptedException
	 */
	public void hold(int key, int duration) throws InterruptedException {
		press(key);

		if (duration >= 0) {
			Thread.sleep(duration);
		}

		release(key);
	}

	/**
	 * Press a key. The keys are held down after the method finishes.
	 * @param key the integer representing the key to be pressed. See {@link java.awt.event.KeyEvent} class for these integers
	 */
	public void press(int key) {
		controller.keyPress(key);
		if (OSIdentifier.IS_OSX && OSX_FLAG_KEYS.contains(key)) {
			controller.delay(OSX_KEY_FLAG_DELAY_MS);
		}
	}

	/**
	 * Press a series of key. The key is held down after the method finishes.
	 * @param keys the array of keys to be pressed. See {@link java.awt.event.KeyEvent} class for these integers
	 */
	public void press(int...keys) {
		for (int key : keys) {
			press(key);
		}
	}

	/**
	 * Release a key
	 * @param key key the integer representing the key to be released. See {@link java.awt.event.KeyEvent} class for these integers
	 */
	public void release(int key) {
		controller.keyRelease(key);
		if (OSIdentifier.IS_OSX && OSX_FLAG_KEYS.contains(key)) {
			controller.delay(OSX_KEY_FLAG_DELAY_MS);
		}
	}

	/**
	 * Release a series of key
	 * @param keys the array of keys to be released. See {@link java.awt.event.KeyEvent} class for these integers
	 */
	public void release(int...keys) {
		for (int key : keys) {
			release(key);
		}
	}

	/**
	 * Check if a key is on (in locked state).
	 * @param key key to check if on E.g. VK_CAPS_LOCk, VK_NUM_LOCK
	 * @return if key locking state is on
	 */
	public boolean isLocked(int key) {
		return toolkit.getLockingKeyState(key);
	}
}
