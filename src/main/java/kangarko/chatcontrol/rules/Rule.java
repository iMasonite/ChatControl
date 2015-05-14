
package kangarko.chatcontrol.rules;

import java.util.HashMap;

import kangarko.chatcontrol.utils.Common;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/** Represents a single rule
 * 
 * @author kangarko */
public class Rule {
	
	/** Flags */
	public static final int CHAT = 0, COMMAND = 1, SIGN = 2;
	
	/** Required regular expression used against the checked message */
	private final String match;
	
	/** A name/id of the rule. Used in some messages. */
	private String id;
	
	/** Ignore following string/regular expression. */
	private String ignoredMessage;
	
	/** Ignore player if they have specified permission. */
	private String bypassPerm;
	
	/** Ignore events. Currently can be: chat, command or sign */
	private Integer ignoredEvent;
	
	/** Required regular expression used before the message is checked to strip characters */
	private String stripBefore;
	
	/** Replace the part of the checked message that matches {@link #match} with one of the string
	 * (randomly chosen) */
	private String[] replacement;
	
	/** Rewrite the entire message to specified string chosen randomly */
	private String[] rewrite;
	
	/** Optional commands executed as the server console divided by | */
	private String[] commandToExecute;
	
	/** A message to the player */
	private String warnMessage;
	
	/** A permission required to get message {@link #customNotifyMessage} */
	private String customNotifyPermission;
	
	/** A message broadcasted to players with {@link #customNotifyPermission} */
	private String customNotifyMessage;
	
	/** A kick message. */
	private String kickMessage;
	
	/** A handler that triggers when {@link #match} matches the checked message */
	private Handler handler;
	
	/** Whenever the message should be cancelled from appearing */
	private boolean cancel = false;
	
	/** Whenever the message should be logged and saved into a file. */
	private boolean log = false;
	
	/** How much money to take from the player (Vault must be loaded) */
	private Double fine;
	
	/** If set the rule is a {@link PacketRule}; */
	private PacketRule packetRule;
	
	/** Creates a new rule with provided regular expression
	 * 
	 * @param match the regular expression used against the checked message */
	public Rule(String match) {
		this.match = match;
	}
	
	/** Checks specified message against regex {@link #match}
	 * 
	 * @param message The checked message
	 * @return if the message matches the {@link #match} regular expression */
	public boolean matches(String message) {
		if (stripBefore != null) {
			message = message.replaceAll(stripBefore, "");
		}
		
		if (ignoredMessage != null && Common.regExMatch(ignoredMessage, message)) {
			Common.Debug("&fIGNORE&c:&r " + ignoredMessage + ", message \'" + message + "\' ignored");
			
			message = message.replaceAll(ignoredMessage, ""); // prevent bypasses
		}
		
		return Common.regExMatch(match, message);
	}
	
	public String getMatch() {
		return match;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		Validate.isTrue(this.id == null, "ID already set on: " + this);
		
		this.id = id;
	}
	
	public void setIgnoredMessage(String ignoredMessage) {
		Validate.isTrue(this.ignoredMessage == null, "Ignored message already set on: " + this);
		
		this.ignoredMessage = ignoredMessage;
	}
	
	public Integer getIgnoredEvent() {
		return ignoredEvent;
	}
	
	public void parseIgnoreEvent(String ignoreEvent) {
		Validate.isTrue(ignoredEvent == null, "Ignored event already set on: " + this);
		
		ignoredEvent = getEventFromName(ignoreEvent);
	}
	
	public String getBypassPerm() {
		return bypassPerm;
	}
	
	public void setBypassPerm(String bypassPerm) {
		Validate.isTrue(this.stripBefore == null, "Bypass permission already set on: " + this);
		
		this.bypassPerm = bypassPerm;
	}
	
	public void setStripBefore(String stripBefore) {
		Validate.isTrue(this.stripBefore == null, "Strip before already set on: " + this);
		
		this.stripBefore = stripBefore;
	}
	
	public String[] getReplacements() {
		return replacement;
	}
	
	public void parseReplacements(String line) {
		Validate.isTrue(this.replacement == null, "Replacement already set on: " + this);
		
		this.replacement = line.split("\\|");
	}
	
	public String[] getRewrites() {
		return rewrite;
	}
	
	public void parseRewrites(String line) {
		Validate.isTrue(this.rewrite == null, "Rewrite message already set on: " + this);
		
		this.rewrite = line.split("\\|");
	}
	
	public String[] getCommandsToExecute() {
		return commandToExecute;
	}
	
	public void parseCommandsToExecute(String line) {
		Validate.isTrue(this.commandToExecute == null, "Command to execute already set on: " + this);
		
		this.commandToExecute = line.split("\\|");
	}
	
	public String getWarnMessage() {
		return warnMessage;
	}
	
	public void setWarnMessage(String warnMessage) {
		Validate.isTrue(this.warnMessage == null, "Warn message already set on: " + this);
		
		this.warnMessage = warnMessage;
	}
	
	public void parseCustomNotify(String raw) {
		String[] parts = raw.split(" ");
		Validate.isTrue(parts.length > 0, "Malformed then notify - must specify permission and a message.");
		
		String permission = parts[0];
		
		customNotifyPermission = permission;
		customNotifyMessage = raw.replace(permission + " ", "");
	}
	
	public String getCustomNotifyMessage() {
		return customNotifyMessage;
	}
	
	public String getCustomNotifyPermission() {
		return customNotifyPermission;
	}
	
	public String getKickMessage() {
		return kickMessage;
	}
	
	public void setKickMessage(String kickMessage) {
		Validate.isTrue(this.kickMessage == null, "Kick message already set on: " + this);
		
		this.kickMessage = kickMessage.isEmpty() ? "Kicked from the server" : kickMessage;
	}
	
	public Handler getHandler() {
		return handler;
	}
	
	public void setHandler(Handler handler) {
		Validate.isTrue(this.handler == null, "Handler already set on: " + this);
		
		this.handler = handler;
	}
	
	public boolean cancelEvent() {
		return cancel;
	}
	
	public void setCancelEvent() {
		Validate.isTrue(!cancel, "Message already set to be cancelled on: " + this);
		
		cancel = true;
	}
	
	public boolean log() {
		return log;
	}
	
	public void setLog() {
		Validate.isTrue(!log, "Rule already being logged on: " + this);
		
		log = true;
	}
	
	public Double getFine() {
		return fine;
	}
	
	public void setFine(Double fine) {
		Validate.isTrue(this.fine == null, "Fine already set on: " + this);
		
		this.fine = fine;
	}
	
	public void setPacketRule() {
		Validate.isTrue(this.packetRule == null, "Rule is already a packet rule: " + this);
		
		this.packetRule = new PacketRule();
	}
	
	public PacketRule getPacketRule() {
		return packetRule;
	}
	
	@Override
	public String toString() {
		return Common.stripColors(getPacketRule() != null ? getPacketRule().toString() : ("Rule{\n" + (id != null ? "    Id = " + id + "\n" : "") + "    Match = \'" + match + "\',\n" + (stripBefore != null ? "    Strip Before Match = \'" + stripBefore + "\',\n" : "") + (bypassPerm != null ? "    Bypass With Perm = \'" + bypassPerm + "\',\n" : "") + (ignoredMessage != null ? "    Ignore Message = \'" + ignoredMessage + "\',\n" : "") + (ignoredEvent != null ? "    Ignore Event = \'" + ignoredEvent + "\',\n" : "") + (replacement != null ? "    Replace With = \'" + StringUtils.join(replacement, ",") + "\',\n" : "") + (rewrite != null ? "    Rewrite = \'" + rewrite + "\',\n" : "") + (commandToExecute != null ? "    Execute Command = \'" + StringUtils.join(commandToExecute, ",") + "\',\n" : "") + (handler != null ? "    Handler = \'" + handler + "\',\n" : "") + (warnMessage != null ? "    Warn Message = \'" + warnMessage + "\',\n" : "") + (cancel ? "    Deny = " + cancel + "\n" : "") + (log ? "    Log = " + log + "\n" : "") + "}"));
	}
	
	/** Short version of {@link #toString()} that returns only regex used in this rule.
	 * 
	 * @return rule's regular expression */
	public String toShortString() {
		return (getPacketRule() != null ? "PacketRule" : "Rule") + " {" + (id != null ? "ID=" + id + "," : "") + "Match=\'" + match + "\'}";
	}
	
	private int getEventFromName(String str) {
		switch (str.toLowerCase().replace(" ", "")) {
			case "chat":
			case "asyncplayerchatevent":
				return CHAT;
			case "command":
			case "commands":
			case "playercommandpreprocessevent":
				return COMMAND;
			case "sign":
			case "signs":
			case "signchangeevent":
				return SIGN;
			default:
				throw new NullPointerException("Unknown ignore event: " + str);
		}
	}
}


/** A special case rule used against chat packet. From normal rule uses only {@link #match} */
class PacketRule {
	
	/** Whenever the message should be cancelled from appearing. */
	private boolean deny = false;
	
	/** A string used to replace matched part of the checked message. */
	private String replace;
	
	/** A message to replace the entire checked message. */
	private String rewrite;
	
	/** Whenever the rule should not be logged into console even if Verbose is enabled. */
	private boolean doNotVerbose = false;
	
	/** A message to replace the entire checked message (custom per world). */
	private HashMap<String, String> rewritePerWorld;
	
	public void setDeny() {
		Validate.isTrue(!this.deny, "Rule is already denied: " + this);
		
		this.deny = true;
	}
	
	public boolean deny() {
		return deny;
	}
	
	public void setReplacePacket(String replace) {
		Validate.isTrue(this.replace == null, "Replace already set on: " + this);
		
		this.replace = replace;
	}
	
	public String getReplacePacket() {
		return replace;
	}
	
	public void setRewritePacket(String rewrite) {
		Validate.isTrue(this.rewrite == null, "Rewrite already set on: " + this);
		
		this.rewrite = rewrite;
	}
	
	public String getRewritePacket() {
		return rewrite;
	}
	
	public void addRewriteIn(String line) {
		if (rewritePerWorld == null) {
			rewritePerWorld = new HashMap<>();
		}
		
		String[] parts = line.split(" ");
		Validate.isTrue(parts.length > 1, "Malformed rule then rewritein, please specify world and message! If you want to hide rule, set the message to \'none\'. Example: then rewritein hardcore &cCommand disabled in Hardcore world.");
		
		Validate.isTrue(!this.rewritePerWorld.containsKey(parts[0]), "Rewrite already set in world: " + parts[0] + " to: " + this.rewritePerWorld.get(parts[0]) + " on: " + this);
		
		this.rewritePerWorld.put(parts[0], line.replace(parts[0] + " ", ""));
	}
	
	public HashMap<String, String> getRewritePerWorld() {
		return rewritePerWorld;
	}
	
	public boolean doNotVerboe() {
		return doNotVerbose;
	}
	
	public void setDoNotVerbose() {
		Validate.isTrue(!doNotVerbose, "Rule already being ignored from verbose: " + this);
		
		this.doNotVerbose = true;
	}
	
	@Override
	public String toString() {
		return "PacketRule{\n" + (replace != null ? "    Replace Word: \'" + replace + "\'\n" : "") + (rewrite != null ? "    Rewrite With: \'" + rewrite + "\'\n" : "") + "    Then Deny: " + deny + "\n" + "}";
	}
}
