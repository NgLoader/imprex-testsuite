package de.imprex.testsuite.local;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.api.TestsuiteSender;
import dev.imprex.testsuite.util.Chat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class LocalCommand {

	private static final TestsuiteSender DUMMY_PLAYER = (message) -> TestsuiteLogger.info("CONSOLE: " + PlainTextComponentSerializer.plainText().serialize(message));

	private CommandDispatcher<TestsuiteSender> dispatcher;

	public LocalCommand(TestsuitePlugin plugin) {
		this.dispatcher = plugin.getCommandRegistry().getDispatcher();
	}

	public void execute(String input) {
		try {
			this.dispatcher.execute(input, DUMMY_PLAYER);
		} catch (CommandSyntaxException e) {
			// Ignore syntax exceptions
		} catch (Exception e) {
			e.printStackTrace();
			Chat.send(DUMMY_PLAYER, Component.text("Error occurred by executing the command!"));
		}
	}
}
