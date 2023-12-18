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
						.buildSuggest("name"))
				.then(
						argument("command", StringArgumentType.greedyString())
						.executes(this::executeCommand)));
	}

	public int executeCommand(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		if (server == null) {
			Chat.builder().append("Server was not found!").send(context);
			return Command.SINGLE_SUCCESS;
		}

		String command = context.getArgument("command", String.class);
		if (command.startsWith("/")) {
			command = command.substring(1);
		}

		Chat.builder(server).append("Executing command...", server.getName()).send(context);
		server.executeCommand(command).whenComplete((__, error) -> {
			if (error != null) {
				Chat.builder(server)
						.append("Unable to execute command! {0}", error.getMessage())
						.send(context);
			} else {
				Chat.builder(server).append("Executed command.").send(context);
			}
		});
		return Command.SINGLE_SUCCESS;
	}
}