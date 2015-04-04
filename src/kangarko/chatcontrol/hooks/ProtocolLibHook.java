package kangarko.chatcontrol.hooks;

import java.io.File;

import kangarko.chatcontrol.ChatControl;
import kangarko.chatcontrol.config.Settings;
import kangarko.chatcontrol.rules.ChatCeaser.PacketCancelledException;
import kangarko.chatcontrol.utils.Common;
import kangarko.chatcontrol.utils.Permissions;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

public class ProtocolLibHook {

	private static final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
	private static final JSONParser parser = new JSONParser();

	public static void init() {
		
		if (Settings.Packets.TabComplete.DISABLE) {
		
			if (new File("spigot.yml").exists())
				Common.Log("&aDetected spigot (or similar), it is recommended to use its inbuilt tab-complete instead.");

			manager.addPacketListener(new PacketAdapter(ChatControl.instance(), PacketType.Play.Client.TAB_COMPLETE) {

				@Override
				public void onPacketReceiving(PacketEvent e) {
					if (Common.hasPerm(e.getPlayer(), Permissions.Bypasses.TAB_COMPLETE))
						return;

					String msg = e.getPacket().getStrings().read(0);

					if (Settings.Packets.TabComplete.DISABLE_ONLY_IN_CMDS && !msg.startsWith("/"))
						return;

					if (Settings.Packets.TabComplete.ALLOW_IF_SPACE && msg.contains(" "))
						return;

					if (msg.length() > Settings.Packets.TabComplete.IGNORE_ABOVE_LENGTH)
						e.setCancelled(true);
				}
			});

		}

		if (Settings.Rules.CHECK_PACKETS) {
			manager.addPacketListener(new PacketAdapter(ChatControl.instance(), PacketType.Play.Server.CHAT) {

				@Override
				public void onPacketSending(PacketEvent e) {
					if (e.getPlayer() == null || !e.getPlayer().isOnline())
						return;

					StructureModifier<WrappedChatComponent> chat = e.getPacket().getChatComponents();

					String raw = chat.read(0).getJson();
					if (raw == null || raw.isEmpty())
						return;

					Object parsed;

					try {
						parsed = parser.parse(raw);
					} catch (Throwable t) {
						return;
					}

					if (!(parsed instanceof JSONObject))
						return;

					JSONObject json = (JSONObject) parsed;					
					String origin = json.toJSONString();

					try {
						ChatControl.instance().chatCeaser.parsePacketRules(e.getPlayer(), json);
					} catch (PacketCancelledException ex) {
						e.setCancelled(true);
						return;
					}

					if (!json.toJSONString().equals(origin))
						chat.write(0, WrappedChatComponent.fromJson(json.toJSONString()));
				}
			});
		}
	}
}
