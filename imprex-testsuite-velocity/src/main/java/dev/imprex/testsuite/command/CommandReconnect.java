package dev.imprex.testsuite.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.util.Chat;

public class CommandReconnect {

	private final ProxyServer proxy;

	public CommandReconnect(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();

		this.register();
	}

	public void register() {
		CommandManager commandManager = this.proxy.getCommandManager();
		CommandMeta commandMeta = commandManager.metaBuilder("reconnect")
				.aliases("rc")
				.plugin(this)
				.build();

		BrigadierCommand command = new BrigadierCommand(this.create());
		commandManager.register(commandMeta, command);
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
			Chat.send(player, "Unable to find lobby server!");
			return Command.SINGLE_SUCCESS;
		}

		ServerConnection serverConnection = player.getCurrentServer().orElseGet(() -> null);
		if (serverConnection == null) {
			Chat.send(player, "Unable to find current server!");
			return Command.SINGLE_SUCCESS;
		}
		RegisteredServer current = serverConnection.getServer();

		if (lobby.equals(current)) {
			Chat.send(player, "You can only reconnect on non lobby servers!");
			return Command.SINGLE_SUCCESS;
		}

		this.reconnectPlayer(player, lobby, current);
		return Command.SINGLE_SUCCESS;
	}

	public void reconnectPlayer(Player player, RegisteredServer lobby, RegisteredServer current) {
		player.createConnectionRequest(lobby != null ? lobby : current).connect().whenComplete((result, error) -> {
			if (error != null) {
				error.printStackTrace();

				if (lobby == null) {
					Chat.send(player, "Unable to connect back! " + error.getMessage());
				} else {
					Chat.send(player, "Unable to connect too lobby server! " + error.getMessage());
				}
				return;
			}

			switch (result.getStatus()) {
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