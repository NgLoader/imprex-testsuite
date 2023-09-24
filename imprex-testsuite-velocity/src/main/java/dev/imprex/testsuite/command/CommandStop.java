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

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;

public class CommandStop {

	public static LiteralArgumentBuilder<CommandSource> COMMAND;

	private final ProxyServer proxy;
	private final ServerManager serverManager;

	public CommandStop(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.serverManager = plugin.getServerManager();

		COMMAND = this.create();
		this.register();
	}

	public void register() {
		CommandManager commandManager = this.proxy.getCommandManager();
		CommandMeta commandMeta = commandManager.metaBuilder("stop")
				.plugin(this)
				.build();

		BrigadierCommand command = new BrigadierCommand(COMMAND);
		commandManager.register(commandMeta, command);
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("stop")
				.executes(this::stopCurrentServer)
				.then(
					argument("name", StringArgumentType.greedyString())
					.suggests(this::suggestServers)
					.executes(this::stopTargetServer));
	}

	public int stopCurrentServer(CommandContext<CommandSource> context) {
		if (context.getSource() instanceof Player player) {
			ServerConnection serverConnection = player.getCurrentServer().orElseGet(() -> null);
			if (serverConnection == null) {
				Chat.send(context, "Your currently not connected to any server!");
				return Command.SINGLE_SUCCESS;
			}

			String serverName = serverConnection.getServerInfo().getName();
			ServerInstance server = this.serverManager.getServer(serverName);
			this.stopServer(context.getSource(), server);
		} else {
			Chat.send(context, "Server was not found!");
		}
		return Command.SINGLE_SUCCESS;
	}

	public int stopTargetServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		this.stopServer(context.getSource(), server);
		return Command.SINGLE_SUCCESS;
	}

	public void stopServer(CommandSource source, ServerInstance instance) {
		if (instance == null) {
			Chat.send(source, "Server was not found!");
			return;
		}

		if (instance.getStatus() == UtilizationState.OFFLINE || instance.getStatus() == UtilizationState.STOPPING) {
			Chat.send(source, "Server {0} is not online!", instance.getName());
			return;
		}

		Chat.send(source, "Stopping server {0}...", instance.getName());
		instance.stop().whenComplete((__, error) -> {
			if (error != null) {
				Chat.send(source, "Server {0} is unable to stop! {1}", instance.getName(), error.getMessage());
			} else {
				Chat.send(source, "Server {0} stopped", instance.getName());
			}
		});
	}

	public CompletableFuture<Suggestions> suggestServers(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		String input = builder.getRemaining().toLowerCase();
		this.serverManager.getServers().stream()
			.filter(server -> server.getStatus() == UtilizationState.RUNNING || server.getStatus() == UtilizationState.STARTING)
			.map(server -> server.getName())
			.filter(name -> name.toLowerCase().contains(input))
			.forEach(builder::suggest);
		return builder.buildFuture();
	}
}