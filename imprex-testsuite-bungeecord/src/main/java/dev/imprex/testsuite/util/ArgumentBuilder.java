package dev.imprex.testsuite.util;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.md_5.bungee.api.CommandSender;

public class ArgumentBuilder {

	public static LiteralArgumentBuilder<CommandSender> literal(String name) {
		return LiteralArgumentBuilder.<CommandSender>literal(name);
	}

    public static <T> RequiredArgumentBuilder<CommandSender, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.<CommandSender, T>argument(name, type);
    }
}