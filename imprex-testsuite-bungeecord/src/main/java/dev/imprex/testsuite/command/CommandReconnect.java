package dev.imprex.testsuite.command;

import static dev.imprex.testsuite.util.ArgumentBuilder.literal;

import java.util.concurrent.TimeUnit;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.command.brigadier.BrigadierCommand;
import dev.imprex.testsuite.util.Chat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;

public class CommandReconnect {

	public static LiteralArgumentBuilder<CommandSender> COMMAND;

	private final TestsuitePlugin plugin;
	private final ProxyServer proxy;

	public CommandReconnect(TestsuitePlugin plugin) {
		this.plugin = plugin;
		this.proxy = plugin.getProxy();

		COMMAND = this.create();
	}

	public BrigadierCommand brigadierCommand() {
		return new BrigadierCommand(COMMAND, "rc");
	}

	public LiteralArgumentBuilder<CommandSender> create() {
		return literal("reconnect")
				.requires(source -> source instanceof ProxiedPlayer)
				.executes(this::reconnect);
	}

	public int reconnect(CommandContext<CommandSender> context) {
		ProxiedPlayer player = (ProxiedPlayer) context.getSource();

		ServerInfo lobby = this.proxy.getServerInfo("lobby");
		if (lobby == null) {
			Chat.send(player, "Unable to find lobby server!");
			return Command.SINGLE_SUCCESS;
		}

		Server serverConnection = player.getServer();
		if (serverConnection == null) {
			Chat.send(player, "Unable to find current server!");
			return Command.SINGLE_SUCCESS;
		}
		ServerInfo current = serverConnection.getInfo();

		if (lobby.equals(current)) {
			Chat.send(player, "You can only reconnect on non lobby servers!");
			return Command.SINGLE_SUCCESS;
		}

		this.reconnectPlayer(player, lobby, current);
		return Command.SINGLE_SUCCESS;
	}

	public void reconnectPlayer(ProxiedPlayer player, ServerInfo lobby, ServerInfo current) {
		player.connect(ServerConnectRequest.builder()
				.target(lobby != null ? lobby : current)
				.reason(Reason.COMMAND)
				.callback((result, error) -> {
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
							ProxyServer.getInstance().getScheduler().schedule(this.plugin, () -> this.reconnectPlayer(player, null, current), 1, TimeUnit.SECONDS);
						}
					}
					case ALREADY_CONNECTED -> Chat.send(player, "Your already connected");
					case ALREADY_CONNECTING -> Chat.send(player, "Connection is in progress");
					case EVENT_CANCEL -> Chat.send(player, "Connection was cancelled");
					case FAIL -> Chat.send(player, "Server disconnected");
					}
				})
				.build());
	}
}