package dev.imprex.testsuite.command.command;

import static dev.imprex.testsuite.command.ArgumentBuilder.argument;
import static dev.imprex.testsuite.command.ArgumentBuilder.literal;

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

public class CommandReinstall {

	private final ServerManager serverManager;
	private final CommandSuggestion suggestion;

	public CommandReinstall(TestsuitePlugin plugin) {
		this.serverManager = plugin.getServerManager();
		this.suggestion = plugin.getCommandSuggestion();
	}

	public LiteralArgumentBuilder<TestsuiteSender> create() {
		return literal("reinstall").then(
				argument("name", StringArgumentType.greedyString())
				.suggests(this.suggestion.server()
						.buildSuggest("name"))
				.executes(this::deleteServer));
	}

	public int deleteServer(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		if (server == null) {
			Chat.builder().append("Server was not found!").send(context);
			return Command.SINGLE_SUCCESS;
		}

		Chat.builder(server).append("Requesting reinstall...").send(context);
		server.reinstall().whenComplete((__, error) -> {
			if (error != null) {
				error.printStackTrace();
				Chat.builder(server).append("Server is unable to reinstall! {0}", error.getMessage()).send(context);
			} else {
				Chat.builder(server).append("Reinstalling server...").send(context);
			}
		});
		return Command.SINGLE_SUCCESS;
	}
}