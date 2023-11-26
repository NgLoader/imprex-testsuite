package de.imprex.testsuite.local;

import java.util.Scanner;

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

	private Scanner scanner = new Scanner(System.in);

	public LocalCommand(TestsuitePlugin plugin) {
		this.dispatcher = plugin.getCommandRegistry().getDispatcher();
	}

	public void readConsole() {
		System.out.print("Command: ");
		String line = scanner.nextLine();
		if (line.equalsIgnoreCase("exit")) {
			System.out.println("Shutdown started...");

			LocalApi.RUNNING = false;
			LocalApi.RUNNING_THREADS
					.stream()
					.filter(Thread::isAlive)
					.filter(thread -> !thread.isInterrupted())
					.forEach(Thread::interrupt);

			System.out.println("Bye.");
			System.exit(0);
			return;
		}

		this.execute(line);
		this.readConsole();
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
