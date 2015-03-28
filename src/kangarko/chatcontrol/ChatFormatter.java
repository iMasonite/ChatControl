package kangarko.chatcontrol;

import kangarko.chatcontrol.hooks.MultiverseHook;
import kangarko.chatcontrol.hooks.SimpleClansHook;
import kangarko.chatcontrol.hooks.TownyHook;
import kangarko.chatcontrol.model.Settings;
import kangarko.chatcontrol.utils.Common;
import kangarko.chatcontrol.utils.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatFormatter implements Listener {

	private final Pattern COLOR_REGEX = Pattern.compile("(?i)&([0-9A-F])");
	private final Pattern MAGIC_REGEN = Pattern.compile("(?i)&([K])");
	private final Pattern BOLD_REGEX = Pattern.compile("(?i)&([L])");
	private final Pattern STRIKETHROUGH_REGEX = Pattern.compile("(?i)&([M])");
	private final Pattern UNDERLINE_REGEX = Pattern.compile("(?i)&([N])");
	private final Pattern ITALIC_REGEX = Pattern.compile("(?i)&([O])");
	private final Pattern RESET_REGEX = Pattern.compile("(?i)&([R])");

	private MultiverseHook mvHook;
	private SimpleClansHook clansHook;
	private TownyHook townyHook;

	public ChatFormatter() {
		if (doesPluginExist("Multiverse-Core", "World Alias"))
			mvHook = new MultiverseHook();

        if (doesPluginExist("SimpleClans"))
            clansHook = new SimpleClansHook();

		if (doesPluginExist("Towny", "Tested on v0.8x"))
			townyHook = new TownyHook();
	}

	@EventHandler(ignoreCancelled = true)
	public void onChatFormat(AsyncPlayerChatEvent e) {
		Player pl = e.getPlayer();
		String msg = e.getMessage();

		String format = Settings.Chat.Formatter.FORMAT;
		boolean rangedChat = Settings.Chat.Formatter.RANGED_MODE;

		if (rangedChat && msg.startsWith("!") && Common.hasPerm(pl, Permissions.Formatter.GLOBAL_CHAT)) {
			rangedChat = false;
			msg = msg.substring(1);

			format = Settings.Chat.Formatter.GLOBAL_FORMAT;
		}

		format = formatColor(format);

		msg = formatColor(msg, pl);

		format = format.replace("%message", "%2$s").replace("%displayname", "%1$s");
		format = replacePlayerVariables(pl, format);
		format = replaceTime(format);

		e.setFormat(format);
		e.setMessage(msg);

		if (rangedChat) {
			e.getRecipients().clear();
			e.getRecipients().addAll(getLocalRecipients(pl, format, Settings.Chat.Formatter.RANGE));
		}

		// experiment start
		/*System.out.println("PREFIX: " + ChatControl.instance().vault.getPlayerPrefix(pl));

		String vault_prefix = ChatControl.instance().vault.getPlayerPrefix(pl);
		String vault_suffix = ChatControl.instance().vault.getPlayerSuffix(pl);

		for (Player online : e.getRecipients()) {
			new Experimental.JsonBuilder(/*msgFormat.replace("%1$s", "").replace("%2$s", msg))
			.add(vault_prefix)
			.setHoverAction(HoverAction.SHOW_TEXT, " &7An Operator is the highest rank, \n &7which manages things around \n &7the server and its players. ")

			.add(Common.lastColor(vault_prefix) + pl.getName())
			.setHoverAction(HoverAction.SHOW_TEXT, "&7Message Issued: &b" + Common.getFormattedDate() + "\n&7Click to send a PM.")
			.setClickAction(ClickAction.SUGGEST_COMMAND, "/tell " + pl.getName() + " ")

			.add("&7: " + vault_suffix + msg)
			.send(online);
		}

		e.getRecipients().clear();*/
		// experiment end
	}

	public String replacePlayerVariables(Player pl, String format) {
		String world = pl.getWorld().getName();

		if (ChatControl.instance().authMe != null)
			format = format.replace("%countrycode", ChatControl.instance().authMe.getCountryCode(pl)).replace("%countryname", ChatControl.instance().authMe.getCountryName(pl));

		return format
				.replace("%pl_prefix", formatColor(ChatControl.instance().vault.getPlayerPrefix(pl)))
				.replace("%pl_suffix", formatColor(ChatControl.instance().vault.getPlayerSuffix(pl)))
				.replace("%world", getWorldAlias(world))
				.replace("%health", formatHealth(pl) + ChatColor.RESET)
				.replace("%player", pl.getName())
				.replace("%town", getTown(pl))
				.replace("%nation", getNation(pl))
				.replace("%clan", getClanTag(pl));
	}

	private List<Player> getLocalRecipients(Player pl, String message, double range) {
		List<Player> recipients = new LinkedList<Player>();
		try {
			Location playerLocation = pl.getLocation();
			double squaredDistance = Math.pow(range, 2.0D);

			for (Player recipient : Bukkit.getOnlinePlayers())
				if (recipient.getWorld().equals(pl.getWorld()))
					if (playerLocation.distanceSquared(recipient.getLocation()) <= squaredDistance || Common.hasPerm(pl, Permissions.Formatter.OVERRIDE_RANGED))
						recipients.add(recipient);

			return recipients;
		} catch (ArrayIndexOutOfBoundsException ex) {
			Common.Debug("(Range Chat) Got " + ex.getMessage() + ", trying backup.");

			if (Common.hasPerm(pl, Permissions.Formatter.OVERRIDE_RANGED)) {
				for (Player recipient : Bukkit.getOnlinePlayers())
					if (recipient.getWorld().equals(pl.getWorld()))
						recipients.add(recipient);
				return recipients;
			}

			for (Entity en : pl.getNearbyEntities(range, range, range))
				if (en.getType() == EntityType.PLAYER)
					recipients.add((Player) en);

			return recipients;
		}
	}

	private String replaceTime(String msg) {
		Calendar c = Calendar.getInstance();

		if (msg.contains("%h"))
			msg = msg.replace("%h", String.format("%02d", c.get(Calendar.HOUR)));

		if (msg.contains("%H"))
			msg = msg.replace("%H", String.format("%02d", c.get(Calendar.HOUR_OF_DAY)));

		if (msg.contains("%g"))
			msg = msg.replace("%g", Integer.toString(c.get(Calendar.HOUR)));

		if (msg.contains("%G"))
			msg = msg.replace("%G", Integer.toString(c.get(Calendar.HOUR_OF_DAY)));

		if (msg.contains("%i"))
			msg = msg.replace("%i", String.format("%02d", c.get(Calendar.MINUTE)));

		if (msg.contains("%s"))
			msg = msg.replace("%s", String.format("%02d", c.get(Calendar.SECOND)));

		if (msg.contains("%a"))
			msg = msg.replace("%a", c.get(Calendar.AM_PM) == 0 ? "am" : "pm");

		if (msg.contains("%A"))
			msg = msg.replace("%A", c.get(Calendar.AM_PM) == 0 ? "AM" : "PM");

		return msg;
	}

	private String formatColor(String string) {
		if (string == null)
			return "";

		return Common.colorize(string);
	}

	private String formatColor(String string, Player pl) {
		if (string == null)
			return "";

		String str = string;
		if (Common.hasPerm(pl, Permissions.Formatter.COLOR))
			str = COLOR_REGEX.matcher(str).replaceAll("\u00A7$1");

		if (Common.hasPerm(pl, Permissions.Formatter.MAGIC))
			str = MAGIC_REGEN.matcher(str).replaceAll("\u00A7$1");

		if (Common.hasPerm(pl, Permissions.Formatter.BOLD))
			str = BOLD_REGEX.matcher(str).replaceAll("\u00A7$1");

		if (Common.hasPerm(pl, Permissions.Formatter.STRIKETHROUGH))
			str = STRIKETHROUGH_REGEX.matcher(str).replaceAll("\u00A7$1");

		if (Common.hasPerm(pl, Permissions.Formatter.UNDERLINE))
			str = UNDERLINE_REGEX.matcher(str).replaceAll("\u00A7$1");

		if (Common.hasPerm(pl, Permissions.Formatter.ITALIC))
			str = ITALIC_REGEX.matcher(str).replaceAll("\u00A7$1");

		str = RESET_REGEX.matcher(str).replaceAll("\u00A7$1");
		return str;
	}

	private String formatHealth(Player pl) {
		int health = (int) pl.getHealth();

		if (health > 10)
			return ChatColor.DARK_GREEN + "" + health;
		if (health > 5)
			return ChatColor.GOLD + "" + health;
		return ChatColor.RED + "" + health;
	}
	
	private boolean doesPluginExist(String plugin) {
		return doesPluginExist(plugin, null);
	}
	
	private boolean doesPluginExist(String plugin, String message) {
		if (Bukkit.getPluginManager().getPlugin(plugin) != null) {
			Common.Log("&f" + plugin + " integration enabled!" + (message == null ? "" : " (" + message + ")"));
			return true;
		}
		return false;
	}

	private String getWorldAlias(String world) {
		if (mvHook == null)
			return world;

		return mvHook.getColoredAlias(world);
	}

	private String getNation(Player pl) {
		if (townyHook == null)
			return "";

		return townyHook.getNation(pl);
	}

	private String getTown(Player pl) {
		if (townyHook == null)
			return "";

		return townyHook.getTownName(pl);
	}

	private String getClanTag(Player pl) {
		if (clansHook == null)
			return "";

		return clansHook.getClanTag(pl);
	}
}

/*class Experimental {

	enum ClickAction {
		RUN_COMMAND,
		SUGGEST_COMMAND;
	}

	enum HoverAction {
		SHOW_TEXT,
	}

	@SuppressWarnings("unchecked")
	public static class JsonBuilder {

		private JSONObject json = new JSONObject();

		public JsonBuilder() {
			this("");
		}

		public JsonBuilder(String message) {
			JSONArray array = new JSONArray();
			JSONObject extra = new JSONObject();

			extra.put("text", message);
			array.add(extra);

			json.put("extra", array);
			json.put("text", "");
		}

		public JsonBuilder add(String message) {
			JSONObject extra = new JSONObject();

			extra.put("text", message);

			putNewMapping(extra);

			return this;
		}

		public JsonBuilder setClickAction(ClickAction action, String value) {
			JSONObject event = new JSONObject();

			event.put("action", action.toString().toLowerCase());
			event.put("value", value);

			insertToLastMapping("clickEvent", event);

			return this;
		}

		public JsonBuilder setHoverAction(HoverAction action, String value) {
			JSONObject event = new JSONObject();

			event.put("action", action.toString().toLowerCase());
			event.put("value", value);

			insertToLastMapping("hoverEvent", event);

			return this;
		}

		// ------------- Helpers

		private void insertToLastMapping(String key, Object obj) {
			JSONArray array = (JSONArray) json.get("extra");
			JSONObject last = (JSONObject) array.get(array.size() - 1);

			last.put(key, obj);
			json.put("extra", array);
		}

		private void putNewMapping(Object obj) {
			JSONArray array = (JSONArray) json.get("extra");

			array.add(obj);
			json.put("extra", array);
		}

		// ------------- Final methods

		public String toJSONString() {
			return json.toJSONString();
		}

		public void send(Player badass) {
			try {
				System.out.println("Sending: " + json.toJSONString());

				IChatBaseComponent comp = ChatSerializer.a( Common.colorize(json.toJSONString()) );
				PacketPlayOutChat packet = new PacketPlayOutChat(comp);

				((CraftPlayer) badass).getHandle().playerConnection.sendPacket(packet);
			} catch (Exception ex) {
				Common.tell(badass, "&cSeems like an error occured, hahaha");
				ex.printStackTrace();
			}
		}
	}
}*/