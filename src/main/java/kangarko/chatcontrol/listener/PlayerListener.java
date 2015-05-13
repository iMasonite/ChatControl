package kangarko.chatcontrol.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import kangarko.chatcontrol.ChatControl;
import kangarko.chatcontrol.PlayerCache;
import kangarko.chatcontrol.config.Localization;
import kangarko.chatcontrol.config.Settings;
import kangarko.chatcontrol.utils.Common;
import kangarko.chatcontrol.utils.LagCatcher;
import kangarko.chatcontrol.utils.Permissions;
import kangarko.chatcontrol.utils.UpdateCheck;

public class PlayerListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onPreLogin(AsyncPlayerPreLoginEvent e) {
		long now = System.currentTimeMillis() / 1000L;
		String ip = e.getAddress().getHostAddress();

		if (ChatControl.ipLastLogin.containsKey(ip) && ChatControl.ipLastLogin.get(ip) > Settings.AntiBot.REJOIN_TIME) {
			if (Settings.AntiBot.REJOIN_TIME - (now - ChatControl.ipLastLogin.get(ip)) <= 0)
				return;

			long time = Settings.AntiBot.REJOIN_TIME - (now - ChatControl.ipLastLogin.get(ip));
			String msg = Common.colorize(Localization.ANTIBOT_REJOIN_WAIT_MESSAGE.replace("%time", String.valueOf(time)).replace("%seconds", Localization.Parts.SECONDS.formatNumbers(time)));
			msg.split("\n");

			e.disallow(Result.KICK_OTHER, msg);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		LagCatcher.start("Join event");

		long now = System.currentTimeMillis() / 1000L;

		if (!Common.hasPerm(e.getPlayer(), Permissions.Bypasses.REJOIN))
			ChatControl.ipLastLogin.put(e.getPlayer().getAddress().getAddress().getHostAddress(), now);

		ChatControl.getDataFor(e.getPlayer()).loginLocation = e.getPlayer().getLocation();

		if (e.getPlayer().getName().equals("kangarko"))
			Common.tellLater(e.getPlayer(), 30,
					Common.consoleLine(),
					"&e Na serveri je nainstalovany ChatControl v" + ChatControl.instance().getDescription().getVersion() + "!",
					Common.consoleLine());

		if (UpdateCheck.needsUpdate && Settings.Updater.NOTIFY)
			for (Player pl : Bukkit.getOnlinePlayers())
				if (Common.hasPerm(pl, Permissions.Notify.UPDATE_AVAILABLE)) {
					String sprava = Common.colorize(Localization.UPDATE_AVAILABLE).replace("%current", ChatControl.instance().getDescription().getVersion()).replace("%new", UpdateCheck.newVersion);
					sprava.split("\n");
					Common.tellLater(pl, 4 * 20, sprava);
				}

		LagCatcher.end("Join event");

		if (ChatControl.muted && Settings.Mute.SILENT_JOIN) {
			e.setJoinMessage(null);
			return;
		}

		switch (Settings.Messages.JOIN.getType()) {
			case HIDDEN:
				e.setJoinMessage(null);
				break;
			case CUSTOM:
				e.setJoinMessage(replacePlayerVariables(Settings.Messages.JOIN.getMessage(), e.getPlayer()));
				break;
			default:
				break;
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		if (ChatControl.muted && Settings.Mute.SILENT_QUIT) {
			e.setQuitMessage(null);
			return;
		}

		switch (Settings.Messages.QUIT.getType()) {
			case HIDDEN:
				e.setQuitMessage(null);
				break;
			case CUSTOM:
				e.setQuitMessage(replacePlayerVariables(Settings.Messages.QUIT.getMessage(), e.getPlayer()));
				break;
			default:
				break;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onKick(PlayerKickEvent e) {
		if (ChatControl.muted && Settings.Mute.SILENT_KICK) {
			e.setLeaveMessage(null);
			return;
		}

		switch (Settings.Messages.KICK.getType()) {
			case HIDDEN:
				e.setLeaveMessage(null);
				break;
			case CUSTOM:
				e.setLeaveMessage(replacePlayerVariables(Settings.Messages.KICK.getMessage(), e.getPlayer()));
				break;
			default:
				break;
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		if (ChatControl.muted && Settings.Mute.SILENT_DEATHS)
			e.setDeathMessage(null);
	}

	@EventHandler(ignoreCancelled = true)
	public void onSignChange(SignChangeEvent e) {
		if (Bukkit.getOnlinePlayers().length < Settings.MIN_PLAYERS_TO_ENABLE)
			return;

		LagCatcher.start("Sign event");

		Player pl = e.getPlayer();
		PlayerCache plData = ChatControl.getDataFor(pl);
		String msg = e.getLine(0) + e.getLine(1) + e.getLine(2) + e.getLine(3);

		msg = msg.trim();

		if (Settings.Signs.DUPLICATION_CHECK && plData.lastSignText.equalsIgnoreCase(msg) && !Common.hasPerm(pl, Permissions.Bypasses.SIGN_DUPLICATION)) {
			if (Settings.Signs.DUPLICATION_ALERT_STAFF)
				for (Player online : Bukkit.getOnlinePlayers())
					if (!online.getName().equals(pl.getName()) && Common.hasPerm(online, Permissions.Notify.SIGN_DUPLICATION))
						Common.tell(online, Localization.SIGNS_DUPLICATION_STAFF.replace("%message", msg), pl.getName());

			Common.tell(pl, Localization.SIGNS_DUPLICATION);
			e.setCancelled(true);

			if (Settings.Signs.DROP_SIGN)
				e.getBlock().breakNaturally();	

			LagCatcher.end("Sign event");
			return;
		}

		if (Settings.Rules.CHECK_SIGNS && !Common.hasPerm(e.getPlayer(), Permissions.Bypasses.RULES)) {
			ChatControl.instance().chatCeaser.parseRules(e, pl, msg);

			if (e.isCancelled()) {
				Common.tellLater(pl, 2, Localization.SIGNS_BROKE); // display at the bottom
				e.setCancelled(true);

				if (Settings.Signs.DROP_SIGN)
					e.getBlock().breakNaturally();
			}
		}

		LagCatcher.end("Sign event");
	}

	public String replacePlayerVariables(String msg, Player pl) {
		msg = msg.replace("%player", pl.getName());

		if (ChatControl.instance().formatter != null)
			msg = ChatControl.instance().formatter.replacePlayerVariables(pl, msg);

		return Common.colorize(msg);
	}
}
