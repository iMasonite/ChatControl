package kangarko.chatcontrol.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import kangarko.chatcontrol.ChatControl;
import kangarko.chatcontrol.config.Localization;
import kangarko.chatcontrol.config.Settings;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

public class UpdateCheck implements Runnable {

	public static boolean needsUpdate = false;
	public static String newVersion;

	private String fileurl;

	public UpdateCheck(String fileurl) {
		this.fileurl = fileurl;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		String oldversion = ChatControl.instance().getDescription().getVersion();

		if (oldversion.contains("SNAPSHOT") || oldversion.contains("DEV"))
			return;

		String newversion = oldversion;

		try {
			YamlConfiguration conf;
			InputStream is = new URL(fileurl).openConnection().getInputStream();
			conf = YamlConfiguration.loadConfiguration(is);
			newversion = conf.getString("version");

			if (newversion.contains("SNAPSHOT") || newversion.contains("DEV"))
				return;

			if (toNumber(newversion) > toNumber(oldversion))
				if (Settings.Updater.DOWNLOAD) {
					URL adresa = null;

					try {
						Common.Log("&bChatControl is updating! Downloading v" + newversion);

						adresa = new URL("https://raw.githubusercontent.com/kangarko/ChatControl/master/precompiled/ChatControl_v" + newversion + ".jar");

						Common.Log("Got file of size: " + (double) adresa.openConnection().getContentLengthLong() / 1000 + " kb");

						File file = new File(Bukkit.getUpdateFolder(), "ChatControl.jar");

						if (!file.exists())
							file.mkdirs();

						Files.copy(adresa.openStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

						Common.Log("Downloaded! File uploaded into the " + Bukkit.getUpdateFolder() + " folder. Please copy it to plugins folder.");
					} catch (FileNotFoundException ex) {
						Common.Warn("Cannot download file from " + adresa.toString() + " (Malformed URL / file not uploaded yet)");
					} catch (IOException ex) {
						Common.Warn("Cannot download file from " + adresa.toString() + " (check console for error)");
						ex.printStackTrace();
					}
				} else {
					needsUpdate = true;
					newVersion = newversion;

					String[] msgs = Localization.UPDATE_AVAILABLE.replace("%current", oldversion).replace("%new", newversion).split("\n");
					for (String part : msgs)
						Common.Log(part);
				}

		} catch (UnknownHostException | MalformedURLException ex) {
			Common.Warn("Update check failed, could not connect to: " + fileurl);

			if (Settings.DEBUG)
				ex.printStackTrace();
		} catch (NumberFormatException ex) {
			Common.Warn("Update check failed, malformed version string: " + ex.getMessage());
		} catch (IOException ex) {
			if (ex.getMessage().equals("Permission denied: connect"))
				Common.Warn("Unable to connect to the update site, check your internet/firewall.");
			else
				Common.Error("Error while checking for update from: " + fileurl, ex);
		}
	}

	private int toNumber(String s) {
		return Integer.valueOf(s.replace(".", "").replace("-BETA", "").replace("-ALPHA", ""));
	}
}