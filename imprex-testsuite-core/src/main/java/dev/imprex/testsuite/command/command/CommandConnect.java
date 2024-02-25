package dev.imprex.testsuite.command.command;

import static dev.imprex.testsuite.command.ArgumentBuilder.argument;
import static dev.imprex.testsuite.command.ArgumentBuilder.literal;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.api.TestsuiteSender;
import dev.imprex.testsuite.api.TestsuiteServer;
import dev.imprex.testsuite.command.ArgumentBuilder;
import dev.imprex.testsuite.command.suggestion.CommandSuggestion;
import dev.imprex.testsuite.util.Chat;
import dev.imprex.testsuite.util.ChatMessageBuilder.ChatMessageSenderBuilder;

public class CommandConnect {

	private final TestsuitePlugin plugin;
	private final CommandSuggestion suggestion;

	public CommandConnect(TestsuitePlugin plugin) {
		this.plugin = plugin;
		this.suggestion = plugin.getCommandSuggestion();
	}

	public LiteralArgumentBuilder<TestsuiteSender> create() {
		return literal("connect")
				.requires(sender -> sender instanceof TestsuitePlayer)
				.executes(command -> Chat.send(command, builder -> builder.append("Please enter a server name")))
				.then(
					argument("name", StringArgumentType.string())
					.suggests(this.suggestion.server()
							.hasStatus(UtilizationState.RUNNING)
							.buildSuggest("name"))
					.executes(this::connectServer)
					.then(
							argument("player", StringArgumentType.string())
							.suggests(this.suggestPlayers())
							.executes(this::connectPlayer)));
	}

	public SuggestionProvider<TestsuiteSender> suggestPlayers() {
		return (context, builder) -> {
			this.suggestion.player()
				.map(TestsuitePlayer::getName)
				.buildSuggest("player")
				.getSuggestions(context, builder);

			String input = ArgumentBuilder.getSafeStringArgument(context, "player", "");
			if ("all".startsWith(input)) {
				builder.suggest("all");
			}

			return builder.buildFuture();
		};
	}

	public int connectServer(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		TestsuiteServer server = this.plugin.getServer(serverName);
		ChatMessageSenderBuilder chatMessageBuilder = Chat.builder(server).sender(context);
		
		if (server == null) {
			chatMessageBuilder.append("Unable to find server \"{0}\"", serverName);
			return chatMessageBuilder.send();
		}

		if (server.getPlayers().contains(context.getSource())) {
			chatMessageBuilder.append("Your already connected to this server!");
			return chatMessageBuilder.send();
		}

		((TestsuitePlayer) context.getSource()).connect(server);
		chatMessageBuilder.append("Connecting to \"{0}\"", server.getName());
		return chatMessageBuilder.send();
	}

	public int connectPlayer(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		TestsuiteServer server = this.plugin.getServer(serverName);

		if (server == null) {
			Chat.builder(server).append("Unable to find server \"{0}\"", serverName).send(context);
			return Command.SINGLE_SUCCESS;
		}

		String executorName = context.getSource() instanceof TestsuitePlayer executor ? executor.getName() : "CONSOLE";
		String playername = context.getArgument("player", String.class);
		if (playername.equalsIgnoreCase("all")) {
			int sendCount = 0;
			for (TestsuitePlayer targetPlayer : this.plugin.getPlayers()) {
				TestsuiteServer serverConnection = targetPlayer.getServer();
				if (serverConnection != null && serverConnection.equals(server)) {
					continue;
				}

				sendCount++;
				targetPlayer.connect(server);
				Chat.builder(server).append("{0} sending you to \"{1}\"", executorName, server.getName()).send(targetPlayer);
			}
			
			Chat.builder(server).append("Connecting {0} players to \"{1}\"", sendCount, server.getName()).send(context);
		} else {
			TestsuitePlayer targetPlayer = this.plugin.getPlayer(playername);
			if (targetPlayer == null) {
				Chat.builder(server).append("Unable to find player {0}!", playername).send(context);
				return Command.SINGLE_SUCCESS;
			}

			if (server.getPlayers().contains(targetPlayer)) {
				Chat.builder(server).server(server).append("{0} is already connected to this server!", targetPlayer.getName()).send(context);
				return Command.SINGLE_SUCCESS;
			}

			targetPlayer.connect(server);
			Chat.builder(server).append("Connecting {0} to \"{1}\"", targetPlayer.getName(), server.getName()).send(context);
			Chat.builder(server).append("{0} is sending you to \"{1}\"", executorName, server.getName()).send(targetPlayer);
		}
		return Command.SINGLE_SUCCESS;
	}
}