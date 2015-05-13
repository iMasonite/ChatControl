package kangarko.chatcontrol;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import kangarko.chatcontrol.config.Settings;
import kangarko.chatcontrol.utils.Common;
import kangarko.chatcontrol.utils.Permissions;
import kangarko.chatcontrol.utils.Writer;

public class ChatFormatter implements Listener {

	private final Pattern COLOR_REGEX = Pattern.compile("(?i)&([0-9A-F])");
	private final Pattern MAGIC_REGEN = Pattern.compile("(?i)&([K])");
	private final Pattern BOLD_REGEX = Pattern.compile("(?i)&([L])");
	private final Pattern STRIKETHROUGH_REGEX = Pattern.compile("(?i)&([M])");
	private final Pattern UNDERLINE_REGEX = Pattern.compile("(?i)&([N])");
	private final Pattern ITALIC_REGEX = Pattern.compile("(?i)&([O])");
	private final Pattern RESET_REGEX = Pattern.compile("(?i)&([R])");

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

		msg = formatColor(msg, pl);

		format = format.replace("%message", "%2$s").replace("%displayname", "%1$s");
		format = replaceAllVariables(pl, format);

		e.setFormat(format);
		e.setMessage(msg);

		if (rangedChat) {
			e.getRecipients().clear();
			e.getRecipients().addAll(getLocalRecipients(pl, msg, Settings.Chat.Formatter.RANGE));
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

	private String replaceAllVariables(Player pl, String format) {
		format = formatColor(format);
		format = replacePlayerVariables(pl, format);
		format = replaceTime(format);

		return format;
	}

	public String replacePlayerVariables(Player pl, String format) {
		String world = pl.getWorld().getName();

		return format
				.replace("%pl_prefix", formatColor(ChatControl.instance().vault.getPlayerPrefix(pl)))
				.replace("%pl_suffix", formatColor(ChatControl.instance().vault.getPlayerSuffix(pl)))
				.replace("%world", world)
				.replace("%health", formatHealth(pl) + ChatColor.RESET)
				.replace("%player", pl.getName());
	}

	private List<Player> getLocalRecipients(Player sender, String message, double range) {
		List<Player> recipients = new LinkedList<Player>();
		try {
			Location playerLocation = sender.getLocation();
			double squaredDistance = Math.pow(range, 2.0D);

			for (Player receiver : Bukkit.getOnlinePlayers()) {
				if (receiver.getWorld().getName().equals(sender.getWorld().getName()))
					if (Common.hasPerm(sender, Permissions.Formatter.OVERRIDE_RANGED_WORLD) || playerLocation.distanceSquared(receiver.getLocation()) <= squaredDistance) {
						recipients.add(receiver);
						continue;
					}
				
				if (Common.hasPerm(receiver, Permissions.Formatter.SPY))
					Common.tell(receiver, replaceAllVariables(sender, Settings.Chat.Formatter.SPY_FORMAT.replace("%message", message).replace("%displayname", sender.getDisplayName())));
			}

			return recipients;
		} catch (ArrayIndexOutOfBoundsException ex) {
			Common.Log("(Range Chat) Got " + ex.getMessage() + ", trying (limited) backup.");
			Writer.Write(Writer.ERROR_FILE_PATH, "Range Chat", sender.getName() + ": \'" + message + "\' Resulted in error: " + ex.getMessage());

			if (Common.hasPerm(sender, Permissions.Formatter.OVERRIDE_RANGED_WORLD)) {
				for (Player recipient : Bukkit.getOnlinePlayers())
					if (recipient.getWorld().equals(sender.getWorld()))
						recipients.add(recipient);

			} else {
				for (Entity en : sender.getNearbyEntities(range, range, range))
					if (en.getType() == EntityType.PLAYER)
						recipients.add((Player) en);
			}
		}

		return recipients;
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

}
