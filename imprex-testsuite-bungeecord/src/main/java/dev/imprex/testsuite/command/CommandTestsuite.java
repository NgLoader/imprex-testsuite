package dev.imprex.testsuite.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.command.brigadier.BrigadierCommand;
import net.md_5.bungee.api.CommandSender;

public class CommandTestsuite {

	private final TestsuitePlugin plugin;

	public CommandTestsuite(TestsuitePlugin plugin) {
		this.plugin = plugin;
	}

	public BrigadierCommand brigadierCommand() {
		return new BrigadierCommand(this.create(), "ts");
	}

	public LiteralArgumentBuilder<CommandSender> create() {
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
