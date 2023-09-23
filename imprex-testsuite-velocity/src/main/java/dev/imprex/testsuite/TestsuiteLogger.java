package dev.imprex.testsuite;

import org.slf4j.Logger;

import com.velocitypowered.api.proxy.ProxyServer;

import dev.imprex.testsuite.util.Chat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class TestsuiteLogger {

	private static ProxyServer proxy;
	private static Logger logger;

	static void initialize(Logger loggerInstance) {
		if (logger != null) {
			throw new IllegalStateException("Logger instance is already defined");
		}
		logger = loggerInstance;
	}

	static void initialize(ProxyServer proxyInstance) {
		if (proxy != null) {
			throw new IllegalStateException("Logger proxy instance is already defined");
		}
		proxy = proxyInstance;
	}

	public static void broadcast(Component message) {
		info(PlainTextComponentSerializer.plainText().serialize(message));

		if (proxy != null) {
			proxy.getAllPlayers().forEach(player -> Chat.send(player, message));
		}
	}

	public static void broadcast(String message, Object... args) {
		broadcast(Component.text(Chat.format(message, args)));
	}

	public static void info(String message, Object... args) {
		logger.info(Chat.format(message, args));
	}

	public static void error(Throwable throwable, String message, Object... args) {
		logger.error(Chat.format(message, args), throwable);
	}
}