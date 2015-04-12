package kangarko.chatcontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Filter;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import kangarko.chatcontrol.config.ConfHelper;
import kangarko.chatcontrol.config.ConfHelper.IllegalLocaleException;
import kangarko.chatcontrol.config.ConfHelper.InBuiltFileMissingException;
import kangarko.chatcontrol.config.Settings;
import kangarko.chatcontrol.filter.ConsoleFilter;
import kangarko.chatcontrol.filter.Log4jFilter;
import kangarko.chatcontrol.hooks.AuthMeHook;
import kangarko.chatcontrol.hooks.EssentialsHook;
import kangarko.chatcontrol.hooks.ProtocolLibHook;
import kangarko.chatcontrol.hooks.RushCoreHook;
import kangarko.chatcontrol.hooks.VaultHook;
import kangarko.chatcontrol.listener.ChatListener;
import kangarko.chatcontrol.listener.CommandListener;
import kangarko.chatcontrol.listener.PlayerListener;
import kangarko.chatcontrol.rules.ChatCeaser;
import kangarko.chatcontrol.utils.Common;
import kangarko.chatcontrol.utils.LagCatcher;
import kangarko.chatcontrol.utils.Permissions;
import kangarko.chatcontrol.utils.UpdateCheck;

public class ChatControl extends JavaPlugin {

	private static ChatControl instance;

	// Player IP, Time
	public static HashMap<String, Long> ipLastLogin = new HashMap<>();

	// Player Name, Player Cache
	private static HashMap<String, PlayerCache> playerData = new HashMap<>();

	public static boolean muted = false;

	public EssentialsHook ess;
	public VaultHook vault;
	public AuthMeHook authMe;
	public ChatFormatter formatter;	
	public ChatCeaser chatCeaser;

	@Override
	public void onEnable() {
		try {			
			instance = this;

			ConfHelper.loadAll();

			chatCeaser = new ChatCeaser();
			chatCeaser.load();

			for (Player pl : getServer().getOnlinePlayers())
				getDataFor(pl);

			if (doesPluginExist("Essentials"))
				ess = new EssentialsHook();

			if (doesPluginExist("Vault"))
				vault = new VaultHook();

			if (doesPluginExist("AuthMe"))
				authMe = new AuthMeHook();
			
			if (doesPluginExist("RushCore"))
				RushCoreHook.zapnute = true;

			getServer().getPluginManager().registerEvents(new ChatListener(), this);
			getServer().getPluginManager().registerEvents(new PlayerListener(), this);
			getServer().getPluginManager().registerEvents(new CommandListener(), this);

			if (Settings.Console.FILTER_ENABLED)
				try {
					Log4jFilter.init();
					Common.Debug("Console filtering now using Log4j Filter.");
				} catch (NoClassDefFoundError err) {
					Filter filter = new ConsoleFilter();
					for (Plugin plugin : getServer().getPluginManager().getPlugins())
						plugin.getLogger().setFilter(filter);

					Bukkit.getLogger().setFilter(filter);
					Common.Debug("Console filtering initiated (MC 1.6.4 and lower).");
				}

			if (Settings.Packets.ENABLED)
				if (doesPluginExist("ProtocolLib")) {
					ProtocolLibHook.init();
				} else
					Common.LogInFrame(false, "Cannot enable packet features!", "Required plugin missing: ProtocolLib");

			if (Settings.Chat.Formatter.ENABLED)
				if (vault != null) {
					if (doesPluginExist("ChatManager"))
						Common.LogInFrame(true, "Detected &fChatManager&c! Please copy", "settings from it to ChatControl", "and disable the plugin afterwards!");
					else {
						formatter = new ChatFormatter();
						getServer().getPluginManager().registerEvents(formatter, this);
					}
				} else
					Common.LogInFrame(false, "You need Vault to enable ChatFormatter.");

			if (Settings.Messages.TIMED_ENABLED)
				scheduleTimedMessages();

			getCommand("chatcontrol").setExecutor(new CommandsHandler());

			if (Settings.Updater.ENABLED)
				getServer().getScheduler().runTaskAsynchronously(this, new UpdateCheck("https://raw.github.com/kangarko/ChatControl/master/plugin.yml"));

			Common.addLoggingPrefix();
			
			if (Settings.DEBUG)
				Common.Log("Debug was enabled, ready for console spam :P");

		} catch (Throwable t) {
			t.printStackTrace();

			Common.Log("&4!----------------------------------------------!");
			Common.Log(" &cError loading ChatControl, plugin is disabled!");
			Common.Log(" &cRunning on " + getServer().getVersion() + " (" + Common.getServerVersion() + ") and Java " + System.getProperty("java.version"));
			Common.Log("&4!----------------------------------------------!");

			if (t instanceof InvalidConfigurationException) {
				Common.Log(" &cIt seems like your config is not a valid YAML.");
				Common.Log(" &cUse online services like");
				Common.Log(" &chttp://yaml-online-parser.appspot.com/");
				Common.Log(" &cto check for syntax errors!");

			} else if (t instanceof IllegalLocaleException)
				Common.Log(" &cChatControl doesn't have the locale: " + Settings.LOCALIZATION_SUFFIX);
			
			else if (t instanceof InBuiltFileMissingException) {
				Common.Log(" &c" + t.getMessage());
				Common.Log(" &cTo fix it, create a blank file with");
				Common.Log(" &cthe name &f" + ((InBuiltFileMissingException) t).file + " &cin plugin folder.");
				Common.Log(" &cIt will be filled with default values.");
				Common.Log(" &ePlease inform the developer about this error.");

			} else {
				String error = "Unable to get error message, search above.";				
				if (t.getMessage() != null && !t.getMessage().isEmpty() && !t.getMessage().equalsIgnoreCase("null"))
					error = t.getMessage();					
				Common.Log(" &cThe error was: " + error);
			}
			Common.Log("&4!----------------------------------------------!");

			getPluginLoader().disablePlugin(this);
		}
	}

	@Override
	public void onDisable() {
		muted = false;
		playerData.clear();
		ipLastLogin.clear();

		RushCoreHook.zapnute = false;
		UpdateCheck.needsUpdate = false;
		getServer().getScheduler().cancelTasks(this);

		instance = null;
	}

	private void scheduleTimedMessages() {
		final HashMap<String, Integer> broadcasterIndexes = new HashMap<String, Integer>();
		final HashMap<String, List<String>> broadcasterCache = new HashMap<>();
		final Random rand = new Random();

		final HashMap<String, List<String>> timed = Settings.Messages.TIMED;

		if (!Settings.Messages.TIMED_RANDOM_ORDER)
			for (String world : timed.keySet())
				broadcasterIndexes.put(world, 0);

		if (Settings.Messages.TIMED_RANDOM_NO_REPEAT)
			for (String world : timed.keySet())
				broadcasterCache.put(world, new ArrayList<String>(timed.get(world)));

		if (Settings.DEBUG)
			for (String world : timed.keySet()) {
				Common.Debug("&fMessages for: " + world);

				for (String msg : timed.get(world))
					Common.Debug(" - " + msg);
			}

		new BukkitRunnable() {

			@Override
			public void run() {
				LagCatcher.start("timed messages");
				
				for (String world : timed.keySet()) {
					List<String> msgs = timed.get(world); // messages in world					
					
					if (msgs.size() == 0) // no messages there, pass through
						continue;

					String msg;

					if (Settings.Messages.TIMED_RANDOM_ORDER) {
						if (Settings.Messages.TIMED_RANDOM_NO_REPEAT) {
							List<String> worldCache = broadcasterCache.get(world);

							if (worldCache.size() == 0)
								worldCache.addAll(msgs);

							// Pull the message randomly from the cache
							msg = worldCache.remove(rand.nextInt(worldCache.size()));
						} else
							msg = msgs.get(rand.nextInt(msgs.size()));
					} else {
						int last = broadcasterIndexes.get(world);

						if (msgs.size() < last + 1)
							last = 0;

						msg = msgs.get(last);

						broadcasterIndexes.put(world, last + 1);
					}

					if (msg == null)
						continue;
					else {
						String prefix = Settings.Messages.TIMED_PREFIX;
						String suffix = Settings.Messages.TIMED_SUFFIX;
							
						msg = (!prefix.isEmpty() ? prefix + " " : "") + msg + (!suffix.isEmpty() ? " " + suffix : "");
					}
						
					Common.Debug(msg);
					
					if (world.equalsIgnoreCase("global")) {
						for (Player online : getServer().getOnlinePlayers())
							if (!timed.keySet().contains(online.getWorld().getName()) && Common.hasPerm(online, Permissions.VIEW_TIMED_MESSAGES) && RushCoreHook.moznoZobrazitSpravu(online.getName()))
								Common.tell(online, msg.replace("%world", online.getWorld().getName()));

					} else {
						World bukkitworld = getServer().getWorld(world);

						if (bukkitworld == null)
							Common.Warn("World \"" + world + "\" doesn't exist. No timed messages broadcast.");
						else
							for (Player online : bukkitworld.getPlayers())
								if (Common.hasPerm(online, Permissions.VIEW_TIMED_MESSAGES) && RushCoreHook.moznoZobrazitSpravu(online.getName()))
									Common.tell(online, msg.replace("%world", world));
					}
				}
				
				LagCatcher.end("timed messages");
			}
		}.runTaskTimer(this, 20, 20 * Settings.Messages.TIMED_DELAY_SECONDS);
	}

	public boolean doesPluginExist(String pluginName) {
		Plugin plugin = getServer().getPluginManager().getPlugin(pluginName);

		if (plugin != null) {
			Common.Log("&fHooked with: " + pluginName);
			return true;
		}
		return false;
	}

	// ------------------------ static ------------------------

	public static PlayerCache getDataFor(Player pl) {
		return getDataFor(pl.getName());
	}

	public static PlayerCache getDataFor(String pl) {
		PlayerCache cache = playerData.get(pl);

		if (cache == null) {
			cache = new PlayerCache();
			playerData.put(pl, cache);
		}

		return playerData.get(pl);
	}

	public static ChatControl instance() {
		if (instance == null) {
			instance = new ChatControl();

			Common.Warn("ChatControl instance is null! Was the plugin reloaded? Creating new.");
		}

		return instance;
	}
}
