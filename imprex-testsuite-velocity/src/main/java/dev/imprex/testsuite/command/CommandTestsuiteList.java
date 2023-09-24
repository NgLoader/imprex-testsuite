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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.common.SuggestionProvider;
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.template.ServerTemplate;
import dev.imprex.testsuite.template.ServerTemplateList;
import dev.imprex.testsuite.util.Chat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class CommandTestsuiteList {

	private final ServerTemplateList templateList;
	private final ServerManager serverManager;

	public CommandTestsuiteList(TestsuitePlugin plugin) {
		this.templateList = plugin.getTemplateList();
		this.serverManager = plugin.getServerManager();
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("list").then(
				argument("name", StringArgumentType.greedyString())
				.suggests(this::suggestTemplates)
				.executes(this::listServer))
			.executes(this::listServer);
	}

	public int listServer(CommandContext<CommandSource> context) {
		ServerTemplate template = null;
		if (context.getArguments().containsKey("name")) {
			String name = context.getArgument("name", String.class);
			template = this.templateList.getTemplate(name);

			if (template == null) {
				Chat.send(context, "Unable to find template {0}", name);
				return Command.SINGLE_SUCCESS;
			}
		}

		Component component = Chat.PREFIX
				.append(Component.text("[] --- --- { Server List } --- --- []"))
				.append(Component.newline())
				.append(Component.newline());

		for (ServerInstance server : this.serverManager.getServers().stream()
				.sorted((a, b) -> a.getName().compareTo(b.getName()))
				.toList()) {

			if (template != null && !template.equals(server.getTemplate())) {
				continue;
			}

			Component serverInfo = Component.text("  - ")
					.color(Chat.Color.GRAY)
					.append(Component.text(server.getName())
							.color(Chat.Color.statusColor(server)));

			Component serverAction;
			if (server.getStatus() == UtilizationState.OFFLINE) {
				serverAction = Component.text("Start")
						.color(Chat.Color.LIGHT_GREEN)
						.clickEvent(ClickEvent.suggestCommand("/testsuite start " + server.getName()));
			} else if (server.getStatus() != UtilizationState.STOPPING) {
				Component playerCount = Component.text("(")
						.color(Chat.Color.GRAY)
						.append(Component.text(server.getCurrentServer().getPlayersConnected().size())
								.color(Chat.Color.LIGHT_GREEN)
								.hoverEvent(HoverEvent.showText(Component.text("Player count")
										.color(Chat.Color.LIGHT_GREEN))))
						.append(Component.text(" | "))
						.append(Component.text(milliToSeconds(server.getInactiveTime()) + "s")
								.color(Chat.Color.DARK_GREEN)
								.hoverEvent(HoverEvent.showText(Component.text("Inactive time")
										.color(Chat.Color.DARK_GREEN))))
						.append(Component.text(")")
								.color(Chat.Color.GRAY));

				Component connectServer = Component.text("Connect")
						.color(Chat.Color.DARK_GREEN)
						.clickEvent(ClickEvent.runCommand("/connect " + server.getName()));

				serverAction = Component.text("")
						.append(playerCount)
						.appendSpace()
						.append(
							Component.text("Stop")
							.color(Chat.Color.RED)
							.clickEvent(ClickEvent.suggestCommand("/testsuite stop " + server.getName())));

				if (context.getSource() instanceof Player player) {
					ServerConnection serverConnection = player.getCurrentServer().orElseGet(() -> null);
					if (serverConnection == null ||
							!serverConnection.getServerInfo().equals(server.getCurrentServer().getServerInfo())) {
						serverAction = serverAction
								.appendSpace()
								.append(connectServer);
					}
				} else {
					serverAction = serverAction
							.appendSpace()
							.append(connectServer);
				}
			} else {
				serverAction = Component.empty();
			}

			component = component
					.append(serverInfo)
					.appendSpace()
					.append(serverAction)
					.appendNewline();
		}

		context.getSource().sendMessage(component
				.append(Component.newline())
				.append(Chat.PREFIX)
				.append(Component.text("[] --- --- { Server List } --- --- []")));
		return Command.SINGLE_SUCCESS;
	}

	public int milliToSeconds(long time) {
		return (int) ((time - System.currentTimeMillis()) / 1000);
	}

	public CompletableFuture<Suggestions> suggestTemplates(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
		return SuggestionProvider.suggest(builder, this.templateList.getTemplates().stream()
				.map(template -> template.getName())
				.toList());
	}
}