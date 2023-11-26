package de.imprex.testsuite.local;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LocalLogger extends Logger {

	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

	protected LocalLogger() {
		super("TestsuiteLogger", null);
	}

	@Override
	public void log(LogRecord record) {
		System.out.println(String.format("%s %s: %s",
				SIMPLE_DATE_FORMAT.format(new Date(record.getInstant().toEpochMilli())),
				record.getLevel().getName(),
				record.getMessage()));
	}
}
