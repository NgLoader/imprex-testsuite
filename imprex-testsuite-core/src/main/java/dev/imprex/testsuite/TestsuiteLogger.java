package dev.imprex.testsuite;

import java.util.logging.Level;
import java.util.logging.Logger;

import dev.imprex.testsuite.util.Chat;
import dev.imprex.testsuite.util.ChatMessageBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class TestsuiteLogger {

	private static TestsuitePlugin plugin;
	private static Logger logger;

	private static boolean debug = false;

	static void initialize(TestsuitePlugin plugin, Logger loggerInstance) {
		if (TestsuiteLogger.logger != null || TestsuiteLogger.plugin != null) {
			throw new IllegalStateException("Logger instance is already defined");
		}

		TestsuiteLogger.plugin = plugin;
		TestsuiteLogger.logger = loggerInstance;
	}

	public static void setDebugLogging(boolean logging) {
		TestsuiteLogger.debug = logging;
		if (TestsuiteLogger.debug) {
			TestsuiteLogger.debug("Debug logging has been enabled");
		}
	}

	public static void broadcast(ChatMessageBuilder<?> messageBuilder) {
		Component message = messageBuilder.build();
		TestsuiteLogger.info(PlainTextComponentSerializer.plainText().serialize(message));

		if (TestsuiteLogger.plugin != null) {
			TestsuiteLogger.plugin.getPlayers().forEach(messageBuilder::send);
		}
	}

	public static void info(String message, Object... args) {
		TestsuiteLogger.logger.info(Chat.format(message, args));
	}

	public static void debug(String message, Object... args) {
		if (TestsuiteLogger.debug) {
			TestsuiteLogger.logger.log(Level.INFO, "[Debug] " + Chat.format(message, args));
		}
	}

	public static void error(Throwable throwable, String message, Object... args) {
		TestsuiteLogger.logger.log(Level.SEVERE, Chat.format(message, args), throwable);
	}
}