package dev.imprex.testsuite.command;

import java.util.HashSet;
import java.util.Set;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.imprex.testsuite.api.TestsuiteSender;

public class CommandBuilder {

	public static CommandBuilder command(LiteralArgumentBuilder<TestsuiteSender> literal) {
		return new CommandBuilder(literal);
	}

	protected final LiteralArgumentBuilder<TestsuiteSender> literal;

	protected boolean isRoot = false;
	protected Set<String> aliases = new HashSet<>();

	private CommandBuilder(LiteralArgumentBuilder<TestsuiteSender> literal) {
		this.literal = literal;
	}

	public CommandBuilder alias(String alias, String... aliasArray) {
		this.aliases.add(alias);
		for (String aliasEntry : aliasArray) {
			this.aliases.add(aliasEntry);
		}
		return this;
	}

	public CommandBuilder asRoot() {
		this.isRoot = true;
		return this;
	}

	public CommandMeta build() {
		return new CommandMeta(this.literal, this.isRoot, this.aliases);
	}
}