package dev.imprex.testsuite.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import dev.imprex.testsuite.server.ServerInstance;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

public class Chat {

	public static final BaseComponent[] PREFIX = new ComponentBuilder()
			.append(new ComponentBuilder("[").color(ChatColor.of(new java.awt.Color(60, 70, 200))).create())
			.append(new ComponentBuilder("Testsuite").color(ChatColor.of(new java.awt.Color(60, 180, 200))).create())
			.append(new ComponentBuilder("]").color(ChatColor.of(new java.awt.Color(60, 70, 200))).create())
			.append(new ComponentBuilder("").color(Color.GRAY).create())
			.append(" ")
			.create();

	public static int send(CommandContext<CommandSender> context, BaseComponent component) {
		return send(context.getSource(), component);
	}

	public static int send(CommandContext<CommandSender> context, String message, Object... args) {
		return send(context.getSource(), message, args);
	}

	public static int send(CommandSender audience, String message, Object... args) {
		return send(audience, new TextComponent(format(message, args)));
	}

	public static int send(CommandSender audience, BaseComponent component) {
		audience.sendMessage(new ComponentBuilder()
				.append(PREFIX)
				.append(component)
				.create());
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

		public static final ChatColor GRAY = ChatColor.of(new java.awt.Color(180, 180, 200));
		public static final ChatColor RED = ChatColor.of(new java.awt.Color(200, 40, 40));
		public static final ChatColor DARK_GREEN = ChatColor.of(new java.awt.Color(60, 180, 60));
		public static final ChatColor LIGHT_GREEN = ChatColor.of(new java.awt.Color(140, 200, 130));
		public static final ChatColor PURPLE = ChatColor.of(new java.awt.Color(200, 40, 200));

		public static ChatColor statusColor(ServerInstance server) {
			return switch (server.getServerStatus()) {
				case INSTALLING -> ChatColor.of(new java.awt.Color(200, 40, 200));
				default -> switch (server.getStatus()) {
						case STARTING -> ChatColor.of(new java.awt.Color(0, 200, 0));
						case RUNNING -> ChatColor.of(new java.awt.Color(60, 180, 60));
						case STOPPING -> ChatColor.of(new java.awt.Color(200, 40, 40));
						default -> ChatColor.of(new java.awt.Color(100, 100, 100));
					};
				};
		}
	}
}