package dev.imprex.testsuite.command;

import java.util.Set;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.imprex.testsuite.util.TestsuiteSender;

public record CommandRegistration(LiteralArgumentBuilder<TestsuiteSender> literal, boolean isRoot, Set<String> aliases) {

}
