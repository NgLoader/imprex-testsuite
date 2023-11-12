package dev.imprex.testsuite.bungeecord;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.api.TestsuiteSender;
import dev.imprex.testsuite.command.CommandRegistry;
import dev.imprex.testsuite.util.Chat;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeecordCommand extends Command implements TabExecutor {

	public static TestsuiteSender toTestsuiteSender(CommandSender source) {
		if (source instanceof ProxiedPlayer player) {
			return BungeecordPlayer.get(player);
		} else {
			return message -> BungeecordPlugin.audiences.sender(source).sendMessage(message);
		}
	}

	private final Function<String, String> prefix;

	private CommandRegistry commandRegistry;
	private CommandDispatcher<TestsuiteSender> dispatcher;

	public BungeecordCommand(TestsuitePlugin plugin, Function<String, String> prefix, String command, String... aliases) {
		super(command, "A testsuite command", aliases);
		this.prefix = prefix;
		this.commandRegistry = plugin.getCommandRegistry();
		this.dispatcher = this.commandRegistry.getDispatcher();
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		TestsuiteSender testsuiteSender = BungeecordCommand.toTestsuiteSender(sender);

		try {
			this.dispatcher.execute(this.getCommand(args).trim(), testsuiteSender);
		} catch (CommandSyntaxException e) {
			// Ignore syntax exceptions
		} catch (Exception e) {
			e.printStackTrace();
			Chat.send(testsuiteSender, Component.text("Error occurred by executing the command!"));
		}
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		TestsuiteSender testsuiteSender = BungeecordCommand.toTestsuiteSender(sender);
		StringReader cursor = new StringReader(this.getCommand(args));
		if (cursor.canRead() && cursor.peek() == '/') {
			cursor.skip();
		}

		ParseResults<TestsuiteSender> result = this.dispatcher.parse(cursor, testsuiteSender);
		CompletableFuture<Suggestions> suggestions = this.dispatcher.getCompletionSuggestions(result);

		try {
			return suggestions.get().getList().stream()
					.map(suggestion -> suggestion.getText())
					.toList();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	public String getCommand(String[] args) {
		return this.prefix.apply(String.join(" ", args));
	}
}
