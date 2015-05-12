package kangarko.chatcontrol.hooks;

import org.bukkit.Bukkit;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;

public class MultiverseHook {

	private final MultiverseCore multiVerse;

	public MultiverseHook() {
		multiVerse = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
	}

	public String getColoredAlias(String world) {
		MultiverseWorld mvWorld = multiVerse.getMVWorldManager().getMVWorld(world);

		if (mvWorld != null)
			return mvWorld.getColoredWorldString();

		return world;
	}
}
