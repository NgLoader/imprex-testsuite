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
import dev.imprex.testsuite.util.TestsuiteSender;

public class CommandExecute {

	private final ServerManager serverManager;
	private final CommandSuggestion suggestion;

	public CommandExecute(TestsuitePlugin plugin) {
		this.serverManager = plugin.getServerManager();
		this.suggestion = plugin.getCommandSuggestion();
	}

	public LiteralArgumentBuilder<TestsuiteSender> create() {
		return literal("exec").then(
				argument("name", StringArgumentType.word())
				.suggests(this.suggestion.server()
						.hasStatus(UtilizationState.RUNNING)
						.buildSuggest())
				.then(
						argument("command", StringArgumentType.greedyString())
						.executes(this::executeCommand)));
	}

	public int executeCommand(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		if (server == null) {
			Chat.send(context, "Server was not found!");
			return Command.SINGLE_SUCCESS;
		}

		String command = context.getArgument("command", String.class);
		if (command.startsWith("/")) {
			command = command.substring(1);
		}

		Chat.send(context, "Executing command on {0}...", server.getName());
		server.executeCommand(command).whenComplete((__, error) -> {
			if (error != null) {
				Chat.send(context, "Server {0} is unable to execute command! {1}", server.getName(), error.getMessage());
			} else {
				Chat.send(context, "Server {0} executed command.", server.getName());
			}
		});
		return Command.SINGLE_SUCCESS;
	}
}