
package kangarko.chatcontrol.rules;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import kangarko.chatcontrol.ChatControl;
import kangarko.chatcontrol.config.Settings;
import kangarko.chatcontrol.utils.Common;
import kangarko.chatcontrol.utils.LagCatcher;
import kangarko.chatcontrol.utils.Writer;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/** Custom rule engine. Reads a set of rules from a file
 * 
 * @author kangarko
 * @since 5.0.0 */
public final class ChatCeaser {
	
	/** Stored rules by file. Can only be modified in {@link #load()} method. */
	private final HashMap<String, List<Rule>> rulesMap = new HashMap<>();
	
	/** Rule files. */
	private final String RULES = "rules.txt", CHAT = "chat.txt", COMMAND = "commands.txt",
			SIGN = "sign.txt", PACKET = "packets.txt";
	
	private final Random rand = new Random();
	
	/** Clears {@link #rules} and load them . */
	public void load() {
		rulesMap.clear();
		
		loadRules(RULES, CHAT, COMMAND, SIGN, PACKET);
	}
	
	/** Fill {@link #rules} with rules in specified file paths.
	 * 
	 * @param filePaths the paths for every rule file */
	private void loadRules(String... filePaths) {
		for (String path : filePaths) {
			File file = Writer.Extract("rules/" + path);
			List<Rule> createdRules = new ArrayList<>();
			
			try {
				Rule rule = null; // The rule being created.
				String previousRuleName = null;
				boolean packetRule = path.equals(PACKET);
				
				List<String> rawLines = Files.readAllLines(Paths.get(file.toURI()), StandardCharsets.UTF_8);
				
				for (int i = 0; i < rawLines.size(); i++) {
					String line = rawLines.get(i).trim();
					
					if (!line.isEmpty() && !line.startsWith("#"))
					// If a line starts with 'match ' then assume a new rule is found and start creating it.
					// This makes a new instance of 'rule' variable.
					if (line.startsWith("match ")) {
						if (rule != null) { // Found another match, assuming previous rule is finished creating.
							Validate.isTrue(!createdRules.contains(path), path + " already contains rule where match is: " + line);
							createdRules.add(rule);
						}
						
						rule = new Rule(line.replaceFirst("match ", ""));
						previousRuleName = rule.toShortString();
						
						if (packetRule) {
							rule.setPacketRule();
						}
					}
					else {
						Objects.requireNonNull(rule, "Cannot define an operator when no rule is being created! File: \'" + path + "\' Line: \'" + line + "\' Previous rule: \'" + previousRuleName + "\'");
						// If a rule is being created then attempt to parse operators.
						
						if (packetRule) {
							if ("then deny".equals(line)) {
								rule.getPacketRule().setDeny();
							}
							else if ("dont verbose".equals(line)) {
								rule.getPacketRule().setDoNotVerbose();
							}
							else if (line.startsWith("then replace ")) {
								rule.getPacketRule().setReplacePacket(line.replaceFirst("then replace ", ""));
							}
							else if (line.startsWith("then rewrite ")) {
								rule.getPacketRule().setRewritePacket(line.replaceFirst("then rewrite ", ""));
							}
							else if (line.startsWith("then rewritein ")) {
								rule.getPacketRule().addRewriteIn(line.replaceFirst("then rewritein ", ""));
							}
							else throw new NullPointerException("Unknown packet rule operator: " + line);
						}
						else {
							if ("then deny".equals(line)) {
								rule.setCancelEvent();
							}
							else if ("then log".equals(line)) {
								rule.setLog();
							}
							else if (line.startsWith("strip ")) {
								rule.setStripBefore(line.replaceFirst("strip ", ""));
							}
							else if (line.startsWith("id ")) {
								rule.setId(line.replaceFirst("id ", ""));
							}
							else if (line.startsWith("ignore string ")) {
								rule.setIgnoredMessage(line.replaceFirst("ignore string ", ""));
							}
							else if (line.startsWith("ignore event ")) {
								rule.parseIgnoreEvent(line.replaceFirst("ignore event ", ""));
							}
							else if (line.startsWith("ignore perm ")) {
								rule.setBypassPerm(line.replaceFirst("ignore perm ", ""));
							}
							else if (line.startsWith("then rewrite ")) {
								rule.parseRewrites(line.replaceFirst("then rewrite ", ""));
							}
							else if (line.startsWith("then replace ")) {
								rule.parseReplacements(line.replaceFirst("then replace ", ""));
							}
							else if (line.startsWith("then replace")) {
								rule.parseReplacements(line.replaceFirst("then replace", ""));
								//
							}
							else if (line.startsWith("then console ")) {
								rule.parseCommandsToExecute(line.replaceFirst("then console ", ""));
							}
							else if (line.startsWith("then warn ")) {
								rule.setWarnMessage(line.replaceFirst("then warn ", ""));
							}
							else if (line.startsWith("then notify ")) {
								rule.parseCustomNotify(line.replaceFirst("then notify ", ""));
							}
							else if (line.startsWith("then fine ")) {
								rule.setFine(Double.parseDouble(line.replaceFirst("then fine ", "")));
							}
							else if (line.startsWith("then kick ")) {
								rule.setKickMessage(line.replaceFirst("then kick ", ""));
							}
							else if (line.startsWith("then kick")) {
								rule.setKickMessage(line.replaceFirst("then kick", ""));
								//
							}
							else if (line.startsWith("handle as ")) {
								rule.setHandler(HandlerLoader.loadHandler(line.replaceFirst("handle as ", ""), rule.getId()));
							}
							else throw new NullPointerException("Unknown operator: " + line);
						}
					}
					
					if (i + 1 == rawLines.size() && rule != null) {
						createdRules.add(rule);
					}
				}
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
			
			Validate.isTrue(!rulesMap.containsKey(path), "Rules map already contains rules from: " + path + "!");
			rulesMap.put(path, createdRules);
		}
		
		if (Settings.DEBUG) {
			for (String file : rulesMap.keySet()) {
				Common.Debug("&e" + Common.consoleLine());
				Common.Debug("&eDisplaying rules from: " + file);
				
				for (Rule rule : rulesMap.get(file)) {
					Common.Debug("Loaded rule:\n" + rule);
				}
			}
		}
		
		for (String file : rulesMap.keySet()) {
			Common.Verbose("&fLoaded " + rulesMap.get(file).size() + " Rules in " + file);
		}
	}
	
	/** Check the message against all rules. Can cancel the event or return modified message.
	 * 
	 * @param e the event - must be cancellable
	 * @param pl the player that triggered filtering
	 * @param msg the message that is being checked
	 * @return the message that was initially put, might be changed */
	public <T extends Cancellable> String parseRules(T e, Player pl, String msg) {
		int flag = Rule.CHAT;
		
		if (e instanceof PlayerCommandPreprocessEvent) {
			flag = Rule.COMMAND;
		}
		else if (e instanceof SignChangeEvent) {
			flag = Rule.SIGN;
		}
		
		LagCatcher.start("Rule parse");
		
		String origin = msg;
		
		// First iterate over all rules.
		List<Rule> rules = rulesMap.get(RULES);
		
		LagCatcher.start("Rule parse: global");
		msg = iterateStandardRules(rules, e, pl, msg, flag, true);
		LagCatcher.end("Rule parse: global");
		
		if (flag == Rule.CHAT) {
			rules = rulesMap.get(CHAT);
		}
		else if (flag == Rule.COMMAND) {
			rules = rulesMap.get(COMMAND);
		}
		else if (flag == Rule.SIGN) {
			rules = rulesMap.get(SIGN);
		}
		
		// Then iterate over specific rules for events.
		LagCatcher.start("Rule parse from: " + e.getClass().getSimpleName());
		msg = iterateStandardRules(rules, e, pl, msg, flag, false);
		LagCatcher.end("Rule parse from: " + e.getClass().getSimpleName());
		
		if (e.isCancelled()) {
			Common.Verbose("&fOriginal message &ccancelled&f.");
		}
		else if (!origin.equals(msg)) {
			Common.Verbose("&fFINAL&a: &r" + msg);
		}
		
		LagCatcher.end("Rule parse");
		
		return msg;
	}
	
	/** Internal method, {@link #parseRules(Cancellable, Player, String)} */
	private <T extends Cancellable> String iterateStandardRules(List<Rule> rules, T e, Player pl, String msg, int flag, boolean global) {
		for (Rule rule : rules) {
			if (!global && rule.getIgnoredEvent() != null && rule.getIgnoredEvent() == flag) {
				continue;
			}
			
			if (rule.getBypassPerm() != null) if (Common.hasPerm(pl, rule.getBypassPerm())) {
				continue;
			}
			
			if (rule.matches(msg)) {
				
				Common.Verbose("&f*--------- ChatControl rule match on " + pl.getName() + " --------- ID " + (rule.getId() != null ? rule.getId() : "UNSET"));
				Common.Verbose("&fMATCH&b: &r" + (Settings.DEBUG ? rule : rule.getMatch()));
				Common.Verbose("&fCATCH&b: &r" + msg);
				
				if (rule.log()) {
					Common.Log(org.bukkit.ChatColor.RED + (flag == Rule.SIGN ? "[SIGN at " + Common.shortLocation(pl.getLocation()) + "] " : "") + pl.getName() + " violated " + rule.toShortString() + " with message: &f" + msg);
					Writer.Write(Writer.RULES_FILE_PATH, pl.getName(), (flag == Rule.SIGN ? "[SIGN at " + Common.shortLocation(pl.getLocation()) + "] " : "") + rule.toShortString() + " caught message: " + msg);
				}
				
				if (rule.getCustomNotifyMessage() != null) {
					Objects.requireNonNull(rule.getCustomNotifyPermission(), "Custom alert permission cannot be null!");
					
					for (Player online : Bukkit.getOnlinePlayers())
						if (Common.hasPerm(online, rule.getCustomNotifyPermission())) {
							Common.tellLater(online, 1, replaceVariables(pl, rule, rule.getCustomNotifyMessage()).replace("%player", pl.getName()).replace("%message", msg));
						}
				}
				
				if (rule.getHandler() != null) {
					msg = handle(e, pl, rule.getMatch(), msg, rule.getHandler(), flag);
				}
				
				if (e.isCancelled()) return msg; // The message will not appear in the chat, no need to
																					// continue.
				
				if (rule.getRewrites() != null) {
					msg = getRandomString(pl, rule, rule.getRewrites());
				}
				
				if (rule.getReplacements() != null) {
					msg = msg.replaceAll("(?i)" + rule.getMatch(), getRandomString(pl, rule, rule.getReplacements()));
				}
				
				if (rule.getCommandsToExecute() != null) {
					for (String command : rule.getCommandsToExecute()) {
						command = replaceVariables(pl, rule, command);
						Common.customAction(pl, command, msg);
					}
				}
				
				if (rule.getWarnMessage() != null) {
					if (rule.cancelEvent()) {
						Common.tell(pl, replaceVariables(pl, rule, rule.getWarnMessage()));
					}
					else {
						Common.tellLater(pl, 1, replaceVariables(pl, rule, rule.getWarnMessage()));
					}
				}
				
				if (rule.getFine() != null && ChatControl.instance().vault != null) {
					ChatControl.instance().vault.takeMoney(pl.getName(), rule.getFine());
				}
				
				if (rule.getKickMessage() != null) {
					final Player Pl = pl;
					final Rule Rule = rule;
					
					new BukkitRunnable() {
						@Override
						public void run() {
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + Pl.getName() + " " + Common.colorize(Rule.getKickMessage()));
						}
					}.runTask(ChatControl.instance());
				}
				
				if (rule.cancelEvent()) {
					e.setCancelled(true);
					return msg; // The message will not appear in the chat, no need to continue.
				}
			}
		}
		
		HandlerCache.reset();
		return msg;
	}
	
	/** Handlers a custom handler. Returns the original message (can be modified) Can cancel the event. */
	private <T extends Cancellable> String handle(T e, Player pl, String match, String msg, Handler handler, int flag) {
		if (handler.getBypassPermission() != null && Common.hasPerm(pl, handler.getBypassPermission())) return msg;
		
		if (flag == Rule.COMMAND && handler.getIgnoredInCommands() != null) {
			for (String ignored : handler.getIgnoredInCommands())
				if (msg.startsWith(ignored)) return msg;
		}
		
		String warnMessage = handler.getPlayerWarnMsg();
		
		if (warnMessage != null && !HandlerCache.lastWarnMsg.equals(warnMessage)) {
			
			if (handler.blockMessage()) {
				Common.tell(pl, replaceVariables(pl, handler, warnMessage));
			}
			else {
				Common.tellLater(pl, 1, replaceVariables(pl, handler, warnMessage));
			}
			
			HandlerCache.lastWarnMsg = warnMessage;
		}
		
		String broadcastMessage = handler.getBroadcastMsg();
		
		if (broadcastMessage != null && !HandlerCache.lastBroadcastMsg.equals(broadcastMessage)) {
			Common.broadcastWithPlayer(replaceVariables(pl, handler, broadcastMessage).replace("%message", msg), pl.getName());
			HandlerCache.lastBroadcastMsg = broadcastMessage;
		}
		
		if (handler.getStaffAlertMsg() != null) {
			Objects.requireNonNull(handler.getStaffAlertPermission(), "Staff alert permission is null for: " + this);
			
			for (Player online : Bukkit.getOnlinePlayers())
				if (Common.hasPerm(online, handler.getStaffAlertPermission())) {
					Common.tell(online, (flag == Rule.SIGN ? "[SIGN at " + Common.shortLocation(pl.getLocation()) + "] " : "") + replaceVariables(pl, handler, handler.getStaffAlertMsg()).replace("%message", msg), pl.getName());
				}
		}
		
		if (handler.getConsoleMsg() != null) {
			Common.Log(replaceVariables(pl, handler, handler.getConsoleMsg()).replace("%player", pl.getName()).replace("%message", msg));
		}
		
		if (handler.getCommandsToExecute() != null) {
			for (String cmd : handler.getCommandsToExecute()) {
				Common.customAction(pl, replaceVariables(pl, handler, cmd), msg);
			}
		}
		
		if (handler.getWriteToFileName() != null) {
			Writer.Write(handler.getWriteToFileName(), pl.getName(), replaceVariables(pl, handler, "[Handler=%handler, Rule ID=%ruleID] ") + msg);
		}
		
		if (handler.blockMessage() || (flag == Rule.SIGN && Settings.Signs.BLOCK_WHEN_VIOLATES_RULE)) {
			e.setCancelled(true);
		}
		else if (handler.getMsgReplacement() != null) return msg.replaceAll("(?i)" + match, Common.colorize(replaceVariables(pl, handler, handler.getMsgReplacement())));
		else if (handler.getRewriteTo() != null) return Common.colorize(replaceVariables(pl, handler, handler.getRewriteTo()).replace("%player", pl.getName()).replace("%message", msg));
		
		return msg;
	}
	
	/** Parses the JSON chat message and check it against packet rules
	 * 
	 * @param input the JSON chat message object
	 * @return whenever the packet should be cancelled (this is defined in the packet rule)
	 * @throws PacketCancelledException if the packet should be cancelled */
	@SuppressWarnings("unchecked")
	public boolean parsePacketRules(Player player, Object input) throws PacketCancelledException {
		if (input instanceof JSONObject) {
			JSONObject objects = (JSONObject) input;
			
			for (Object key : objects.keySet()) {
				Object value = objects.get(key);
				
				if (value instanceof JSONObject) {
					parsePacketRules(player, (JSONObject) value);
				}
				else if (value instanceof JSONArray) {
					parsePacketRules(player, (JSONArray) value);
				}
				else if (value instanceof String) {
					String result = parsePacketRule(player, value.toString());
					objects.put(key, result);
				}
			}
			
		}
		else if (input instanceof JSONArray) {
			JSONArray array = (JSONArray) input;
			
			for (int i = 0; i < array.size(); i++) {
				Object value = array.get(i);
				
				if (value instanceof JSONObject) {
					parsePacketRules(player, (JSONObject) value);
				}
				else if (value instanceof JSONArray) {
					parsePacketRules(player, (JSONArray) value);
				}
				else if (value instanceof String) {
					String result = parsePacketRule(player, value.toString());
					array.set(i, result);
				}
			}
		}
		else {
			Common.Debug("Skipping unknown object: " + input.getClass().getSimpleName());
		}
		
		return false;
	}
	
	private String parsePacketRule(Player player, String msg) throws PacketCancelledException {
		if (msg == null || msg.isEmpty()) return msg;
		
		for (Rule standardrule : rulesMap.get(PACKET)) {
			if (standardrule.matches(Common.stripColors(msg.toLowerCase()))) {
				PacketRule rule = standardrule.getPacketRule();
				Objects.requireNonNull(rule, "Malformed rule - must be a packet rule: " + standardrule);
				
				if (!rule.doNotVerboe()) {
					Common.Verbose("&f*--------- ChatControl rule match: chat packet ---------");
					Common.Verbose("&fMATCH&b: &r" + (Settings.DEBUG ? rule : standardrule.getMatch()));
					Common.Verbose("&fCATCH&b: &r" + msg);
				}
				
				String origin = msg;
				String world = player.getWorld().getName();
				
				if (rule.deny()) {
					if (!rule.doNotVerboe()) {
						Common.Verbose("&fPacket sending &ccancelled&f.");
					}
					throw new PacketCancelledException();
				}
				
				else if (rule.getRewritePerWorld() != null && rule.getRewritePerWorld().get(world) != null) {
					msg = Common.colorize(replaceVariables(player, standardrule, rule.getRewritePerWorld().get(world)));
					
					if (msg.equalsIgnoreCase("none") || msg.equalsIgnoreCase("hidden")) {
						if (!rule.doNotVerboe()) {
							Common.Verbose("&fPacket sending &ccancelled&f.");
						}
						throw new PacketCancelledException();
					}
				}
				
				else if (rule.getRewritePacket() != null) {
					msg = Common.colorize(replaceVariables(player, standardrule, rule.getRewritePacket()));
				}
				else if (rule.getReplacePacket() != null) {
					msg = msg.replaceAll(standardrule.getMatch(), Common.colorize(rule.getReplacePacket()));
				}
				
				if (!origin.equals(msg) && !rule.doNotVerboe()) {
					Common.Verbose("&fFINAL&a: &r" + msg);
				}
			}
		}
		
		return msg;
	}
	
	/** Replaces rule ID (if set) and handler name (if set) in the message and player and world name.
	 * 
	 * @param player the player
	 * @param handler the handler the variables will be taken from
	 * @param message the message to replace variables in
	 * @returns message with modified variables */
	private String replaceVariables(Player player, Handler handler, String message) {
		return message.replace("%ruleID", handler.getRuleID()).replace("%handler", handler.getName()).replace("%player", player.getName()).replace("%world", player.getWorld().getName());
	}
	
	/** Replaces rule ID (if set) in the message and player and world name.
	 * 
	 * @param player the player
	 * @param rule the rule the id will be taken from
	 * @param message the message to replace variables in
	 * @returns message with modified variables */
	private String replaceVariables(Player player, Rule rule, String message) {
		return message.replace("%ruleID", rule.getId() != null ? rule.getId() : "UNSET").replace("%player", player.getName()).replace("%world", player.getWorld().getName());
	}
	
	/** Get one colorized string with replaced rule variables from a list.
	 * 
	 * @param rule the rule variables will be taken from
	 * @param messages the messages to choose from
	 * @return a colorized string with replaced variables randoly choosen from strings */
	private String getRandomString(Player player, Rule rule, String[] messages) {
		String randomMsg = messages[rand.nextInt(messages.length)];
		return Common.colorize(replaceVariables(player, rule, randomMsg));
	}
	
	public static class PacketCancelledException extends Exception {
		private static final long serialVersionUID = 1L;
	}
}


class HandlerLoader {
	
	private static YamlConfiguration cfg;
	private static String sectionName;
	
	static Handler loadHandler(String name, String ruleID) {
		File file = Writer.Extract("handlers.yml");
		cfg = YamlConfiguration.loadConfiguration(file);
		
		if (!cfg.isConfigurationSection(name)) throw new NullPointerException("Unknown handler: " + name);
		
		sectionName = cfg.getConfigurationSection(name).getName();
		
		Handler handler = new Handler(sectionName, ruleID);
		String message;
		
		message = getString("Bypass_With_Permission");
		if (isValid(message)) {
			handler.setBypassPermission(message);
		}
		
		message = getString("Player_Warn_Message");
		if (isValid(message)) {
			handler.setPlayerWarnMsg(message);
		}
		
		message = getString("Broadcast_Message");
		if (isValid(message)) {
			handler.setBroadcastMsg(message);
		}
		
		message = getString("Staff_Alert_Message");
		if (isValid(message)) {
			handler.setStaffAlertMsg(message);
		}
		
		message = getString("Staff_Alert_Permission");
		if (isValid(message)) {
			handler.setStaffAlertPermission(message);
		}
		
		message = getString("Console_Message");
		if (isValid(message)) {
			handler.setConsoleMsg(message);
		}
		
		message = getString("Write_To_File");
		if (isValid(message)) {
			handler.setWriteToFileName(message);
		}
		
		Boolean block = cfg.getBoolean(sectionName + ".Block_Message");
		if (block != null && block) {
			handler.setBlockMessage();
		}
		
		message = getString("Replace_Word");
		if (isValid(message)) {
			handler.setMsgReplacement(message);
		}
		
		message = getString("Replace_Whole");
		if (isValid(message)) {
			handler.setRewriteTo(message);
		}
		
		List<String> list;
		if (cfg.isSet(sectionName + ".Execute_Commands")) {
			list = cfg.getStringList(sectionName + ".Execute_Commands");
			handler.setCommandsToExecute(list);
		}
		
		if (cfg.isSet(sectionName + ".Ignored_In_Commands")) {
			list = cfg.getStringList(sectionName + ".Ignored_In_Commands");
			handler.setIgnoredInCommands(list);
		}
		
		sectionName = null;
		
		return handler;
	}
	
	private static boolean isValid(String msg) {
		return msg != null && !msg.isEmpty() && !msg.equalsIgnoreCase("none");
	}
	
	private static String getString(String path) {
		String msg = cfg.getString(sectionName + "." + path);
		
		return msg != null && !msg.isEmpty() && !msg.equalsIgnoreCase("none") ? msg : null;
	}
}


/** Caches last messages displayed to the player and broadcasted to everyone, prevents duplicate
 * displaying when multiple rules are violated at once. */
class HandlerCache {
	static String lastWarnMsg = "";
	static String lastBroadcastMsg = "";
	
	static void reset() {
		lastWarnMsg = "";
		lastBroadcastMsg = "";
	}
}
