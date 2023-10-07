package dev.imprex.testsuite.command.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.util.Chat;
import dev.imprex.testsuite.util.TestsuitePlayer;
import dev.imprex.testsuite.util.TestsuiteSender;
import dev.imprex.testsuite.util.TestsuiteServer;

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
			Chat.send(player, "Unable to find lobby server!");
			return Command.SINGLE_SUCCESS;
		}

		TestsuiteServer serverConnection = player.getServer();
		if (serverConnection == null) {
			Chat.send(player, "Unable to find current server!");
			return Command.SINGLE_SUCCESS;
		}

		if (lobby.equals(serverConnection)) {
			Chat.send(player, "You can only reconnect on non lobby servers!");
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
					Chat.send(player, "Unable to connect back! " + error.getMessage());
				} else {
					Chat.send(player, "Unable to connect too lobby server! " + error.getMessage());
				}
				return;
			}

			switch (result) {
			case SUCCESS -> {
				if (lobby == null) {
					Chat.send(player, "Successful reconnected.");
				} else {
					this.reconnectPlayer(player, null, current);
				}
			}
			case ALREADY_CONNECTED -> Chat.send(player, "Your already connected");
			case CONNECTION_CANCELLED -> Chat.send(player, "Connection was cancelled");
			case CONNECTION_IN_PROGRESS -> Chat.send(player, "Connection is in progress");
			case SERVER_DISCONNECTED -> Chat.send(player, "Server disconnected");
			}
		});
	}
}