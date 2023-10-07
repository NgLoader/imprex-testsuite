package dev.imprex.testsuite.command.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.argument;
import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

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

public class CommandDelete {

	private final ServerManager serverManager;
	private final CommandSuggestion suggestion;

	public CommandDelete(TestsuitePlugin plugin) {
		this.serverManager = plugin.getServerManager();
		this.suggestion = plugin.getCommandSuggestion();
	}

	public LiteralArgumentBuilder<TestsuiteSender> create() {
		return literal("delete").then(
				argument("name", StringArgumentType.greedyString())
				.suggests(this.suggestion.server().buildSuggest("name"))
				.executes(this::deleteServer));
	}

	public int deleteServer(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		if (server == null) {
			Chat.send(context, "Server was not found!");
			return Command.SINGLE_SUCCESS;
		}

		Chat.send(context, "Deleting server {0}...", server.getName());
		server.delete().whenComplete((__, error) -> {
			if (error != null) {
				error.printStackTrace();
				Chat.send(context, "Server {0} is unable to delete! {1}", server.getName(), error.getMessage());
			} else {
				Chat.send(context, "Server {0} deleted.", server.getName());
			}
		});
		return Command.SINGLE_SUCCESS;
	}
}