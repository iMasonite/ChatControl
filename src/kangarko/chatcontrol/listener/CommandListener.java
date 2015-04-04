package kangarko.chatcontrol.listener;

import kangarko.chatcontrol.ChatControl;
import kangarko.chatcontrol.PlayerCache;
import kangarko.chatcontrol.config.Localization;
import kangarko.chatcontrol.config.Settings;
import kangarko.chatcontrol.hooks.RushCoreHook;
import kangarko.chatcontrol.utils.Common;
import kangarko.chatcontrol.utils.LagCatcher;
import kangarko.chatcontrol.utils.Permissions;
import kangarko.chatcontrol.utils.Writer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
		if (Bukkit.getOnlinePlayers().size() < Settings.MIN_PLAYERS_TO_ENABLE)
			return;

		LagCatcher.start("Command event");

		String command = e.getMessage();
		String[] args = command.split(" ");

		Player pl = e.getPlayer();
		PlayerCache plData = ChatControl.getDataFor(pl);

		muteCheck: if (ChatControl.muted) {
			if (Common.hasPerm(pl, Permissions.Bypasses.MUTE))
				break muteCheck;

			if (Settings.Mute.DISABLED_CMDS_WHEN_MUTED.contains(args[0].replaceFirst("/", ""))) {
				Common.tell(pl, Localization.CANNOT_COMMAND_WHILE_MUTED);
				e.setCancelled(true);
				LagCatcher.end("Command event");
				return;
			}
		}

		long now = System.currentTimeMillis() / 1000L;

		timeCheck: if (now - plData.lastCommandTime < Settings.AntiSpam.Commands.DELAY) {
			if (Common.hasPerm(pl, Permissions.Bypasses.DELAY_COMMANDS))
				break timeCheck;

			if (Settings.AntiSpam.Commands.WHITELIST_DELAY.contains(args[0].replaceFirst("/", "")))
				break timeCheck;

			long time = Settings.AntiSpam.Commands.DELAY - (now - plData.lastCommandTime);

			Common.tell(pl, Localization.COMMAND_WAIT_MESSAGE.replace("%time", String.valueOf(time)).replace("%seconds", Localization.Parts.SECONDS.formatNumbers(time)));
			e.setCancelled(true);
			LagCatcher.end("Command event");
			return;
		} else
			plData.lastCommandTime = now;

		dupeCheck: if (Settings.AntiSpam.Commands.SIMILARITY > 0 && Settings.AntiSpam.Commands.SIMILARITY < 100) {
			String strippedCmd = command;

			// Strip from messages like /tell <player> <msg> the player name, making the check less less annoying.
			if (Settings.AntiSpam.IGNORE_FIRST_ARGUMENTS_IN_CMDS && args.length > 2)
				strippedCmd = strippedCmd.replace(args[0], "").replace(args[1], "");

			strippedCmd = Common.prepareForSimilarityCheck(strippedCmd);

			if (Common.similarity(strippedCmd, plData.lastCommand) > Settings.AntiSpam.Commands.SIMILARITY) {
				if (Common.hasPerm(pl, Permissions.Bypasses.SIMILAR_COMMANDS))
					break dupeCheck;

				if (Settings.AntiSpam.Commands.WHITELIST_SIMILARITY.contains(args[0].replaceFirst("/", "")))
					break dupeCheck;

				Common.tell(pl, Localization.ANTISPAM_SIMILAR_COMMAND);
				e.setCancelled(true);
				LagCatcher.end("Command event");
				return;
			}
			plData.lastCommand = strippedCmd;
		}

		if (Settings.Rules.CHECK_COMMANDS && !Common.hasPerm(e.getPlayer(), Permissions.Bypasses.RULES))
			command = ChatControl.instance().chatCeaser.parseRules(e, pl, command);

		if (e.isCancelled()) { // some of the rule or handler has cancelled it
			LagCatcher.end("Command event");
			return;
		}

		if (!command.equals(e.getMessage()))
			e.setMessage(command);

		if (Settings.Writer.ENABLED && !Settings.Writer.WHITELIST_PLAYERS.contains(pl.getName().toLowerCase()))
			for (String prikaz : Settings.Writer.INCLUDE_COMMANDS)
				if (command.toLowerCase().startsWith("/" + prikaz.toLowerCase()))
					Writer.Write(Writer.CHAT_FILE_PATH, "[CMD] " + pl.getName(), command);

		sound: if (Settings.SoundNotify.ENABLED_IN_COMMANDS.contains(args[0].replaceFirst("/", "")))
			if (ChatControl.instance().ess != null && (command.startsWith("/r ") || command.startsWith("/reply "))) {
				Player reply = ChatControl.instance().ess.getReplyTo(pl.getName());

				if (reply != null && (Common.hasPerm(reply, Permissions.Notify.WHEN_MENTIONED) || RushCoreHook.moznoPrehratZvuk(reply.getName())))
					reply.playSound(reply.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);
			} else if (args.length > 2) {
				Player player = Bukkit.getPlayer(args[1]);
				if (player == null || !player.isOnline() || !RushCoreHook.moznoPrehratZvuk(player.getName()))
					break sound;

				player.playSound(player.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);
			}

			LagCatcher.end("Command event");
	}
}