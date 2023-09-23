package dev.imprex.testsuite.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.argument;
import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

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

	private final ProxyServer proxy;
	private final ServerManager serverManager;

	public CommandConnect(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.serverManager = plugin.getServerManager();

		this.register();
	}

	public void register() {
		CommandManager commandManager = this.proxy.getCommandManager();
		CommandMeta commandMeta = commandManager.metaBuilder("connect")
						.aliases("con", "tc")
				.plugin(this)
				.build();

		BrigadierCommand command = new BrigadierCommand(this.create());
		commandManager.register(commandMeta, command);
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("connect")
				.requires(sender -> sender instanceof Player)
				.executes(command -> Chat.send(command, "Please enter a server name"))
				.then(
					argument("name", StringArgumentType.greedyString())
					.suggests(this::suggestServers)
					.executes(this::connectServer));
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