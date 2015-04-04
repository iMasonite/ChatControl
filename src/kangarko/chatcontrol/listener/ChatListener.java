package kangarko.chatcontrol.listener;

import kangarko.chatcontrol.ChatControl;
import kangarko.chatcontrol.PlayerCache;
import kangarko.chatcontrol.config.Localization;
import kangarko.chatcontrol.config.Settings;
import kangarko.chatcontrol.hooks.RushCoreHook;
import kangarko.chatcontrol.utils.Common;
import kangarko.chatcontrol.utils.Permissions;
import kangarko.chatcontrol.utils.Writer;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		if (Bukkit.getOnlinePlayers().size() < Settings.MIN_PLAYERS_TO_ENABLE)
			return;

		Player pl = e.getPlayer();
		PlayerCache plData = ChatControl.getDataFor(pl);
		String message = e.getMessage();

		if (Settings.AntiSpam.BLOCK_CHAT_UNTIL_MOVED && pl.getLocation().equals(plData.loginLocation))
			if (!Common.hasPerm(pl, Permissions.Bypasses.MOVE)) {
				Common.tell(pl, Localization.CANNOT_CHAT_UNTIL_MOVED);
				e.setCancelled(true);
				return;
			}

		if (ChatControl.muted && !Common.hasPerm(pl, Permissions.Bypasses.MUTE)) {
			Common.tell(pl, Localization.CANNOT_CHAT_WHILE_MUTED);
			e.setCancelled(true);
			return;
		}

		long now = System.currentTimeMillis() / 1000L;
		if (now - plData.lastMessageTime < Settings.AntiSpam.Messages.DELAY)
			if (!Common.hasPerm(pl, Permissions.Bypasses.DELAY_CHAT)) {
				long time = Settings.AntiSpam.Messages.DELAY - (now - plData.lastMessageTime);

				Common.tell(pl, Localization.CHAT_WAIT_MESSAGE.replace("%time", String.valueOf(time)).replace("%seconds", Localization.Parts.SECONDS.formatNumbers(time)));
				e.setCancelled(true);
				return;
			}
		plData.lastMessageTime = now;

		if (Settings.AntiSpam.Messages.SIMILARITY > 0 && Settings.AntiSpam.Messages.SIMILARITY < 100) {
			String strippedMsg = Common.prepareForSimilarityCheck(message);

			if (Common.similarity(strippedMsg, plData.lastMessage) > Settings.AntiSpam.Messages.SIMILARITY)
				if (!Common.hasPerm(pl, Permissions.Bypasses.SIMILAR_CHAT)) {
					Common.tell(pl, Localization.ANTISPAM_SIMILAR_MESSAGE);
					e.setCancelled(true);
					return;
				}
			plData.lastMessage = strippedMsg;
		}

		if (Settings.Rules.CHECK_CHAT && !Common.hasPerm(e.getPlayer(), Permissions.Bypasses.RULES))
			message = ChatControl.instance().chatCeaser.parseRules(e, pl, message);

		if (e.isCancelled()) // cancelled from chat ceaser
			return;

		if (Settings.AntiCaps.ENABLED && !Common.hasPerm(pl, Permissions.Bypasses.CAPS))
			if (message.length() >= Settings.AntiCaps.MIN_MESSAGE_LENGTH) {
				String msgBefore = message;
				int[] newMessage = Common.checkCaps(message);
				if (Common.percentageCaps(newMessage) >= Settings.AntiCaps.MIN_CAPS_PERCENTAGE || Common.checkCapsInRow(newMessage) >= Settings.AntiCaps.MIN_CAPS_IN_A_ROW) {

					String[] parts = message.split(" ");
					boolean capsAllowed = false;
					boolean whitelisted = false;

					for (int i = 0; i < parts.length; i++) {
						for (String whitelist : Settings.AntiCaps.WHITELIST) {
							if (whitelist.equalsIgnoreCase(parts[i])) {
								whitelisted = true;
								capsAllowed = true;
								continue;
							}
						}

						if (Settings.AntiCaps.IGNORE_USERNAMES) {
							for (Player online : Bukkit.getOnlinePlayers())
								if (online.getName().equalsIgnoreCase(parts[i])) {
									whitelisted = true;
									capsAllowed = true;
									continue;
								}
						}

						if (!whitelisted) {
							if (!capsAllowed) {
								char firstChar = parts[i].charAt(0);
								parts[i] = firstChar + parts[i].toLowerCase().substring(1);
							} else
								parts[i] = parts[i].toLowerCase();

							capsAllowed = !parts[i].endsWith(".") && !parts[i].endsWith("!");
						}

						whitelisted = false;
					}

					message = StringUtils.join(parts, " ");

					if (!msgBefore.equals(message) && Settings.AntiCaps.WARN_PLAYER)
						Common.tellLater(pl, 1, Localization.ANTISPAM_CAPS_MESSAGE);
				}
			}

		if (!Common.hasPerm(pl, Permissions.Bypasses.CAPITALIZE))
			message = Common.capitalize(message);
		if (!Common.hasPerm(pl, Permissions.Bypasses.PUNCTUATE))
			message = Common.insertDot(message);

		if (!message.equals(e.getMessage()))
			e.setMessage(message);

		if (Settings.Writer.ENABLED && !Settings.Writer.WHITELIST_PLAYERS.contains(pl.getName().toLowerCase()))
			Writer.Write(Writer.CHAT_FILE_PATH, pl.getName(), message);

		if (Settings.SoundNotify.ENABLED && !RushCoreHook.zapnute)
			if (Settings.SoundNotify.CHAT_PREFIX.equalsIgnoreCase("none")) {
				for (Player online : Bukkit.getOnlinePlayers())
					if (message.toLowerCase().contains(online.getName().toLowerCase()) && canSoundNotify(online.getName()) && Common.hasPerm(online, Permissions.Notify.WHEN_MENTIONED))
						online.playSound(online.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);

			} else
				for (Player online : Bukkit.getOnlinePlayers())
					if (message.toLowerCase().contains(Settings.SoundNotify.CHAT_PREFIX + online.getName().toLowerCase()) && canSoundNotify(online.getName())
							&& Common.hasPerm(online, Permissions.Notify.WHEN_MENTIONED))
						online.playSound(online.getLocation(), Settings.SoundNotify.SOUND.sound, Settings.SoundNotify.SOUND.volume, Settings.SoundNotify.SOUND.pitch);
	}

	public boolean canSoundNotify(String pl) {
		if (!Settings.SoundNotify.ONLY_WHEN_AFK || ChatControl.instance().ess == null)
			return true;

		return ChatControl.instance().ess.isAfk(pl);
	}
}