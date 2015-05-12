package kangarko.chatcontrol.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import kangarko.chatcontrol.config.ConfHelper.ChatMessage.Type;
import kangarko.chatcontrol.utils.Common;

@SuppressWarnings("unused")
public class Settings extends ConfHelper {

	protected static void load() throws Exception {
		createFileAndLoad("settings.yml", Settings.class);
	}

	public static class Packets {
		
		public static boolean ENABLED;
		
		private static void init() {
			pathPrefix("Packets");
			
			ENABLED = getBoolean("Enabled", true);
		}
		
		public static class TabComplete {
			public static boolean DISABLE, DISABLE_ONLY_IN_CMDS, ALLOW_IF_SPACE;
			public static int IGNORE_ABOVE_LENGTH;

			private static void init() {
				pathPrefix("Packets.Tab_Complete");
				
				DISABLE = getBoolean("Disable", true);
				DISABLE_ONLY_IN_CMDS = getBoolean("Disable_Only_In_Commands", true);
				ALLOW_IF_SPACE = getBoolean("Allow_When_Message_Has_Space", true);
				IGNORE_ABOVE_LENGTH = getInteger("Allow_When_Length_Above", 0);
			}
		}
	}

	public static class SoundNotify {
		public static boolean ENABLED;
		public static boolean ONLY_WHEN_AFK;
		public static HashSet<String> ENABLED_IN_COMMANDS;
		public static SoundHelper SOUND;
		public static String CHAT_PREFIX;

		private static void init() {
			pathPrefix("Sound_Notify");

			ENABLED = getBoolean("Enabled", true);
			ONLY_WHEN_AFK = getBoolean("Notify_Only_When_Afk", true);
			CHAT_PREFIX = getString("Notify_Only_If_Prefixed_With", "@");
			SOUND = new SoundHelper(getString("Sound", "CHICKEN_EGG_POP, 1F, 1.5F"));
			ENABLED_IN_COMMANDS = new HashSet<>(getStringList("Enabled_In_Commands", Arrays.asList("msg", "tell", "t", "w", "r")));
		}
	}

	public static class AntiSpam {
		public static class Messages {
			public static int DELAY;
			public static int SIMILARITY;

			private static void init() {
				pathPrefix("Anti_Spam.Chat");

				DELAY = getInteger("Delay_Between_Messages", 1);
				SIMILARITY = getInteger("Similar_Percentage_Block", 80);
			}
		}

		public static class Commands {
			public static HashSet<String> WHITELIST_DELAY;
			public static HashSet<String> WHITELIST_SIMILARITY;

			public static int DELAY;
			public static int SIMILARITY;

			private static void init() {
				pathPrefix("Anti_Spam.Commands");

				DELAY = getInteger("Delay_Between_Commands", 2);
				SIMILARITY = getInteger("Similar_Percentage_Block", 80);
				WHITELIST_DELAY = new HashSet<>(getStringList("Whitelist_Delay", Arrays.asList("spawn", "home")));
				WHITELIST_SIMILARITY = new HashSet<>(getStringList("Whitelist_Similarity", Arrays.asList("tell", "pm", "t", "w", "r")));
			}
		}

		public static boolean BLOCK_CHAT_UNTIL_MOVED;
		public static boolean STRIP_SPECIAL_CHARS;
		public static boolean STRIP_DUPLICATE_CHARS;
		public static boolean IGNORE_FIRST_ARGUMENTS_IN_CMDS;

		private static void init() {
			pathPrefix("Anti_Spam");

			BLOCK_CHAT_UNTIL_MOVED = getBoolean("Block_Chat_Until_Moved", true);

			pathPrefix("Anti_Spam.Similarity_Check");		

			STRIP_SPECIAL_CHARS = getBoolean("Ignore_Special_Characters", true);
			STRIP_DUPLICATE_CHARS = getBoolean("Ignore_Duplicate_Characters", false);
			STRIP_DUPLICATE_CHARS = getBoolean("Ignore_First_Arguments_In_Commands", true);
		}
	}

	public static class Chat {
		public static class Formatter {
			public static boolean ENABLED;
			public static boolean RANGED_MODE;
			public static String FORMAT;
			public static String GLOBAL_FORMAT;
			public static String SPY_FORMAT;
			public static double RANGE;

			private static void init() {
				pathPrefix("Chat.Formatter");

				ENABLED = getBoolean("Enabled", false);
				FORMAT = getString("Message_Format", "%pl_prefix%player:%pl_suffix %message");
				GLOBAL_FORMAT = getString("Global_Message_Format", "&8[GLOBAL]&f %pl_prefix%player:%pl_suffix %message");
				SPY_FORMAT = getString("Spy_Message_Format", "&8SPY: [%world&8]&f %pl_prefix%player:%pl_suffix %message");
				RANGED_MODE = getBoolean("Ranged_Mode", false);
				RANGE = getDouble("Range", 100);
			}
		}

		public static class Grammar {
			public static boolean INSERT_DOT;
			public static int INSERT_DOT_MSG_LENGTH;

			public static boolean CAPITALIZE;
			public static int CAPITALIZE_MSG_LENGTH;

			private static void init() {
				pathPrefix("Chat.Grammar.Insert_Dot");

				INSERT_DOT = getBoolean("Enabled", true);
				INSERT_DOT_MSG_LENGTH = getInteger("Min_Message_Length", 5);

				pathPrefix("Chat.Grammar.Capitalize");

				CAPITALIZE = getBoolean("Enabled", true);
				CAPITALIZE_MSG_LENGTH = getInteger("Min_Message_Length", 5);
			}
		}
	}

	public static class Messages {
		public static ChatMessage JOIN, QUIT, KICK;

		public static boolean QUIT_ONLY_WHEN_LOGGED;
		
		public static boolean TIMED_ENABLED;
		public static boolean TIMED_RANDOM_ORDER;
		public static boolean TIMED_RANDOM_NO_REPEAT;
		public static String TIMED_PREFIX;
		public static String TIMED_SUFFIX;
		public static int TIMED_DELAY_SECONDS;

		public static HashMap<String, List<String>> TIMED;

		private static void init() {
			pathPrefix("Messages");

			JOIN = getMessage("Join", new ChatMessage(Type.DEFAULT));
			QUIT = getMessage("Quit", new ChatMessage(Type.DEFAULT));
			KICK = getMessage("Kick", new ChatMessage(Type.DEFAULT));

			QUIT_ONLY_WHEN_LOGGED = getBoolean("Show_Quit_Only_When_Logged", true);
			
			pathPrefix("Messages.Timed");

			TIMED_ENABLED = getBoolean("Enabled", false);
			TIMED_RANDOM_ORDER = getBoolean("Random_Order", false);
			TIMED_RANDOM_NO_REPEAT = getBoolean("Random_No_Repeat", true);
			TIMED_PREFIX = getString("Prefix", "&8[&2Tip&8]&2");
			TIMED_SUFFIX = getString("Suffix", "");
			TIMED_DELAY_SECONDS = getInteger("Delay_Seconds", 180);

			HashMap<String, List<String>> timedDef = new HashMap<>();
			timedDef.put("global", Arrays.asList("Hey, %player, did you know that this server is running ChatControl?", "Visit developer website: &awww.rushmine.6f.sk"));
			timedDef.put("hardcore", Arrays.asList("Grief is not permitted what-so-ever and every griefer will be banned.", "Can you survive the night on %world world?"));
			timedDef.put("hardcore_nether", Arrays.asList("incluceFrom hardcore"));
			timedDef.put("creative", Arrays.asList("excludeGlobal", "Welcome on Creative world. Enjoy your gamemode :)"));
			timedDef.put("ignored-world", Arrays.asList("excludeGlobal"));

			TIMED = getValuesAndList("Message_List", timedDef);

			List<String> global = TIMED.get("global");

			for (String world : TIMED.keySet()) {
				List<String> worldMessages = TIMED.get(world);

				if (worldMessages.size() == 0 || world.equalsIgnoreCase("global"))
					continue;

				String firstArgument = worldMessages.get(0);
				
				if (firstArgument.startsWith("includeFrom ")) {
					worldMessages.remove(0);
					
					List<String> worldToInclude = TIMED.get(firstArgument.replace("includeFrom ", ""));
					
					if (worldToInclude == null || worldToInclude.size() == 0)
						Common.Warn("Cannot include messages from " + firstArgument.replace("includeFrom ", " ") + " as the world does not exist or is empty");
					
					worldMessages.addAll(worldToInclude);
				}
				
				if (firstArgument.equalsIgnoreCase("excludeGlobal")) {
					worldMessages.remove(0);
					continue;
				}

				if (global != null && !global.isEmpty())
					worldMessages.addAll(global);
			}
		}
	}

	public static class Mute {
		public static boolean BROADCAST;
		public static boolean SILENT_JOIN, SILENT_QUIT, SILENT_KICK, SILENT_DEATHS;
		public static HashSet<String> DISABLED_CMDS_WHEN_MUTED;

		private static void init() {
			pathPrefix("Mute");

			BROADCAST = getBoolean("Broadcast", true);
			SILENT_JOIN = getBoolean("Silence.Join_Messages", true);
			SILENT_QUIT = getBoolean("Silence.Quit_Messages", true);
			SILENT_KICK = getBoolean("Silence.Kick_Messages", true);
			SILENT_DEATHS = getBoolean("Silence.Death_Messages", true);
			DISABLED_CMDS_WHEN_MUTED = new HashSet<>(getStringList("Disabled_Commands_During_Mute", Arrays.asList("me", "tell", "msg", "r", "w")));
		}
	}

	public static class Clear {
		public static boolean BROADCAST;
		public static int CONSOLE_LINES;
		public static boolean IGNORE_STAFF;

		private static void init() {
			pathPrefix("Clear");

			BROADCAST = getBoolean("Broadcast", true);
			IGNORE_STAFF = getBoolean("Do_Not_Clear_For_Staff", true);
			CONSOLE_LINES = getInteger("Console_Lines_To_Clear", 300);
		}
	}

	public static class AntiCaps {
		public static boolean ENABLED;
		public static boolean WARN_PLAYER;
		public static boolean IGNORE_USERNAMES;
		public static List<String> WHITELIST;

		public static int MIN_MESSAGE_LENGTH;
		public static int MIN_CAPS_PERCENTAGE;
		public static int MIN_CAPS_IN_A_ROW;

		private static void init() {
			pathPrefix("Anti_Caps");
			ENABLED = getBoolean("Enabled", true);
			WARN_PLAYER = getBoolean("Warn_Player", true);
			IGNORE_USERNAMES = getBoolean("Ignore_Usernames", false);

			MIN_MESSAGE_LENGTH = getInteger("Min_Message_Length", 5);
			MIN_CAPS_PERCENTAGE = getInteger("Min_Caps_Percentage", 50);
			MIN_CAPS_IN_A_ROW = getInteger("Min_Caps_In_A_Row", 5);

			WHITELIST = getStringList("Whitelist", Arrays.asList("OMG", "LOL", "WTF", "WOW", "ROFL"));
		}
	}

	public static class AntiBot {
		public static int REJOIN_TIME;

		private static void init() {
			pathPrefix("Anti_Bot");
			REJOIN_TIME = getInteger("Rejoin_Delay_Seconds", 4);
		}
	}

	public static class Signs {
		public static boolean DUPLICATION_CHECK;
		public static boolean DUPLICATION_ALERT_STAFF;
		public static boolean BLOCK_WHEN_VIOLATES_RULE;
		public static boolean DROP_SIGN;

		private static void init() {
			pathPrefix("Signs.Duplication");
			DUPLICATION_CHECK = getBoolean("Deny_Signs_With_Same_Text", false);
			DUPLICATION_ALERT_STAFF = getBoolean("Alert_Staff", true);
			BLOCK_WHEN_VIOLATES_RULE = getBoolean("Block_When_Violates_A_Rule", true);
			DROP_SIGN = getBoolean("Drop_Sign", true);
		}
	}

	public static class Rules {
		public static boolean CHECK_CHAT, CHECK_COMMANDS, CHECK_SIGNS, CHECK_PACKETS;

		private static void init() {
			pathPrefix("Rules");

			CHECK_CHAT = getBoolean("Check_Chat", true);
			CHECK_COMMANDS = getBoolean("Check_Commands", true);
			CHECK_SIGNS = getBoolean("Check_Signs", true);
			CHECK_PACKETS = getBoolean("Check_Packets", true);
		}
	}

	public static class Console {
		public static boolean FILTER_ENABLED;
		public static List<String> FILTER_MESSAGES;

		private static final void init() {
			pathPrefix("Console");

			FILTER_ENABLED = getBoolean("Filter.Enabled", true);
			FILTER_MESSAGES = getStringList("Filter.Filter_Console_Messages", Arrays.asList("Reached end of stream for", "Connection reset", "lost connection"));
		}
	}

	public static class Writer {
		public static boolean ENABLED;
		public static boolean STRIP_COLORS;
		public static HashSet<String> WHITELIST_PLAYERS;
		public static HashSet<String> INCLUDE_COMMANDS;

		private static void init() {
			pathPrefix("Writer");

			ENABLED = getBoolean("Write_Chat_Communication", true);
			STRIP_COLORS = getBoolean("Strip_Colors", true);
			WHITELIST_PLAYERS = new HashSet<>(getStringList("Ignore_Players", Arrays.asList("ignoredAdmin")));
			INCLUDE_COMMANDS = new HashSet<>(getStringList("Write_Commands", Arrays.asList("tell")));
		}
	}

	public static class Updater {
		public static boolean ENABLED;
		public static boolean NOTIFY;
		public static boolean DOWNLOAD;

		private static void init() {
			pathPrefix("Updater");

			ENABLED = getBoolean("Enabled", true);
			NOTIFY = getBoolean("Notify", true);
			DOWNLOAD = getBoolean("Download", true);
		}
	}

	public static boolean OP_HAS_PERMISSIONS;
	public static int REGEX_TIMEOUT;
	public static int MIN_PLAYERS_TO_ENABLE;
	public static String LOCALIZATION_SUFFIX;
	protected static String LOCALIZATION;
	public static boolean VERBOSE_RULES;
	public static int CATCH_LAG;
	public static boolean DEBUG;
	public static int VERSION;

	private static void init() {
		MIN_PLAYERS_TO_ENABLE = getInteger("Minimum_Players_To_Enable_Checks", 0);
		OP_HAS_PERMISSIONS = getBoolean("Op_Has_Permissions", true);
		REGEX_TIMEOUT = getInteger("Regex_Timeout_Milis", 100);
		LOCALIZATION_SUFFIX = getString("Locale", "en");
		LOCALIZATION = "messages_" + LOCALIZATION_SUFFIX + ".yml";
		CATCH_LAG = getInteger("Log_Lag_Over_Milis", 100);
		VERBOSE_RULES = getBoolean("Verbose_Rules", true);
		DEBUG = getBoolean("Debug", false);
		VERSION = getInteger("Version", 1);
	}
}
