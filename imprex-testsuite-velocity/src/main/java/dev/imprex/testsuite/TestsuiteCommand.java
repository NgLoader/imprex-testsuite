package dev.imprex.testsuite;

import static dev.imprex.testsuite.util.ArgumentBuilder.argument;
import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mattmalec.pterodactyl4j.application.managers.ServerCreationAction;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import dev.imprex.testsuite.common.ServerType;
import dev.imprex.testsuite.common.ServerVersion;
import dev.imprex.testsuite.common.ServerVersionCache;
import dev.imprex.testsuite.common.SuggestionProvider;
import dev.imprex.testsuite.server.PteroServerCache;
import net.kyori.adventure.text.Component;

public class TestsuiteCommand {

	private final ProxyServer proxy;
	private final PteroServerCache serverCache;
	private final ServerVersionCache versionCache;

	public TestsuiteCommand(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.serverCache = plugin.getServerCache();
		this.versionCache = plugin.getVersionCache();
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("testsuite")
				.requires(source -> source.hasPermission("testsuite"))
				.then(
						literal("create").then(
								argument("name", StringArgumentType.word())
								.suggests(this::suggestServers)
								.then(
										argument("type", StringArgumentType.word())
										.suggests((future, builder) -> SuggestionProvider.suggest(builder, ServerType.TYPES))
										.then(
												argument("version", StringArgumentType.word())
												.suggests(this::suggestVersions)
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
						literal("connect").then(
								argument("name", StringArgumentType.greedyString())
								.suggests(this::suggestServerInfos)
								.executes(this::connectServer))
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

	public CompletableFuture<Suggestions> suggestServerInfos(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		return SuggestionProvider.suggest(builder, this.proxy.getAllServers().stream().map(server -> server.getServerInfo().getName()));
	}

	public CompletableFuture<Suggestions> suggestServers(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		return SuggestionProvider.suggest(builder, this.serverCache.getServers().stream()
				.map(server -> server.getName())
				.toList());
	}

	public CompletableFuture<Suggestions> suggestVersions(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		ServerType serverType = ServerType.fromName(context.getArgument("type", String.class));
		if (serverType == null) {
			return builder.buildFuture();
		}

		String input = builder.getRemaining().toLowerCase();
		this.versionCache.getVersionList(serverType).stream()
			.filter(version -> version.startsWith(input))
			.sorted(ServerVersion::compareVersion)
			.forEachOrdered(builder::suggest);

		return builder.buildFuture();
	}

	public int startServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		ClientServer server = serverCache.getServer(serverName);
		if (server == null) {
			context.getSource().sendMessage(Component.text("No server found"));
			return Command.SINGLE_SUCCESS;
		}

		context.getSource().sendMessage(Component.text("Try starting -> " + server.getName()));
		server.start().executeAsync(__ -> {
			context.getSource().sendMessage(Component.text("Starting -> " + server.getName()));
		}, __ -> {
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
		server.stop().executeAsync(__ -> {
			context.getSource().sendMessage(Component.text("Stopping -> " + server.getName()));
		}, __ -> {
			context.getSource().sendMessage(Component.text("Unable to stop server -> " + server.getName()));
		});
		return Command.SINGLE_SUCCESS;
	}

	public int connectServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		Optional<RegisteredServer> server = this.proxy.getServer(serverName);
		if (server.isEmpty()) {
			context.getSource().sendMessage(Component.text("No server found -> " + serverName));
			return Command.SINGLE_SUCCESS;
		}

		((Player) context.getSource()).createConnectionRequest(server.get()).connectWithIndication();
		context.getSource().sendMessage(Component.text("Sending to -> " + server.get().getServerInfo().getName()));
		return Command.SINGLE_SUCCESS;
	}

	public int createServer(CommandContext<CommandSource> context) {
		String name = context.getArgument("name", String.class);

		ServerType serverType = ServerType.fromName(context.getArgument("type", String.class));
		if (serverType == null) {
			context.getSource().sendMessage(Component.text("Invalid server type"));
			return Command.SINGLE_SUCCESS;
		}

		String version = context.getArgument("version", String.class);
		if (!this.versionCache.getVersionList(serverType).contains(version)) {
			context.getSource().sendMessage(Component.text("Invalid version"));
		}

		ServerCreationAction action = this.serverCache.createServer(name, serverType, version);
		if (action == null) {
			context.getSource().sendMessage(Component.text("nope"));
			return Command.SINGLE_SUCCESS;
		}

		action.executeAsync(__ -> {
			context.getSource().sendMessage(Component.text("Created success"));
		}, __ -> {
			context.getSource().sendMessage(Component.text("Created failed"));
		});
		return Command.SINGLE_SUCCESS;
	}
}