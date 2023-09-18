package dev.imprex.testsuite;

import org.slf4j.Logger;

public class TestsuiteLogger {

	private static Logger logger;

	static void initialize(Logger loggerInstance) {
		if (logger != null) {
			throw new IllegalStateException("Logger is already defined");
		}
		logger = loggerInstance;
	}

	public static void info(String message, Object... args) {
		logger.info(formatMessage(message, args));
	}

	public static void error(Throwable throwable, String message, Object... args) {
		logger.error(formatMessage(message, args), throwable);
	}

	private static String formatMessage(String message, Object... args) {
		char[] array = message.toCharArray();

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			char letter = array[i];

			if (array[i] == '{' && array.length > i + 2 && array[i + 2] == '}') {	
				try {
					int index = Integer.valueOf(String.valueOf(array[i + 1]));

					i += 2;

					if (args.length > index) {
						builder.append(args[index].toString());
					}
				} catch (NumberFormatException e) {
				}
				continue;
			}

			builder.append(letter);
		}

		return builder.toString();
	}
}