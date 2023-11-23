package dev.imprex.testsuite.command.command;

import static dev.imprex.testsuite.command.ArgumentBuilder.argument;
import static dev.imprex.testsuite.command.ArgumentBuilder.literal;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.api.TestsuiteSender;
import dev.imprex.testsuite.api.TestsuiteServer;
import dev.imprex.testsuite.command.suggestion.CommandSuggestion;
import dev.imprex.testsuite.util.Chat;

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
				.executes(command -> Chat.send(command, "Please enter a server name"))
				.then(
					argument("name", StringArgumentType.string())
					.suggests(this.suggestion.server()
							.hasStatus(UtilizationState.RUNNING)
							.buildSuggest("name"))
					.executes(this::connectServer)
					.then(
							argument("player", StringArgumentType.string())
							.suggests(this.suggestion.player()
									.map(TestsuitePlayer::getName)
									.buildSuggest("player"))
							.executes(this::connectPlayer)));
	}

	public int connectServer(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		TestsuiteServer server = this.plugin.getServer(serverName);
		if (server == null) {
			Chat.send(context, "Unable to find server \"{0}\"", serverName);
			return Command.SINGLE_SUCCESS;
		}

		if (server.getPlayers().contains(context.getSource())) {
			Chat.send(context, "Your already connected to this server!");
			return Command.SINGLE_SUCCESS;
		}

		((TestsuitePlayer) context.getSource()).connect(server);
		Chat.send(context, "Connecting to \"{0}\"", server.getName());
		return Command.SINGLE_SUCCESS;
	}

	public int connectPlayer(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		TestsuiteServer server = this.plugin.getServer(serverName);
		if (server == null) {
			Chat.send(context, "Unable to find server \"{0}\"", serverName);
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
				Chat.send(targetPlayer, "{0} sending you to \"{1}\"", executorName, server.getName());
			}
			Chat.send(context, "Connecting {0} players to \"{1}\"", sendCount, server.getName());
		} else {
			TestsuitePlayer targetPlayer = this.plugin.getPlayer(playername);
			if (targetPlayer == null) {
				Chat.send(context, "Unable to find player {0}!", playername);
				return Command.SINGLE_SUCCESS;
			}

			if (server.getPlayers().contains(targetPlayer)) {
				Chat.send(context, "{0} is already connected to this server!", targetPlayer.getName());
				return Command.SINGLE_SUCCESS;
			}

			targetPlayer.connect(server);
			Chat.send(context, "Connecting {0} to \"{1}\"", targetPlayer.getName(), server.getName());
			Chat.send(targetPlayer, "{0} is sending you to \"{1}\"", executorName, server.getName());
		}
		return Command.SINGLE_SUCCESS;
	}
}