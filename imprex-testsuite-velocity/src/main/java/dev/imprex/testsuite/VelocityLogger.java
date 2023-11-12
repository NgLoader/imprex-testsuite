package dev.imprex.testsuite;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class VelocityLogger extends Logger {

	private final org.slf4j.Logger logger;

	protected VelocityLogger(org.slf4j.Logger logger) {
		super(logger.getName(), null);
		this.logger = logger;
	}

	@Override
	public void log(LogRecord logRecord) {
		if (logRecord.getLevel() == Level.OFF) {
			return;
		} else if (logRecord.getLevel() == Level.SEVERE) {
			this.logger.error(logRecord.getMessage(), logRecord.getThrown());
		} else if (logRecord.getLevel() == Level.WARNING) {
			this.logger.warn(logRecord.getMessage(), logRecord.getThrown());
		} else if (logRecord.getLevel() == Level.CONFIG) {
			this.logger.debug(logRecord.getMessage(), logRecord.getThrown());
		} else {
			this.logger.info(logRecord.getMessage(), logRecord.getThrown());
		}
	}
}