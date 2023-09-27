package dev.imprex.testsuite;

import java.util.logging.Level;
import java.util.logging.Logger;

import dev.imprex.testsuite.util.Chat;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class TestsuiteLogger {

	private static ProxyServer proxy;
	private static Logger logger;

	private static boolean debug = false;

	static void initialize(Logger loggerInstance) {
		if (TestsuiteLogger.logger != null) {
			throw new IllegalStateException("Logger instance is already defined");
		}
		TestsuiteLogger.proxy = ProxyServer.getInstance();
		TestsuiteLogger.logger = loggerInstance;
	}

	public static void setDebugLogging(boolean logging) {
		TestsuiteLogger.debug = logging;
		if (TestsuiteLogger.debug) {
			TestsuiteLogger.debug("Debug logging has been enabled");
		}
	}

	public static void broadcast(BaseComponent message) {
		TestsuiteLogger.info(message.toPlainText());

		if (TestsuiteLogger.proxy != null) {
			TestsuiteLogger.proxy.getPlayers().forEach(player -> Chat.send(player, message));
		}
	}

	public static void broadcast(String message, Object... args) {
		TestsuiteLogger.broadcast(new TextComponent(Chat.format(message, args)));
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