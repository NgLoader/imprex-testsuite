package dev.imprex.testsuite.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import dev.imprex.testsuite.TestsuitePlugin;
import net.kyori.adventure.text.Component;

public class CommandReconnect {

	private final ProxyServer proxy;

	public CommandReconnect(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
	}

	public LiteralArgumentBuilder<CommandSource> create() {
		return literal("reconnect")
				.requires(source -> source instanceof Player)
				.executes(this::reconnect);
	}

	public int reconnect(CommandContext<CommandSource> context) {
		Player player = (Player) context.getSource();

		RegisteredServer lobby = this.proxy.getServer("lobby").orElseGet(() -> null);
		if (lobby == null) {
			player.sendMessage(Component.text("Unable to find lobby server!"));
			return Command.SINGLE_SUCCESS;
		}

		ServerConnection serverConnection = player.getCurrentServer().orElseGet(() -> null);
		if (serverConnection == null) {
			player.sendMessage(Component.text("Unable to find current server!"));
			return Command.SINGLE_SUCCESS;
		}
		RegisteredServer current = serverConnection.getServer();

		if (lobby.equals(current)) {
			player.sendMessage(Component.text("You can only reconnect on non lobby servers!"));
			return Command.SINGLE_SUCCESS;
		}

		player.createConnectionRequest(lobby).connect().whenComplete((result, error) -> {
			if (error != null) {
				error.printStackTrace();
				player.sendMessage(Component.text("Unable to connect too lobby server! " + error.getMessage()));
				return;
			}

			switch (result.getStatus()) {
			case SUCCESS:
				player.createConnectionRequest(current).connect().whenComplete((result2, error2) -> {
					if (error2 != null) {
						error2.printStackTrace();
						player.sendMessage(Component.text("Unable to connect back! " + error2.getMessage()));
						return;
					}

					switch (result2.getStatus()) {
					case SUCCESS:
						player.sendMessage(Component.text("§aSuccessful reconnected."));
						break;
					case ALREADY_CONNECTED:
						player.sendMessage(Component.text("Your already connected"));
						break;
					case CONNECTION_CANCELLED:
						player.sendMessage(Component.text("Connection was cancelled"));
						break;
					case CONNECTION_IN_PROGRESS:
						player.sendMessage(Component.text("Connection is in progress"));
						break;
					case SERVER_DISCONNECTED:
						player.sendMessage(Component.text("Server disconnected"));
						break;
					}
				});
				break;
			case ALREADY_CONNECTED:
				player.sendMessage(Component.text("Your already connected"));
				break;
			case CONNECTION_CANCELLED:
				player.sendMessage(Component.text("Connection was cancelled"));
				break;
			case CONNECTION_IN_PROGRESS:
				player.sendMessage(Component.text("Connection is in progress"));
				break;
			case SERVER_DISCONNECTED:
				player.sendMessage(Component.text("Server disconnected"));
				break;
			}
		});
		return Command.SINGLE_SUCCESS;
	}
}