package dev.imprex.testsuite.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.command.command.CommandConnect;
import dev.imprex.testsuite.command.command.CommandExecute;
import dev.imprex.testsuite.command.command.CommandReconnect;
import dev.imprex.testsuite.command.command.CommandRestart;
import dev.imprex.testsuite.command.command.CommandStop;
import dev.imprex.testsuite.command.command.CommandCreate;
import dev.imprex.testsuite.command.command.CommandDelete;
import dev.imprex.testsuite.command.command.CommandDisableIdleTimeout;
import dev.imprex.testsuite.command.command.CommandList;
import dev.imprex.testsuite.command.command.CommandReinstall;
import dev.imprex.testsuite.command.command.CommandSetup;
import dev.imprex.testsuite.command.command.CommandStart;

import static dev.imprex.testsuite.command.CommandBuilder.command;

public class CommandRegistry {

	private final Map<String, CommandRegistration> commands = new HashMap<>();

	public CommandRegistry(TestsuitePlugin plugin) {
		this.register(command(new CommandConnect(plugin).create())
				.alias("con", "tc")
				.asRoot());
		this.register(command(new CommandCreate(plugin).create()));
		this.register(command(new CommandDelete(plugin).create()));
		this.register(command(new CommandDisableIdleTimeout(plugin).create()));
		this.register(command(new CommandExecute(plugin).create()));
		this.register(command(new CommandList(plugin).create()));
		this.register(command(new CommandReconnect(plugin).create()));
		this.register(command(new CommandReinstall(plugin).create()));
		this.register(command(new CommandRestart(plugin).create()));
		this.register(command(new CommandSetup(plugin).create()));
		this.register(command(new CommandStart(plugin).create()));
		this.register(command(new CommandStop(plugin).create()));
	}

	public void register(CommandBuilder builder) {
		String command = builder.literal.getLiteral();
		if (this.commands.containsKey(command)) {
			throw new IllegalArgumentException("Duplicate command: " + command);
		}
		CommandRegistration registration = builder.build();
		this.commands.put(command, registration);

		for (String alias : builder.aliases) {
			if (this.commands.containsKey(alias)) {
				throw new IllegalArgumentException("Duplicate command alias: " + alias);
			}
			this.commands.put(alias, registration);
		}
	}

	public Map<String, CommandRegistration> getCommands() {
		return Collections.unmodifiableMap(this.commands);
	}
}