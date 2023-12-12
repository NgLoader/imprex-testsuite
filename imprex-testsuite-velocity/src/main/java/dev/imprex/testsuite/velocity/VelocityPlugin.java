package dev.imprex.testsuite.velocity;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.server.ServerInstance;

@Plugin(
		id = "imprex-testsuite",
		name = "Imprex Test suite",
		version = "1.0.0",
		authors = { "NgLoader" })
public class VelocityPlugin extends TestsuitePlugin {

	@Inject
	private ProxyServer proxy;

	@Inject
	public VelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
		super.load(new VelocityLogger(logger), dataFolder);

		VelocityPlayer.plugin = this;
	}

	@Subscribe
	public void onProxyInitialize(ProxyInitializeEvent event) {
		super.enable(new VelocityApi(this));

		// Register commands
		CommandManager commandManager = this.proxy.getCommandManager();
		CommandMeta commandMeta = commandManager.metaBuilder("testsuite")
				.aliases("ts", "tests", "tsuite")
				.plugin(this)
				.build();

		commandManager.register(commandMeta, new VelocityCommand(this, (args) -> args));

		this.getCommandRegistry().getCommands().values().stream()
				.filter(command -> command.isRoot())
				.forEach(command -> {
					String literal = command.literal().getLiteral();
					CommandMeta subCommandMeta = commandManager.metaBuilder(literal)
							.aliases(command.aliases().toArray(String[]::new))
							.plugin(this)
							.build();

					commandManager.register(subCommandMeta, new VelocityCommand(this, (args) -> literal + " " + args));
				});
	}

	@Subscribe
	public void onPlayerServerChange(ServerConnectedEvent event) {
		RegisteredServer previousServer = event.getPreviousServer().orElseGet(() -> null);
		if (previousServer != null) {
			ServerInstance pteroServer = this.getServerManager().getServer(previousServer.getServerInfo().getName());
			if (pteroServer == null) {
				return;
			}

			TestsuiteLogger.debug("Reset inactive time on server {1} because {0} disconnected.", event.getPlayer().getUsername(), pteroServer.getName());
			pteroServer.resetInactiveTime();
		}
	}

	@Subscribe
	public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
		VelocityPlayer.add(event.getPlayer());
	}

	@Subscribe
	public void onDisconnect(DisconnectEvent event) {
		VelocityPlayer.remove(event.getPlayer());
	}

	public ProxyServer getProxy() {
		return this.proxy;
	}
}
