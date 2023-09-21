package dev.imprex.testsuite;

import org.slf4j.Logger;

import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

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
		if (proxy != null) {
			Component component = Component.text("")
					.append(Component.text("[").color(TextColor.color(60, 70, 200)))
					.append(Component.text("Testsuite").color(TextColor.color(60, 180, 200)))
					.append(Component.text("]").color(TextColor.color(60, 70, 200)))
					.append(Component.space())
					.append(message.color(TextColor.color(180, 180, 200)));
			proxy.getAllPlayers().forEach(player -> player.sendMessage(component));
		}
	}

	public static void broadcast(String message, Object... args) {
		broadcast(Component.text(formatMessage(message, args)));
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