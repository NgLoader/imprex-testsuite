package dev.imprex.testsuite.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.argument;
import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.command.brigadier.BrigadierCommand;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public class CommandConnect {

	public static LiteralArgumentBuilder<CommandSender> COMMAND;

	private final ProxyServer proxy;
	private final ServerManager serverManager;

	public CommandConnect(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.serverManager = plugin.getServerManager();

		COMMAND = this.create();
	}

	public BrigadierCommand brigadierCommand() {
		return new BrigadierCommand(COMMAND, "con", "tc");
	}

	public LiteralArgumentBuilder<CommandSender> create() {
		return literal("connect")
				.requires(sender -> sender instanceof ProxiedPlayer)
				.executes(command -> Chat.send(command, "Please enter a server name"))
				.then(
					argument("name", StringArgumentType.string())
					.suggests(this::suggestServers)
					.executes(this::connectServer)
					.then(
							argument("player", StringArgumentType.string())
							.suggests(this::suggestPlayers)
							.executes(this::connectPlayer)));
	}

	public int connectServer(CommandContext<CommandSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInfo server = this.proxy.getServerInfo(serverName.toLowerCase());
		if (server == null) {
			Chat.send(context, "Unable to find server \"{0}\"", serverName);
			return Command.SINGLE_SUCCESS;
		}

		if (server.getPlayers().contains(context.getSource())) {
			Chat.send(context, "Your already connected to this server!");
			return Command.SINGLE_SUCCESS;
		}

		((ProxiedPlayer) context.getSource()).connect(server);
		Chat.send(context, "Connecting to \"{0}\"", server.getName());
		return Command.SINGLE_SUCCESS;
	}

	public int connectPlayer(CommandContext<CommandSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInfo server = this.proxy.getServerInfo(serverName);
		if (server == null) {
			Chat.send(context, "Unable to find server \"{0}\"", serverName);
			return Command.SINGLE_SUCCESS;
		}

		String executorName = context.getSource() instanceof ProxiedPlayer executor ? executor.getName() : "CONSOLE";
		String playername = context.getArgument("player", String.class);
		if (playername.equalsIgnoreCase("@all")) {
			int sendCount = 0;
			for (ProxiedPlayer targetPlayer : this.proxy.getPlayers()) {
				Server serverConnection = targetPlayer.getServer();
				if (serverConnection != null && serverConnection.getInfo().equals(server)) {
					continue;
				}

				sendCount++;
				targetPlayer.connect(server);
				Chat.send(targetPlayer, "{0} sending you to \"{1}\"", executorName, server.getName());
			}
			Chat.send(context, "Connecting {0} players to \"{1}\"", sendCount, server.getName());
		} else {
			ProxiedPlayer targetPlayer = this.proxy.getPlayer(playername);
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
			Chat.send(targetPlayer, "{0} sending you to \"{1}\"", executorName, server.getName());
		}
		return Command.SINGLE_SUCCESS;
	}

	public CompletableFuture<Suggestions> suggestPlayers(CommandContext<CommandSender> context, SuggestionsBuilder builder) {
		String input = builder.getRemaining().toLowerCase();

		List<String> players = this.proxy.getPlayers().stream().map(ProxiedPlayer::getName).toList();
		players.add("@all");

		for (String playername : players) {
			if (playername.toLowerCase().contains(input)) {
				builder.suggest(playername);
			}
		}
		return builder.buildFuture();
	}

	public CompletableFuture<Suggestions> suggestServers(CommandContext<CommandSender> context, SuggestionsBuilder builder) {
		String input = builder.getRemaining().toLowerCase();
		ProxiedPlayer player = (ProxiedPlayer) context.getSource();
		Server serverConnection = player.getServer();

		this.serverManager.getServers().stream()
			.filter(server -> server.getStatus() == UtilizationState.RUNNING)
			.filter(server ->
				serverConnection != null ?
						!serverConnection.getInfo().equals(server.getCurrentServer()) :
						true
			)
			.map(server -> server.getName())
			.filter(name -> name.toLowerCase().contains(input))
			.forEach(builder::suggest);
		return builder.buildFuture();
	}
}