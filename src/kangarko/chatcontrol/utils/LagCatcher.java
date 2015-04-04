package kangarko.chatcontrol.utils;

import java.util.HashMap;

import kangarko.chatcontrol.config.Settings;

/**
 * Simple timings-like live lag catcher.
 * @author kangarko
 */
public class LagCatcher {

	public static HashMap<String, Long> lagMap = new HashMap<>();

	public static void start(String section) {
		if (Settings.CATCH_LAG == 0)
			return;
		
		if (lagMap.containsKey(section))
			Common.Debug("Lag of " + section + " already being measured!");

		lagMap.put(section, System.currentTimeMillis());
	}

	public static void end(String section) {
		end(section, Settings.CATCH_LAG);
	}
	
	public static void end(String section, int limit) {
		if (Settings.CATCH_LAG == 0)
			return;
		
		if (!lagMap.containsKey(section)) {
			Common.Debug("Lag measuring of " + section + " is not in our cache!");
			return;
		}

		long lag = System.currentTimeMillis() - lagMap.remove(section);
		
		if (lag > limit)
			Common.Log("&3[&fLag&3] &7" + section + " took &f" + lag + " ms");
	}
}
