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

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.command.brigadier.BrigadierCommand;
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public class CommandRestart {

	public static LiteralArgumentBuilder<CommandSender> COMMAND;

	private final ServerManager serverManager;

	public CommandRestart(TestsuitePlugin plugin) {
		this.serverManager = plugin.getServerManager();

		COMMAND = this.create();
	}

	public BrigadierCommand brigadierCommand() {
		return new BrigadierCommand(COMMAND);
	}

	public LiteralArgumentBuilder<CommandSender> create() {
		return literal("restart")
				.executes(this::restartCurrentServer)
				.then(
					argument("name", StringArgumentType.greedyString())
					.suggests(this::suggestServers)
					.executes(this::restartTargetServer));
	}

	public int restartCurrentServer(CommandContext<CommandSender> context) {
		if (context.getSource() instanceof ProxiedPlayer player) {
			Server serverConnection = player.getServer();
			if (serverConnection == null) {
				Chat.send(context, "Your currently not connected to any server!");
				return Command.SINGLE_SUCCESS;
			}

			String serverName = serverConnection.getInfo().getName();
			ServerInstance server = this.serverManager.getServer(serverName);
			this.restartServer(context.getSource(), server);
		} else {
			Chat.send(context, "Server was not found!");
		}
		return Command.SINGLE_SUCCESS;
	}

	public int restartTargetServer(CommandContext<CommandSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		this.restartServer(context.getSource(), server);
		return Command.SINGLE_SUCCESS;
	}

	public void restartServer(CommandSender source, ServerInstance instance) {
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

	public CompletableFuture<Suggestions> suggestServers(CommandContext<CommandSender> context, SuggestionsBuilder builder) {
		String input = builder.getRemaining().toLowerCase();
		this.serverManager.getServers().stream()
			.filter(server -> server.getStatus() == UtilizationState.RUNNING || server.getStatus() == UtilizationState.STARTING)
			.map(server -> server.getName())
			.filter(name -> name.toLowerCase().contains(input))
			.forEach(builder::suggest);
		return builder.buildFuture();
	}
}