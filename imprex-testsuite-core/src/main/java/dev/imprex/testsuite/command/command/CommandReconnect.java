package dev.imprex.testsuite.command.command;

import static dev.imprex.testsuite.command.ArgumentBuilder.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.api.TestsuiteSender;
import dev.imprex.testsuite.api.TestsuiteServer;
import dev.imprex.testsuite.util.Chat;

public class CommandReconnect {

	private final TestsuitePlugin plugin;

	public CommandReconnect(TestsuitePlugin plugin) {
		this.plugin = plugin;
	}

	public LiteralArgumentBuilder<TestsuiteSender> create() {
		return literal("reconnect")
				.requires(source -> source instanceof TestsuitePlayer)
				.executes(this::reconnect);
	}

	public int reconnect(CommandContext<TestsuiteSender> context) {
		TestsuitePlayer player = (TestsuitePlayer) context.getSource();

		TestsuiteServer lobby = this.plugin.getServer("lobby");
		if (lobby == null) {
			Chat.builder().append("Unable to find lobby server!").send(player);
			return Command.SINGLE_SUCCESS;
		}

		TestsuiteServer serverConnection = player.getServer();
		if (serverConnection == null) {
			Chat.builder().append("Unable to find current server!").send(player);
			return Command.SINGLE_SUCCESS;
		}

		if (lobby.equals(serverConnection)) {
			Chat.builder().append("You can only reconnect on non lobby servers!").send(player);
			return Command.SINGLE_SUCCESS;
		}

		this.reconnectPlayer(player, lobby, serverConnection);
		return Command.SINGLE_SUCCESS;
	}

	public void reconnectPlayer(TestsuitePlayer player, TestsuiteServer lobby, TestsuiteServer current) {
		player.connect(lobby != null ? lobby : current).whenComplete((result, error) -> {
			if (error != null) {
				error.printStackTrace();

				if (lobby == null) {
					Chat.builder().append("Unable to connect back! " + error.getMessage()).send(player);
				} else {
					Chat.builder().append("Unable to connect too lobby server! " + error.getMessage()).send(player);
				}
				return;
			}

			switch (result) {
			case SUCCESS -> {
				if (lobby == null) {
					Chat.builder().append("Successful reconnected.").send(player);
				} else {
					this.reconnectPlayer(player, null, current);
				}
			}
			case ALREADY_CONNECTED -> Chat.builder().append("Your already connected").send(player);
			case CONNECTION_CANCELLED -> Chat.builder().append("Connection was cancelled").send(player);
			case CONNECTION_IN_PROGRESS -> Chat.builder().append("Connection is in progress").send(player);
			case SERVER_DISCONNECTED -> Chat.builder().append("Server disconnected").send(player);
			}
		});
	}
}