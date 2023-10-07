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

public class CommandDisableIdleTimeout {

	private final ServerManager serverManager;
	private final CommandSuggestion suggestion;

	public CommandDisableIdleTimeout(TestsuitePlugin plugin) {
		this.serverManager = plugin.getServerManager();
		this.suggestion = plugin.getCommandSuggestion();
	}

	public LiteralArgumentBuilder<TestsuiteSender> create() {
		return literal("disableidletimeout")
				.then(
						argument("name", StringArgumentType.greedyString())
						.suggests(this.suggestion.server()
								.hasStatus(UtilizationState.STARTING, UtilizationState.RUNNING)
								.buildSuggest())
						.executes(this::toggleIdleServer));
	}

	public int toggleIdleServer(CommandContext<TestsuiteSender> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);

		if (server == null) {
			Chat.send(context, "Server was not found!");
			return Command.SINGLE_SUCCESS;
		}

		if (server.getTemplate() == null) {
			Chat.send(context, "Only template server have a idle timeout!");
			return Command.SINGLE_SUCCESS;
		}

		if (server.toggleIdleTimeout()) {
			server.resetInactiveTime();
			Chat.send(context, "Idle timeout was enabled for {0}", server.getName());
		} else {
			Chat.send(context, "Idle timeout was disabled for {0}", server.getName());
		}
		return Command.SINGLE_SUCCESS;
	}
}