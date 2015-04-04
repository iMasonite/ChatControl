package kangarko.chatcontrol.filter;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import kangarko.chatcontrol.config.Settings;
import kangarko.chatcontrol.utils.Common;

public class ConsoleFilter implements Filter {

	@Override
	public boolean isLoggable(LogRecord record) {
		String msg = record.getMessage();
		
		if (msg == null || msg.isEmpty())
			return false;

		for (String ignored : Settings.Console.FILTER_MESSAGES) {
			if (msg.equalsIgnoreCase(ignored) || msg.toLowerCase().contains(ignored.toLowerCase()))
				return false;
			else if (Common.regExMatch(ignored, msg))
				return false;
		}

		return true;
	}
}