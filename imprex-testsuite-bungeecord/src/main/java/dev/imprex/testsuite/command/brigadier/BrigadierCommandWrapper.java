package dev.imprex.testsuite.command.brigadier;

import com.google.common.base.Joiner;
import com.mojang.brigadier.tree.CommandNode;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class BrigadierCommandWrapper extends Command {

	private final BrigadierCommandDispatcher dispatcher;

	public BrigadierCommandWrapper(BrigadierCommandDispatcher dispatcher, CommandNode<CommandSender> command, String[] aliases) {
		super(command.getName(), null, aliases);
		this.dispatcher = dispatcher;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		this.dispatcher.executeCommand(sender, this.toDispatcher(args, this.getName()), this.toDispatcher(args, this.getName()), true);
	}

	private String toDispatcher(String[] args, String name) {
		return name + (args.length > 0 ? " " + Joiner.on(' ').join(args) : "");
	}
}