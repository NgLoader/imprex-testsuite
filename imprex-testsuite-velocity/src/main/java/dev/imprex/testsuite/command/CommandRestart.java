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

public class CommandRestart {

	public static LiteralArgumentBuilder<CommandSource> COMMAND;

	private final ProxyServer proxy;
	private final ServerManager serverManager;

	public CommandRestart(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.serverManager = plugin.getServerManager();

		COMMAND = this.create();
		this.register();
	}

	public void register() {
		CommandManager commandManager = this.proxy.getCommandManager();
		CommandMeta commandMeta = commandManager.metaBuilder("restart")
				.aliases("tr")
				.plugin(this)
				.build();

		BrigadierCommand command = new BrigadierCommand(COMMAND);
		commandManager.register(commandMeta, command);
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("restart")
				.executes(this::restartCurrentServer)
				.then(
					argument("name", StringArgumentType.greedyString())
					.suggests(this::suggestServers)
					.executes(this::restartTargetServer));
	}

	public int restartCurrentServer(CommandContext<CommandSource> context) {
		if (context.getSource() instanceof Player player) {
			ServerConnection serverConnection = player.getCurrentServer().orElseGet(() -> null);
			if (serverConnection == null) {
				Chat.send(context, "Your currently not connected to any server!");
				return Command.SINGLE_SUCCESS;
			}

			String serverName = serverConnection.getServerInfo().getName();
			ServerInstance server = this.serverManager.getServer(serverName);
			this.restartServer(context.getSource(), server);
		} else {
			Chat.send(context, "Server was not found!");
		}
		return Command.SINGLE_SUCCESS;
	}

	public int restartTargetServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		this.restartServer(context.getSource(), server);
		return Command.SINGLE_SUCCESS;
	}

	public void restartServer(CommandSource source, ServerInstance instance) {
		if (instance == null) {
			Chat.send(source, "Server was not found!");
			return;
		}

		Chat.send(source, "Restarting server {0}...", instance.getName());
		instance.restart().whenComplete((__, error) -> {
			if (error != null) {
				Chat.send(source, "Server {0} is unable to restart! {1}", instance.getName(), error.getMessage());
			} else {
				Chat.send(source, "Server {0} restarting", instance.getName());
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