package dev.imprex.testsuite.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;

import dev.imprex.testsuite.TestsuitePlugin;

public class CommandTestsuite {

	private final TestsuitePlugin plugin;

	public CommandTestsuite(TestsuitePlugin plugin) {
		this.plugin = plugin;

		this.register();
	}

	public void register() {
		CommandManager commandManager = this.plugin.getProxy().getCommandManager();
		CommandMeta commandMeta = commandManager.metaBuilder("testsuite")
				.aliases("test", "ts")
				.plugin(this)
				.build();

		BrigadierCommand command = new BrigadierCommand(this.create());
		commandManager.register(commandMeta, command);
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("testsuite")
				.then(CommandConnect.COMMAND)
				.then(CommandExecute.COMMAND)
				.then(CommandReconnect.COMMAND)
				.then(CommandRestart.COMMAND)
				.then(CommandStop.COMMAND)
				.then(new CommandTestsuiteCreate(this.plugin).create())
				.then(new CommandTestsuiteDelete(this.plugin).create())
				.then(new CommandTestsuiteDisableIdleTimeout(this.plugin).create())
				.then(new CommandTestsuiteList(this.plugin).create())
				.then(new CommandTestsuiteReinstall(this.plugin).create())
				.then(new CommandTestsuiteSetup(this.plugin).create())
				.then(new CommandTestsuiteStart(this.plugin).create());
	}
}
