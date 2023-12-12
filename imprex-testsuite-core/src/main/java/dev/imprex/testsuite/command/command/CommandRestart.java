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
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;

public class CommandRestart {

	private final ServerManager serverManager;
	private final CommandSuggestion suggestion;

	public CommandRestart(TestsuitePlugin plugin) {
		this.serverManager = plugin.getServerManager();
		this.suggestion = plugin.getCommandSuggestion();
	}

	public LiteralArgumentBuilder<TestsuiteSender> create() {
		return literal("restart")
				.executes(this::restartCurrentServer)
				.then(
					argument("name", StringArgumentType.greedyString())
					.suggests(this.suggestion.server()
							.hasStatus(UtilizationState.STARTING, UtilizationState.RUNNING)
							.buildSuggest("name"))
					.executes(this::restartTargetServer));
	}

	public int restartCurrentServer(CommandContext<TestsuiteSender> context) {
		if (context.getSource() instanceof TestsuitePlayer player) {
			TestsuiteServer serverConnection = player.getServer();
			if (serverConnection == null) {
				Chat.builder().append("Your currently not connected to any server!").send(context);
				return Command.SINGLE_SUCCESS;
			}

			String serverName = serverConnection.getName();
			ServerInstance server = this.serverManager.getServer(serverName);
			this.restartServer(context.getSource(), server);
		} else {
			Chat.builder().append("Server was not found!").send(context);
		}
		return Command.SINGLE_SUCCESS;
	}

	public int restartTargetServer(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		this.restartServer(context.getSource(), server);
		return Command.SINGLE_SUCCESS;
	}

	public void restartServer(TestsuiteSender source, ServerInstance instance) {
		if (instance == null) {
			Chat.builder().append("Server was not found!").send(source);
			return;
		}

		Chat.builder(instance).append("Requesting restart...").send(source);
		instance.restart().whenComplete((__, error) -> {
			if (error != null) {
				Chat.builder(instance).append("Server is unable to restart! {0}", error.getMessage()).send(source);
			} else {
				Chat.builder(instance).append("Restarting server").send(source);
			}
		});
	}
}