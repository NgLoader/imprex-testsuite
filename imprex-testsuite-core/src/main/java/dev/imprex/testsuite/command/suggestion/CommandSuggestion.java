
package dev.imprex.testsuite.command.suggestion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.common.ServerType;
import dev.imprex.testsuite.common.ServerVersionCache;
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.template.ServerTemplate;
import dev.imprex.testsuite.template.ServerTemplateList;
import dev.imprex.testsuite.util.TestsuitePlayer;
import dev.imprex.testsuite.util.TestsuiteSender;

public class CommandSuggestion {

	public static CompletableFuture<Suggestions> suggestContent(String input, SuggestionsBuilder builder, Stream<String> stream) {
		String[] keywords = input.toLowerCase().split("[-_. ]");
		stream
			.filter(name -> {
				for (String keyword : keywords) {
					if (!name.toLowerCase().contains(keyword)) {
						return false;
					}
				}
				return true;
			})
			.forEach(builder::suggest);
		return builder.buildFuture();
	}

	private final TestsuitePlugin plugin;
	private final ServerManager serverManager;
	private final ServerTemplateList templateList;
	private final ServerVersionCache versionCache;

	public CommandSuggestion(TestsuitePlugin plugin) {
		this.plugin = plugin;
		this.serverManager = plugin.getServerManager();
		this.templateList = plugin.getTemplateList();
		this.versionCache = plugin.getVersionCache();
	}
	public SuggestionBuilder<String, String> template(ServerType type) {
		return new SuggestionBuilder<>(() -> this.versionCache.getVersionList(type).stream());
	}

	public SuggestionBuilder<ServerTemplate, ServerTemplate> template() {
		return new SuggestionBuilder<>(() -> this.templateList.getTemplates().stream());
	}

	public ServerSuggestionBuilder server() {
		return new ServerSuggestionBuilder(() -> this.serverManager.getServers().stream());
	}

	public SuggestionBuilder<TestsuitePlayer, TestsuitePlayer> player() {
		return new SuggestionBuilder<>(() -> this.plugin.getPlayers().stream());
	}

	public Collection<TestsuitePlayer> readPlayers(CommandContext<TestsuiteSender> context, String fieldName) {
		String input = StringArgumentType.getString(context, fieldName);

		if (input.equalsIgnoreCase("@all")) {
			return this.plugin.getPlayers();
		}

		List<TestsuitePlayer> result = null;
		for (TestsuitePlayer player : this.plugin.getPlayers()) {
			if (player.getName().equalsIgnoreCase(input)) {
				if (result == null) {
					result = new ArrayList<>();
				}
				result.add(player);
			}
		}

		return result != null ? result : Collections.emptyList();
	}

	public ServerInstance readServer(CommandContext<TestsuiteSender> context, String fieldName) {
		String serverName = context.getArgument("name", String.class);
		return this.serverManager.getServer(serverName);
	}

//	public CompletableFuture<Suggestions> suggestPlayers(CommandContext<TestsuiteSender> context, SuggestionsBuilder builder) {
//		String input = builder.getRemaining().toLowerCase();
//
//		List<String> players = new ArrayList<>();
//		players.addAll(this.plugin.getPlayers().stream().map(TestsuiteSender::getName).toList());
//		players.add("@all");
//
//		return TestsuiteSuggestion.suggestContent(input, builder, players.stream());
//	}
//
//	public CompletableFuture<Suggestions> suggestServers(CommandContext<TestsuiteSender> context, SuggestionsBuilder builder) {
//		return this.suggestServers(context, builder, (__) -> true);
//	}
//
//	public CompletableFuture<Suggestions> suggestServers(CommandContext<TestsuiteSender> context, SuggestionsBuilder builder, Predicate<ServerInstance> filter) {
//		String input = builder.getRemaining().toLowerCase();
//		return TestsuiteSuggestion.suggestContent(
//				input,
//				builder,
//				this.serverManager.getServers().stream()
//					.filter(filter::test)
//					.map(server -> server.getName()));
//	}
//
//	public CompletableFuture<Suggestions> suggestTemplates(CommandContext<TestsuiteSender> context, SuggestionsBuilder builder) {
//		return SuggestionProvider.suggest(builder, this.templateList.getTemplates().stream()
//				.map(template -> template.getName())
//				.toList());
//	}
//
//	public CompletableFuture<Suggestions> suggestVersions(CommandContext<TestsuiteSender> context, SuggestionsBuilder builder) {
//		String name = context.getArgument("name", String.class);
//		ServerTemplate template = this.templateList.getTemplate(name);
//
//		ServerType serverType = ServerType.fromName(context.getArgument("type", String.class));
//		if (serverType == null) {
//			return builder.buildFuture();
//		}
//
//		String input = builder.getRemaining().toLowerCase();
//		this.versionCache.getVersionList(serverType).stream()
//			.filter(version -> version.startsWith(input) || version.contains(input))
//			.filter(version -> {
//				if (template == null) {
//					return true;
//				}
//
//				String serverName = String.format("%s-%s-%s", template.getName().toLowerCase(), serverType.name(), version);
//				return this.serverManager.getServer(serverName) == null;
//			})
//			.sorted(ServerVersion::compareVersion)
//			.forEachOrdered(builder::suggest);
//
//		return builder.buildFuture();
//	}
}
