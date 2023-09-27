package dev.imprex.testsuite.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.argument;
import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.common.ServerType;
import dev.imprex.testsuite.common.ServerVersion;
import dev.imprex.testsuite.common.ServerVersionCache;
import dev.imprex.testsuite.common.SuggestionProvider;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.template.ServerTemplate;
import dev.imprex.testsuite.template.ServerTemplateList;
import dev.imprex.testsuite.util.Chat;
import net.md_5.bungee.api.CommandSender;

public class CommandTestsuiteCreate {

	private final ServerVersionCache versionCache;
	private final ServerTemplateList templateList;
	private final ServerManager serverManager;

	public CommandTestsuiteCreate(TestsuitePlugin plugin) {
		this.versionCache = plugin.getVersionCache();
		this.templateList = plugin.getTemplateList();
		this.serverManager = plugin.getServerManager();
	}

	public LiteralArgumentBuilder<CommandSender> create() {
		return literal("create").then(
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
				);
	}

	public int createServer(CommandContext<CommandSender> context) {
		String name = context.getArgument("name", String.class);

		ServerTemplate template = this.templateList.getTemplate(name);

		ServerType serverType = ServerType.fromName(context.getArgument("type", String.class));
		if (serverType == null) {
			Chat.send(context, "Invalid server type");
			return Command.SINGLE_SUCCESS;
		}

		String version = context.getArgument("version", String.class);
		if (!this.versionCache.getVersionList(serverType).contains(version)) {
			Chat.send(context, "Invalid version");
		}

		Chat.send(context, "Creating server " + (template != null ? "template " + template.getName() : name) + "...");
		if (template != null) {
			String serverName = String.format("%s-%s-%s", template.getName(), serverType.name().toLowerCase(), version);
			if (this.serverManager.getServer(serverName) != null) {
				Chat.send(context, "Template aready exist!");
				return Command.SINGLE_SUCCESS;
			}

			this.serverManager.create(template, serverType, version).whenComplete((__, error) -> {
				if (error != null) {
					error.printStackTrace();
					Chat.send(context, "Created failed! " + error.getMessage());
					return;
				}

				Chat.send(context, "Created success");
			});
		} else {
			this.serverManager.create(name, serverType, version).whenComplete((__, error) -> {
				if (error != null) {
					error.printStackTrace();
					Chat.send(context, "Created failed! " + error.getMessage());
					return;
				}

				Chat.send(context, "Created success");
			});
		}
		return Command.SINGLE_SUCCESS;
	}

	public CompletableFuture<Suggestions> suggestTemplates(CommandContext<CommandSender> context, SuggestionsBuilder builder) {
		return SuggestionProvider.suggest(builder, this.templateList.getTemplates().stream()
				.map(template -> template.getName())
				.toList());
	}

	public CompletableFuture<Suggestions> suggestVersions(CommandContext<CommandSender> context, SuggestionsBuilder builder) {
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
}