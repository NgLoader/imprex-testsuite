package dev.imprex.testsuite.command.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.argument;
import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.command.suggestion.CommandSuggestion;
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;
import dev.imprex.testsuite.util.TestsuitePlayer;
import dev.imprex.testsuite.util.TestsuiteSender;
import dev.imprex.testsuite.util.TestsuiteServer;

public class CommandStop {

	public static LiteralArgumentBuilder<TestsuiteSender> COMMAND;

	private final ServerManager serverManager;
	private final CommandSuggestion suggestion;

	public CommandStop(TestsuitePlugin plugin) {
		this.serverManager = plugin.getServerManager();
		this.suggestion = plugin.getCommandSuggestion();
	}

	public LiteralArgumentBuilder<TestsuiteSender> create() {
		return literal("stop")
				.executes(this::stopCurrentServer)
				.then(
					argument("name", StringArgumentType.greedyString())
					.suggests(this.suggestion.server()
							.hasStatus(UtilizationState.STARTING, UtilizationState.RUNNING)
							.buildSuggest("name"))
					.executes(this::stopTargetServer));
	}

	public int stopCurrentServer(CommandContext<TestsuiteSender> context) {
		if (context.getSource() instanceof TestsuitePlayer player) {
			TestsuiteServer serverConnection = player.getServer();
			if (serverConnection == null) {
				Chat.send(context, "Your currently not connected to any server!");
				return Command.SINGLE_SUCCESS;
			}

			String serverName = serverConnection.getName();
			ServerInstance server = this.serverManager.getServer(serverName);
			this.stopServer(context.getSource(), server);
		} else {
			Chat.send(context, "Server was not found!");
		}
		return Command.SINGLE_SUCCESS;
	}

	public int stopTargetServer(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		this.stopServer(context.getSource(), server);
		return Command.SINGLE_SUCCESS;
	}

	public void stopServer(TestsuiteSender source, ServerInstance instance) {
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
}