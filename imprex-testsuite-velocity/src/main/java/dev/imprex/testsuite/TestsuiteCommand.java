package dev.imprex.testsuite;

import static dev.imprex.testsuite.util.ArgumentBuilder.argument;
import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mattmalec.pterodactyl4j.application.entities.PteroApplication;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.CommandSource;

import dev.imprex.testsuite.common.SuggestionProvider;
import net.kyori.adventure.text.Component;

public class TestsuiteCommand {

	private static final List<String> SERVER_VERSION = List.of("1.20.1");
	private static final List<String> SERVER_TYPE = List.of("spigot", "paper", "");

	private final PteroApplication pteroApplication;
	private final PteroClient pteroClient;
	private final PteroServerCache serverCache;

	public TestsuiteCommand(TestsuitePlugin plugin) {
		this.pteroApplication = plugin.getPteroApplication();
		this.pteroClient = plugin.getPteroClient();
		this.serverCache = plugin.getServerCache();
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("testsuite")
				.requires(source -> source.hasPermission("testsuite"))
				.then(
						literal("create").then(
								argument("name", StringArgumentType.word())
								.suggests(this::suggestServers)
								.then(
										argument("version", StringArgumentType.word())
										.then(
												argument("type", StringArgumentType.word())
												.executes(this::createServer)
												)
										)
								)
						)
				.then(
						literal("start").then(
								argument("name", StringArgumentType.greedyString())
								.suggests(this::suggestServers)
								.executes(this::startServer))
						.executes(context -> {
							// SYNTAX
							return 0;
						}))
				.then(
						literal("stop").then(
								argument("name", StringArgumentType.greedyString())
								.suggests(this::suggestServers)
								.executes(this::stopServer))
						.executes(context -> {
							// SYNTAX
							return 0;
						}))
				.then(
						literal("list"))
				.executes(context -> {
					// will be removed to support bukkit commands under same alias
					// SYNTAX
					return 0;
				});
	}

	public CompletableFuture<Suggestions> suggestServers(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		return SuggestionProvider.suggest(builder, this.serverCache.getServers().stream()
				.map(server -> server.getName())
				.toList());
	}

	public int startServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		ClientServer server = serverCache.getServer(serverName);
		if (server == null) {
			context.getSource().sendMessage(Component.text("No server found"));
			return Command.SINGLE_SUCCESS;
		}

		context.getSource().sendMessage(Component.text("Try starting -> " + server.getName()));
		server.start().executeAsync((__) -> {
			context.getSource().sendMessage(Component.text("Starting -> " + server.getName()));
		}, (___) -> {
			context.getSource().sendMessage(Component.text("Unable to start server -> " + server.getName()));
		});
		return Command.SINGLE_SUCCESS;
	}

	public int stopServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		ClientServer server = serverCache.getServer(serverName);
		if (server == null) {
			context.getSource().sendMessage(Component.text("No server found"));
			return Command.SINGLE_SUCCESS;
		}

		context.getSource().sendMessage(Component.text("Try stopping -> " + server.getName()));
		server.stop().executeAsync((__) -> {
			context.getSource().sendMessage(Component.text("Stopping -> " + server.getName()));
		}, (___) -> {
			context.getSource().sendMessage(Component.text("Unable to stop server -> " + server.getName()));
		});
		return Command.SINGLE_SUCCESS;
	}

	public int createServer(CommandContext<CommandSource> context) {
		return Command.SINGLE_SUCCESS;
	}
}