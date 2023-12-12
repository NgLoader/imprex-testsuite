package dev.imprex.testsuite.command.command;

import static dev.imprex.testsuite.command.ArgumentBuilder.argument;
import static dev.imprex.testsuite.command.ArgumentBuilder.literal;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.api.TestsuiteSender;
import dev.imprex.testsuite.command.suggestion.CommandSuggestion;
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;

public class CommandStart {

	private final ServerManager serverManager;
	private final CommandSuggestion suggestion;

	public CommandStart(TestsuitePlugin plugin) {
		this.serverManager = plugin.getServerManager();
		this.suggestion = plugin.getCommandSuggestion();
	}

	public LiteralArgumentBuilder<TestsuiteSender> create() {
		return literal("start").then(
				argument("name", StringArgumentType.greedyString())
				.suggests(this.suggestion.server()
						.hasStatus(UtilizationState.OFFLINE)
						.buildSuggest("name"))
				.executes(this::startServer));
	}

	public int startServer(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		if (server == null) {
			Chat.builder().append("Server was not found!").send(context);
			return Command.SINGLE_SUCCESS;
		}

		if (server.getStatus() != UtilizationState.OFFLINE) {
			Chat.builder(server).append("Server is not offline!").send(context);
			return Command.SINGLE_SUCCESS;
		}

		Chat.builder(server).append("Requesting start...").send(context);
		server.start().whenComplete((__, error) -> {
			if (error != null) {
				Chat.builder(server).append("Server is unable to start! {0}", error.getMessage()).send(context);
			} else {
				Chat.builder(server).append("Starting server").send(context);
			}
		});
		return Command.SINGLE_SUCCESS;
	}
}