package kangarko.chatcontrol.hooks;

import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SimpleClansHook {

	private final SimpleClans clans;

	public SimpleClansHook() {
		clans = (SimpleClans) Bukkit.getPluginManager().getPlugin("SimpleClans");
	}

	public String getClanTag(Player pl) {
		ClanPlayer clanPl = clans.getClanManager().getClanPlayer(pl);
		
		if (clanPl != null)
			return clanPl.getClan().getColorTag();

		return null;
	}
}
