package dev.imprex.testsuite.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.imprex.testsuite.api.TestsuiteSender;

public class ArgumentBuilder {

	public static LiteralArgumentBuilder<TestsuiteSender> literal(String name) {
		return LiteralArgumentBuilder.<TestsuiteSender>literal(name);
	}

	public static <T> RequiredArgumentBuilder<TestsuiteSender, T> argument(final String name,
			final ArgumentType<T> type) {
		return RequiredArgumentBuilder.<TestsuiteSender, T>argument(name, type);
	}

	public static String getSafeStringArgument(CommandContext<TestsuiteSender> context, String fieldName, String defaultValue) {
		String input;
		try {
			input = StringArgumentType.getString(context, fieldName);
		} catch (IllegalArgumentException e) {
			// Ignore missing argument exception and return default value
			return defaultValue;
		}
		return input;
	}
}