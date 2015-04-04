package kangarko.chatcontrol.filter;

import kangarko.chatcontrol.config.Settings;
import kangarko.chatcontrol.utils.Common;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

public class Log4jFilter implements Filter {

	private Log4jFilter() {
	}

	public static void init() {
		((Logger) LogManager.getRootLogger()).addFilter(new Log4jFilter());
	}

	@Override
	public Result filter(LogEvent record) {
		return checkMessage(record.getMessage().getFormattedMessage());
	}

	@Override
	public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object... arg4) {
		return checkMessage(message);
	}

	@Override
	public Result filter(Logger arg0, Level arg1, Marker arg2, Object message, Throwable arg4) {
		return checkMessage(message.toString());
	}

	@Override
	public Result filter(Logger arg0, Level arg1, Marker arg2, Message message, Throwable arg4) {
		return checkMessage(message.getFormattedMessage());
	}

	@Override
	public Result getOnMatch() {
		return Result.NEUTRAL;
	}

	@Override
	public Result getOnMismatch() {
		return Result.NEUTRAL;
	}

	private Result checkMessage(String message) {
		if (message == null || message.isEmpty())
			return Result.NEUTRAL;

		message = Common.stripColors(message);

		for (String filter : Settings.Console.FILTER_MESSAGES)
			if (message.equalsIgnoreCase(filter) || message.toLowerCase().contains(filter.toLowerCase()))
				return Result.DENY;	
		   //else if (Common.regExMatch(filter, message)) // TODO Temporary disabled. Causes server to silently crash if a message is printed inside the match method.
		   //	return Result.DENY;*/

		return Result.NEUTRAL;
	}
}
