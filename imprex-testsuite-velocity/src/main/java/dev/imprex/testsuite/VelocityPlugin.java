package dev.imprex.testsuite;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
import com.velocitypowered.api.proxy.server.ServerInfo;

import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.util.TestsuitePlayer;
import dev.imprex.testsuite.util.TestsuiteServer;

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

		VelocityPlayer.proxy = proxy;
		VelocityServer.proxy = proxy;
	}

	@Subscribe
	public void onProxyInitialize(ProxyInitializeEvent event) {
		super.enable();

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

	@Override
	public void scheduleTask(Runnable runnable, int delay, int repeat, TimeUnit unit) {
		this.proxy.getScheduler().buildTask(this, runnable)
				.delay(delay, unit)
				.repeat(repeat, unit)
				.schedule();
	}

	@Override
	public TestsuitePlayer getPlayer(String name) {
		return VelocityPlayer.get(name);
	}

	@Override
	public List<TestsuitePlayer> getPlayers() {
		return this.proxy.getAllPlayers().stream()
				.map(VelocityPlayer::get)
				.filter(Objects::nonNull)
				.toList();
	}

	@Override
	public TestsuiteServer getServer(String name) {
		return VelocityServer.get(name);
	}

	@Override
	public TestsuiteServer createServer(String name, String ip, int port) {
		ServerInfo serverInfo = new ServerInfo(name, new InetSocketAddress(ip, port));
		RegisteredServer registeredServer = this.proxy.registerServer(serverInfo);
		VelocityServer.add(registeredServer);
		return VelocityServer.get(serverInfo);
	}

	@Override
	public boolean deleteServer(String name) {
		Optional<RegisteredServer> server = this.proxy.getServer(name);
		if (server.isPresent()) {
			RegisteredServer registeredServer = server.get();
			VelocityServer.remove(registeredServer);
			this.proxy.unregisterServer(registeredServer.getServerInfo());
			return true;
		}
		return false;
	}

	@Override
	public List<TestsuiteServer> getServers() {
		return this.proxy.getAllServers().stream()
				.map(RegisteredServer::getServerInfo)
				.map(VelocityServer::get)
				.filter(Objects::nonNull)
				.toList();
	}
}
