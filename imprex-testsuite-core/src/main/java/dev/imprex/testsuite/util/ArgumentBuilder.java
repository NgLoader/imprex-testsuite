package dev.imprex.testsuite.util;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public class ArgumentBuilder {

	public static LiteralArgumentBuilder<TestsuiteSender> literal(String name) {
		return LiteralArgumentBuilder.<TestsuiteSender>literal(name);
	}

	public static <T> RequiredArgumentBuilder<TestsuiteSender, T> argument(final String name,
			final ArgumentType<T> type) {
		return RequiredArgumentBuilder.<TestsuiteSender, T>argument(name, type);
	}
}