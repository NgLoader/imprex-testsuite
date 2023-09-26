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
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;

public class CommandConnect {

	public static LiteralArgumentBuilder<CommandSource> COMMAND;

	private final ProxyServer proxy;
	private final ServerManager serverManager;

	public CommandConnect(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.serverManager = plugin.getServerManager();

		COMMAND = this.create();
		this.register();
	}

	public void register() {
		CommandManager commandManager = this.proxy.getCommandManager();
		CommandMeta commandMeta = commandManager.metaBuilder("connect")
						.aliases("con", "tc")
				.plugin(this)
				.build();

		BrigadierCommand command = new BrigadierCommand(COMMAND);
		commandManager.register(commandMeta, command);
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("connect")
				.requires(sender -> sender instanceof Player)
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

	public int connectServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		RegisteredServer server = this.proxy.getServer(serverName).orElseGet(() -> null);
		if (server == null) {
			Chat.send(context, "Unable to find server \"{0}\"", serverName);
			return Command.SINGLE_SUCCESS;
		}

		if (server.getPlayersConnected().contains(context.getSource())) {
			Chat.send(context, "Your already connected to this server!");
			return Command.SINGLE_SUCCESS;
		}

		((Player) context.getSource()).createConnectionRequest(server).connectWithIndication();
		Chat.send(context, "Connecting to \"{0}\"", server.getServerInfo().getName());
		return Command.SINGLE_SUCCESS;
	}

	public int connectPlayer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		RegisteredServer server = this.proxy.getServer(serverName).orElseGet(() -> null);
		if (server == null) {
			Chat.send(context, "Unable to find server \"{0}\"", serverName);
			return Command.SINGLE_SUCCESS;
		}

		String executorName = context.getSource() instanceof Player executor ? executor.getUsername() : "CONSOLE";
		String playername = context.getArgument("player", String.class);
		if (playername.equalsIgnoreCase("@all")) {
			int sendCount = 0;
			for (Player targetPlayer : this.proxy.getAllPlayers()) {
				ServerConnection serverConnection = targetPlayer.getCurrentServer().orElseGet(() -> null);
				if (serverConnection != null && serverConnection.getServer().equals(server)) {
					continue;
				}

				sendCount++;
				targetPlayer.createConnectionRequest(server).connectWithIndication();
				Chat.send(targetPlayer, "{0} sending you to \"{1}\"", executorName, server.getServerInfo().getName());
			}
			Chat.send(context, "Connecting {0} players to \"{1}\"", sendCount, server.getServerInfo().getName());
		} else {
			Player targetPlayer = this.proxy.getPlayer(playername).orElseGet(() -> null);
			if (targetPlayer == null) {
				Chat.send(context, "Unable to find player {0}!", playername);
				return Command.SINGLE_SUCCESS;
			}

			if (server.getPlayersConnected().contains(targetPlayer)) {
				Chat.send(context, "{0} is already connected to this server!", targetPlayer.getUsername());
				return Command.SINGLE_SUCCESS;
			}

			targetPlayer.createConnectionRequest(server).connectWithIndication();
			Chat.send(context, "Connecting {0} to \"{1}\"", targetPlayer.getUsername(), server.getServerInfo().getName());
			Chat.send(targetPlayer, "{0} sending you to \"{1}\"", executorName, server.getServerInfo().getName());
		}
		return Command.SINGLE_SUCCESS;
	}

	public CompletableFuture<Suggestions> suggestPlayers(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		String input = builder.getRemaining().toLowerCase();

		List<String> players = this.proxy.getAllPlayers().stream().map(Player::getUsername).toList();
		players.add("@all");

		for (String playername : players) {
			if (playername.toLowerCase().contains(input)) {
				builder.suggest(playername);
			}
		}
		return builder.buildFuture();
	}

	public CompletableFuture<Suggestions> suggestServers(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		String input = builder.getRemaining().toLowerCase();
		Player player = (Player) context.getSource();
		ServerConnection serverConnection = player.getCurrentServer().orElseGet(() -> null);

		this.serverManager.getServers().stream()
			.filter(server -> server.getStatus() == UtilizationState.RUNNING)
			.filter(server ->
				serverConnection != null ?
						!serverConnection.getServerInfo().equals(server.getCurrentServer().getServerInfo()) :
						true
			)
			.map(server -> server.getName())
			.filter(name -> name.toLowerCase().contains(input))
			.forEach(builder::suggest);
		return builder.buildFuture();
	}
}