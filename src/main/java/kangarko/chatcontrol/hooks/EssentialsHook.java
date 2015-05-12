package kangarko.chatcontrol.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

public class EssentialsHook {

	private final Essentials ess;

	public EssentialsHook() {
		ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
	}

	public User getUser(String pl) {
		return ess.getUserMap().getUser(pl);
	}

	public boolean isAfk(String pl) {
		User user = getUser(pl);

		if (user == null)
			return false;

		return user.isAfk();
	}

	public Player getReplyTo(String pl) {
		CommandSource source = getUser(pl).getReplyTo();

		if (source != null && source.isPlayer()) {
			Player player = source.getPlayer();
			if (player != null && player.isOnline())
				return player;
		}

		return null;
	}
}
