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
import com.velocitypowered.api.command.CommandSource;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;

public class CommandTestsuiteDisableIdleTimeout {

	private final ServerManager serverManager;

	public CommandTestsuiteDisableIdleTimeout(TestsuitePlugin plugin) {
		this.serverManager = plugin.getServerManager();
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("disableidletimeout")
				.then(
						argument("name", StringArgumentType.greedyString())
						.suggests(this::suggestServers)
						.executes(this::toggleIdleServer));
	}

	public int toggleIdleServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);

		if (server == null) {
			Chat.send(context, "Server was not found!");
			return Command.SINGLE_SUCCESS;
		}

		if (server.toggleIdleTimeout()) {
			Chat.send(context, "Idle timeout was enabled for {0}...", server.getName());
		} else {
			Chat.send(context, "Idle timeout was disabled for {0}...", server.getName());
		}
		return Command.SINGLE_SUCCESS;
	}

	public CompletableFuture<Suggestions> suggestServers(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		String input = builder.getRemaining().toLowerCase();
		this.serverManager.getServers().stream()
			.filter(server -> server.getStatus() == UtilizationState.RUNNING || server.getStatus() == UtilizationState.STARTING)
			.map(server -> server.getName())
			.filter(name -> name.toLowerCase().contains(input))
			.forEach(builder::suggest);
		return builder.buildFuture();
	}
}