
package dev.imprex.testsuite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TestsuiteSuggestion {

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

	private final ProxyServer proxy;
	private final ServerManager serverManager;

	public TestsuiteSuggestion(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.serverManager = plugin.getServerManager();
	}

	public Collection<ProxiedPlayer> readPlayers(CommandContext<CommandSender> context, String fieldName) {
		String input = StringArgumentType.getString(context, fieldName);

		if (input.equalsIgnoreCase("@all")) {
			return this.proxy.getPlayers();
		}

		List<ProxiedPlayer> result = null;
		for (ProxiedPlayer player : this.proxy.getPlayers()) {
			if (player.getName().equalsIgnoreCase(input)) {
				if (result == null) {
					result = new ArrayList<>();
				}
				result.add(player);
			}
		}

		return result != null ? result : Collections.emptyList();
	}

	public CompletableFuture<Suggestions> suggestPlayers(CommandContext<CommandSender> context, SuggestionsBuilder builder) {
		String input = builder.getRemaining().toLowerCase();

		List<String> players = new ArrayList<>();
		players.addAll(this.proxy.getPlayers().stream().map(ProxiedPlayer::getName).toList());
		players.add("@all");

		return TestsuiteSuggestion.suggestContent(input, builder, players.stream());
	}

	public ServerInstance readServer(CommandContext<CommandSender> context, String fieldName) {
		String serverName = context.getArgument("name", String.class);
		return this.serverManager.getServer(serverName);
	}

	public CompletableFuture<Suggestions> suggestServers(CommandContext<CommandSender> context, SuggestionsBuilder builder, Predicate<ServerInstance> filter) {
		String input = builder.getRemaining().toLowerCase();
		return TestsuiteSuggestion.suggestContent(
				input,
				builder,
				this.serverManager.getServers().stream()
					.filter(filter::test)
					.map(server -> server.getName()));
	}
}
