package kangarko.chatcontrol.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import kangarko.chatcontrol.ChatControl;
import kangarko.chatcontrol.utils.Common;
import kangarko.chatcontrol.utils.Writer;

import org.apache.commons.lang.Validate;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;

public abstract class ConfHelper {

	protected static YamlConfiguration cfg;
	protected static File file;

	private static String pathPrefix = null;
	private static boolean save = false;

	protected ConfHelper() {
	}

	public static void loadAll() throws Exception {		
		File datafolder = ChatControl.instance().getDataFolder();
		File oldconfig = new File(datafolder, "config.yml");

		if (oldconfig.exists()) {
			String version = YamlConfiguration.loadConfiguration(oldconfig).getString("Do_Not_Change_Version_Number");

			Common.LogInFrame(false, "&bDetected old &fconfiguration &bfrom version " + version, "&bThis is &cnot &bcompatible with the new version", "&bEntire folder renamed to ChatControl_Old");
			datafolder.renameTo(new File(datafolder.getParent(), "ChatControl_Old"));
			datafolder.delete();
		}

		// Order matters.
		Settings.load();
		Localization.load();
	}

	protected static void loadValues(Class<?> clazz) throws Exception {
		Objects.requireNonNull(cfg, "YamlConfiguration is null!");

		// The class itself.
		invokeMethods(clazz);

		// All sub-classes.
		for (Class<?> subClazz : clazz.getDeclaredClasses()) {
			invokeMethods(subClazz);

			// And classes in sub-classes.
			for (Class<?> subSubClazz : subClazz.getDeclaredClasses())
				invokeMethods(subSubClazz);
		}

		save();
	}

	private static void invokeMethods(Class<?> clazz) throws Exception {
		for (Method m : clazz.getDeclaredMethods()) {
			int modifier = m.getModifiers();

			if (Modifier.isPrivate(modifier) && Modifier.isStatic(modifier) && m.getReturnType() == Void.TYPE && m.getParameterTypes().length == 0) {
				Validate.isTrue(m.getName().equals("init"), "Method must be called init() not " + m.getName());

				m.setAccessible(true);
				m.invoke(null);
				pathPrefix(null);
			}
		}
	}

	private static void save() throws IOException {
		if (file != null && save) {
			cfg.options().header("!---------------------------------------------------------!\n" +
							"! File automatically updated at " + Common.getFormattedDate() + "\n" +
							"! to plugin version v" + ChatControl.instance().getDescription().getVersion() + "\n" +
							"!---------------------------------------------------------!\n" +
							"! Unfortunatelly due to how Bukkit handles YAML\n"+
							"! configurations, all comments (#) were wiped. \n" +
							"! For reference values and comments visit\n" +
							"! https://github.com/kangarko/chatcontrol\n" +
					"!---------------------------------------------------------!\n");
			cfg.save(file);

			Common.Log("&eSaved updated file: " + file.getName() + " (# Comments removed)");
			save = false;
		}
	}

	protected static void createFileAndLoad(String path, Class<?> loadFrom) throws Exception {
		Objects.requireNonNull(path, "File path cannot be null!");

		file = Writer.Extract(path);

		cfg = new YamlConfiguration();
		cfg.load(file);

		loadValues(loadFrom);
	}

	// --------------- Getters ---------------

	protected static boolean getBoolean(String path, boolean def) {
		path = addPathPrefix(path);
		addDefault(path, def);

		Validate.isTrue(cfg.isBoolean(path), "Malformed config value, expected boolean at: " + path);
		return cfg.getBoolean(path);
	}

	protected static String getString(String path, String def) {
		path = addPathPrefix(path);
		addDefault(path, def);

		Validate.isTrue(cfg.isString(path), "Malformed config value, expected string at: " + path);
		return cfg.getString(path);
	}

	protected static int getInteger(String path, int def) {
		path = addPathPrefix(path);
		addDefault(path, def);

		Validate.isTrue(cfg.isInt(path), "Malformed config value, expected integer at: " + path);
		return cfg.getInt(path);
	}

	protected static double getDouble(String path, double def) {
		path = addPathPrefix(path);
		addDefault(path, def);

		Validate.isTrue(cfg.isDouble(path), "Malformed config value, expected double at: " + path);
		return cfg.getDouble(path);
	}

	protected static HashMap<String, List<String>> getValuesAndList(String path, HashMap<String, List<String>> def) {
		path = addPathPrefix(path);

		// add default
		if (!cfg.isSet(path)) {
			validate(path, def);

			for (String str : def.keySet())
				cfg.set(path + "." + str, def.get(str));
		}

		Validate.isTrue(cfg.isConfigurationSection(path), "Malformed config value, expected configuration section at: " + path);
		HashMap<String, List<String>> keys = new HashMap<>();

		for (String key : cfg.getConfigurationSection(path).getKeys(true)) {
			if (keys.containsKey(key))
				Common.Warn("Duplicate key: " + key + " in " + path);
			keys.put(key, getStringList(path + "." + key, Arrays.asList(""), false));
		}

		return keys;
	}

	protected static HashMap<String, String> getValuesAndKeys(String path, HashMap<String, String> def, boolean deep) {
		path = addPathPrefix(path);

		// add default
		if (!cfg.isSet(path)) {
			validate(path, def);

			for (String str : def.keySet())
				cfg.set(path + "." + str, def.get(str));
		}

		Validate.isTrue(cfg.isConfigurationSection(path), "Malformed config value, expected configuration section at: " + path);
		HashMap<String, String> keys = new HashMap<>();

		for (String key : cfg.getConfigurationSection(path).getKeys(deep)) {
			if (keys.containsKey(key))
				Common.Warn("Duplicate key: " + key + " in " + path);
			keys.put(key, getString(path + "." + key, ""));
		}

		return keys;
	}

	protected static List<String> getStringList(String path, List<String> def, boolean addPathPrefix) {
		if (addPathPrefix)
			path = addPathPrefix(path);
		addDefault(path, def);

		Validate.isTrue(cfg.isList(path), "Malformed config value, expected list at: " + path);
		return cfg.getStringList(path);
	}

	protected static List<String> getStringList(String path, List<String> def) {
		return getStringList(path, def, true);
	}

	protected static ChatMessage getMessage(String path, ChatMessage def) {
		return new ChatMessage(getString(path, def.getMessage()));
	}

	private static <T> void validate(String path, T def) {
		if (file == null)
			throw new IllegalStateException("Inbuilt config doesn't contain " + def.getClass().getTypeName() + " at \"" + path + "\". Is it outdated?");

		save = true;
		Common.Log("&fUpdating " + file.getName() + " with &b\'&f" + path + "&b\' &f-> &b\'&f" + def + "&b\'&r");
	}

	// --------------- Lazy helpers ---------------

	private static <T> void addDefault(String path, T def) {
		if (!cfg.isSet(path)) {
			validate(path, def);
			cfg.set(path, def);
		}
	}

	private static String addPathPrefix(String path) {
		return pathPrefix != null ? pathPrefix + "." + path : path;
	}

	protected static void pathPrefix(String pathPrefix) {
		ConfHelper.pathPrefix = pathPrefix;
	}

	// --------------- Classes ---------------

	public static class ChatMessage {
		private final Type type;
		private final String message;

		public ChatMessage(String message) {
			type = Type.fromValue(message);
			this.message = message;
		}

		public ChatMessage(Type type) {
			Validate.isTrue(type != Type.CUSTOM, "Type cannot be custom.");

			this.type = type;
			message = type == Type.DEFAULT ? "default" : type == Type.HIDDEN ? "hidden" : null;
		}

		public Type getType() {
			return type;
		}

		public String getMessage() {
			Objects.requireNonNull(message, "Message cannot be null!");
			return message;
		}

		public enum Type {
			DEFAULT,
			HIDDEN,
			CUSTOM;

			public static Type fromValue(String raw) {
				switch (raw.toLowerCase()) {
					case "default":
					case "def":
					case "vanilla":
						return DEFAULT;
					case "none":
					case "hide":
					case "hidden":
						return HIDDEN;
					default:
						return CUSTOM;
				}
			}
		}
	}

	public static class CasusHelper {
		private final String nominativPl; // 2 to 4 seconds (slovak case - sekundy)
		private final String akuzativSg; // 1 second (slovak case - sekundu) - not in english
		private final String genitivePl; // 5 and more seconds (slovak case - sekund)

		protected CasusHelper(String raw) {
			String[] values = raw.split(", ");

			if (values.length == 2) {
				akuzativSg = values[0];
				nominativPl = values[1];
				genitivePl = nominativPl;
				return;
			}

			if (values.length != 3)
				throw new RuntimeException("Malformed type, use format: 'second, seconds' OR 'sekundu, sekundy, sekund' (if your language has it)");

			akuzativSg = values[0];
			nominativPl = values[1];
			genitivePl = values[2];
		}

		public String formatNumbers(long count) {
			if (count == 1)
				return akuzativSg;
			if (count > 1 && count < 5)
				return nominativPl;

			return genitivePl;
		}
	}

	public static class SoundHelper {
		public final Sound sound;
		public final float volume, pitch;

		public SoundHelper(String raw) {
			String[] values = raw.split(", ");

			if (values.length == 1) {
				sound = Sound.valueOf(values[0].toUpperCase());
				volume = 1F;
				pitch = 1.5F;
				return;
			}

			Validate.isTrue(values.length == 3, "Malformed sound type, use format: 'sound' OR 'sound, volume, pitch'");
			sound = Sound.valueOf(values[0].toUpperCase());
			volume = Float.parseFloat(values[1]);
			pitch = Float.parseFloat(values[2]);
		}
	}

	public static class IllegalLocaleException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static class InBuiltFileMissingException extends Exception {
		private static final long serialVersionUID = 1L;
		public final String file;

		public InBuiltFileMissingException(String msg, String file) {
			super(msg.replace("%file", file));
			this.file = file;
		}
	}
}