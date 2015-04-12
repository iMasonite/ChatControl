package kangarko.chatcontrol.hooks;

import kangarko.rushcore.misc.PlayerInfo;

// A hook with my custom plugin on my server.
public class RushCoreHook {

	public static boolean zapnute = false;
	
	public static boolean moznoPrehratZvuk(String hraca) {
		if (!zapnute)
			return true;
		
		return PlayerInfo.of(hraca).zvukRp;
	}
	
	public static boolean moznoZobrazitSpravu(String hraca) {
		if (!zapnute)
			return true;

		return PlayerInfo.of(hraca).spravyTip;
	}
	
	/*{
		try {
			Object instance = playerInfoClass.getDeclaredMethod("of", String.class).invoke(null, hraca);
			return instance.getClass().getDeclaredField("spravyTip").getBoolean(instance);
		} catch (ReflectiveOperationException ex) {
			return true;
		}
	}*/
}
