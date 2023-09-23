package dev.imprex.testsuite.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;

import dev.imprex.testsuite.server.ServerInstance;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class Chat {

	public static final Component PREFIX = Component.text("")
			.append(Component.text("[").color(TextColor.color(60, 70, 200)))
			.append(Component.text("Testsuite").color(TextColor.color(60, 180, 200)))
			.append(Component.text("]").color(TextColor.color(60, 70, 200)))
			.append(Component.space());

	public static int send(CommandContext<CommandSource> context, Component component) {
		return send(context.getSource(), component);
	}

	public static int send(CommandContext<CommandSource> context, String message, Object... args) {
		return send(context.getSource(), message, args);
	}

	public static int send(Audience audience, String message, Object... args) {
		return send(audience, Component.text(format(message, args)));
	}

	public static int send(Audience audience, Component component) {
		audience.sendMessage(PREFIX.append(component.colorIfAbsent(Color.GRAY)));
		return Command.SINGLE_SUCCESS;
	}

	public static String format(String message, Object... args) {
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

	public static class Color {

		public static final TextColor GRAY = TextColor.color(180, 180, 200);
		public static final TextColor RED = TextColor.color(200, 40, 40);
		public static final TextColor DARK_GREEN = TextColor.color(60, 180, 60);
		public static final TextColor LIGHT_GREEN = TextColor.color(140, 200, 130);
		public static final TextColor PURPLE = TextColor.color(200, 40, 200);

		public static TextColor statusColor(ServerInstance server) {
			return switch (server.getServerStatus()) {
				case INSTALLING -> TextColor.color(200, 40, 200);
				default -> switch (server.getStatus()) {
						case STARTING -> TextColor.color(0, 200, 0);
						case RUNNING -> TextColor.color(60, 180, 60);
						case STOPPING -> TextColor.color(200, 40, 40);
						default -> TextColor.color(100, 100, 100);
					};
				};
		}
	}
}