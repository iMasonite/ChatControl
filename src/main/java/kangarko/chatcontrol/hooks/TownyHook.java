package kangarko.chatcontrol.hooks;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyHook {

	public String getNation(Player pl) {
		try {
			Town t = getTown(pl);

			return t != null ? t.getNation().getName() : "";
		} catch (Exception e) {
			return "";
		}
	}

	public String getTownName(Player pl) {
		Town t = getTown(pl);

		return t != null ? t.getName() : "";
	}

	private Town getTown(Player pl) {
		try {
			Resident res = TownyUniverse.getDataSource().getResident(pl.getName());

			if (res != null)
				return res.getTown();
		} catch (NotRegisteredException e) {
		}

		return null;
	}
}
