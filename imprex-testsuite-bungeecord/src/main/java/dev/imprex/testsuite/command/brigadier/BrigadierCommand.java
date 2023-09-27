package dev.imprex.testsuite.command.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.md_5.bungee.api.CommandSender;

public record BrigadierCommand(LiteralArgumentBuilder<CommandSender> command, String... aliases) {
}