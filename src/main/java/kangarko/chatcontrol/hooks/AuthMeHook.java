package kangarko.chatcontrol.hooks;

import org.bukkit.entity.Player;

import fr.xephi.authme.api.API;

public class AuthMeHook {

	public String getCountryCode(Player pl) {
		String ip = pl.getAddress().toString().replace("/", "");

		return API.instance.getCountryCode(ip);
	}

	public String getCountryName(Player pl) {
		String ip = pl.getAddress().toString().replace("/", "");

		return API.instance.getCountryName(ip);
	}
	
	public boolean isLogged(Player pl) {
		return API.isAuthenticated(pl);
	}
}
