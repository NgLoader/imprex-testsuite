package dev.imprex.testsuite.velocity;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.api.TestsuiteSender;
import dev.imprex.testsuite.command.CommandRegistry;
import dev.imprex.testsuite.util.Chat;
import net.kyori.adventure.text.Component;

public class VelocityCommand implements SimpleCommand {

	public static TestsuiteSender toTestsuiteSender(CommandSource source) {
		if (source instanceof Player player) {
			return VelocityPlayer.get(player);
		} else {
			return message -> source.sendMessage(message);
		}
	}

	private final Function<String, String> prefix;

	private CommandRegistry commandRegistry;
	private CommandDispatcher<TestsuiteSender> dispatcher;

	public VelocityCommand(TestsuitePlugin plugin, Function<String, String> prefix) {
		this.prefix = prefix;
		this.commandRegistry = plugin.getCommandRegistry();
		this.dispatcher = this.commandRegistry.getDispatcher();
	}

	@Override
	public void execute(Invocation invocation) {
		TestsuiteSender sender = VelocityCommand.toTestsuiteSender(invocation.source());

		try {
			this.dispatcher.execute(this.getCommand(invocation.arguments()).trim(), sender);
		} catch (CommandSyntaxException e) {
			// Ignore syntax exceptions
		} catch (Exception e) {
			e.printStackTrace();
			Chat.send(sender, Component.text("Error occurred by executing the command!"));
		}
	}

	@Override
	public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
		TestsuiteSender sender = VelocityCommand.toTestsuiteSender(invocation.source());
		StringReader cursor = new StringReader(this.getCommand(invocation.arguments()));
		if (cursor.canRead() && cursor.peek() == '/') {
			cursor.skip();
		}

		CompletableFuture<List<String>> completableFuture = new CompletableFuture<>();

		ParseResults<TestsuiteSender> result = this.dispatcher.parse(cursor, sender);
		this.dispatcher.getCompletionSuggestions(result).whenComplete((suggestions, error) -> {
			if (error != null) {
				completableFuture.completeExceptionally(error);
				return;
			}

			completableFuture.complete(suggestions.getList().stream()
					.map(suggestion -> suggestion.getText())
					.toList());
		});

		return completableFuture;
	}

	public String getCommand(String[] args) {
		return this.prefix.apply(String.join(" ", args));
	}
}
