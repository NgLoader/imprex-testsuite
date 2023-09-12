package dev.imprex.testsuite.util;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;

public class ArgumentBuilder {

	public static LiteralArgumentBuilder<CommandSource> literal(String name) {
		return LiteralArgumentBuilder.<CommandSource>literal(name);
	}

    public static <T> RequiredArgumentBuilder<CommandSource, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.<CommandSource, T>argument(name, type);
    }
}