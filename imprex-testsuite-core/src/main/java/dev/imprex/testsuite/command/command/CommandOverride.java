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
import dev.imprex.testsuite.override.OverrideException;
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;

public class CommandOverride {

	private final ServerManager serverManager;
	private final CommandSuggestion suggestion;

	public CommandOverride(TestsuitePlugin plugin) {
		this.serverManager = plugin.getServerManager();
		this.suggestion = plugin.getCommandSuggestion();
	}

	public LiteralArgumentBuilder<TestsuiteSender> create() {
		return literal("override").then(
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

		Chat.builder(server).append("Overriting server...").send(context);
		server.override().whenComplete((count, error) -> {
			if (error != null) {
				if (!(error instanceof OverrideException)) {
					error.printStackTrace();
				}
				Chat.builder(server).append("Server is unable to override values because: {0}", error.getMessage()).send(context);
			} else {
				Chat.builder(server).append("Server has changed {0} variables.", count).send(context);
			}
		});
		return Command.SINGLE_SUCCESS;
	}
}