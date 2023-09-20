package dev.imprex.testsuite;

import static dev.imprex.testsuite.util.ArgumentBuilder.argument;
import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mattmalec.pterodactyl4j.UtilizationState;
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
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.template.ServerTemplate;
import dev.imprex.testsuite.template.ServerTemplateList;
import net.kyori.adventure.text.Component;

public class TestsuiteCommand {

	private final ProxyServer proxy;
	private final ServerVersionCache versionCache;
	private final ServerTemplateList templateList;
	private final ServerManager serverManager;

	public TestsuiteCommand(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.versionCache = plugin.getVersionCache();
		this.templateList = plugin.getTemplateList();
		this.serverManager = plugin.getServerManager();
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("testsuite")
				.requires(source -> source.hasPermission("testsuite"))
				.then(
						literal("create").then(
								argument("name", StringArgumentType.word())
								.suggests(this::suggestTemplates)
								.then(
										argument("type", StringArgumentType.word())
										.suggests((future, builder) -> SuggestionProvider.suggest(builder, ServerType.TYPE_NAMES))
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
								.suggests(this::suggestStartedServers)
								.executes(this::stopServer))
						.executes(context -> {
							// SYNTAX
							return 0;
						}))
				.then(
						literal("setup").then(
								argument("name", StringArgumentType.greedyString())
								.suggests(this::suggestServers)
								.executes(this::setupServer))
						.executes(context -> {
							// SYNTAX
							return 0;
						}))
				.then(
						literal("connect").then(
								argument("name", StringArgumentType.greedyString())
								.suggests(this::suggestStartedServers)
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

	public CompletableFuture<Suggestions> suggestServers(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		return SuggestionProvider.suggest(builder, this.serverManager.getServers().stream()
				.map(server -> server.getName())
				.toList());
	}

	public CompletableFuture<Suggestions> suggestStartedServers(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		return SuggestionProvider.suggest(builder, this.serverManager.getServers().stream()
				.filter(server -> server.getStatus() == UtilizationState.RUNNING)
				.map(server -> server.getName())
				.toList());
	}

	public CompletableFuture<Suggestions> suggestTemplates(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		return SuggestionProvider.suggest(builder, this.templateList.getTemplates().stream()
				.map(template -> template.getName())
				.toList());
	}

	public CompletableFuture<Suggestions> suggestVersions(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		String name = context.getArgument("name", String.class);
		ServerTemplate template = this.templateList.getTemplate(name);

		ServerType serverType = ServerType.fromName(context.getArgument("type", String.class));
		if (serverType == null) {
			return builder.buildFuture();
		}

		String input = builder.getRemaining().toLowerCase();
		this.versionCache.getVersionList(serverType).stream()
			.filter(version -> version.startsWith(input) || version.contains(input))
			.filter(version -> {
				if (template == null) {
					return true;
				}

				String serverName = String.format("%s-%s-%s", template.getName().toLowerCase(), serverType.name(), version);
				return this.serverManager.getServer(serverName) == null;
			})
			.sorted(ServerVersion::compareVersion)
			.forEachOrdered(builder::suggest);

		return builder.buildFuture();
	}

	public int startServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		if (server == null) {
			context.getSource().sendMessage(Component.text("No server found"));
			return Command.SINGLE_SUCCESS;
		}

		context.getSource().sendMessage(Component.text("Try starting -> " + server.getName()));
		server.start().whenComplete((__, error) -> {
			if (error != null) {
				context.getSource().sendMessage(Component.text("Unable to start server -> " + server.getName()));
			} else {
				context.getSource().sendMessage(Component.text("Starting -> " + server.getName()));
			}
		});
		return Command.SINGLE_SUCCESS;
	}

	public int stopServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		if (server == null) {
			context.getSource().sendMessage(Component.text("No server found"));
			return Command.SINGLE_SUCCESS;
		}

		context.getSource().sendMessage(Component.text("Try stopping -> " + server.getName()));
		server.stop().whenComplete((__, error) -> {
			if (error != null) {
				context.getSource().sendMessage(Component.text("Unable to stop server -> " + server.getName()));
			} else {
				context.getSource().sendMessage(Component.text("Stopping -> " + server.getName()));
			}
		});
		return Command.SINGLE_SUCCESS;
	}

	public int setupServer(CommandContext<CommandSource> context) {
		String serverName = context.getArgument("name", String.class);
		ServerInstance server = this.serverManager.getServer(serverName);
		if (server == null) {
			context.getSource().sendMessage(Component.text("No server found"));
			return Command.SINGLE_SUCCESS;
		}

		context.getSource().sendMessage(Component.text("Try setup -> " + server.getName()));
		server.setupServer().whenComplete((__, error) -> {
			if (error != null) {
				error.printStackTrace();
				context.getSource().sendMessage(Component.text("Unable to setup server -> " + server.getName()));
			} else {
				context.getSource().sendMessage(Component.text("Setup successful -> " + server.getName()));
			}
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

		ServerTemplate template = this.templateList.getTemplate(name);

		ServerType serverType = ServerType.fromName(context.getArgument("type", String.class));
		if (serverType == null) {
			context.getSource().sendMessage(Component.text("Invalid server type"));
			return Command.SINGLE_SUCCESS;
		}

		String version = context.getArgument("version", String.class);
		if (!this.versionCache.getVersionList(serverType).contains(version)) {
			context.getSource().sendMessage(Component.text("Invalid version"));
		}

		context.getSource().sendMessage(Component.text("Creating server " + (template != null ? "template " + template.getName() : name) + "..."));
		if (template != null) {
			String serverName = String.format("%s-%s-%s", template.getName(), serverType.name().toLowerCase(), version);
			if (this.serverManager.getServer(serverName) != null) {
				context.getSource().sendMessage(Component.text("Template aready exist!"));
				return Command.SINGLE_SUCCESS;
			}

			this.serverManager.create(template, serverType, version).whenComplete((__, error) -> {
				if (error != null) {
					error.printStackTrace();
					context.getSource().sendMessage(Component.text("Created failed! " + error.getMessage()));
					return;
				}

				context.getSource().sendMessage(Component.text("Created success"));
			});
		} else {
			this.serverManager.create(name, serverType, version).whenComplete((__, error) -> {
				if (error != null) {
					error.printStackTrace();
					context.getSource().sendMessage(Component.text("Created failed! " + error.getMessage()));
					return;
				}

				context.getSource().sendMessage(Component.text("Created success"));
			});
		}
		return Command.SINGLE_SUCCESS;
	}
}