package de.imprex.testsuite.local;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import dev.imprex.testsuite.api.TestsuiteApi;
import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.api.TestsuiteServer;

public class LocalApi implements TestsuiteApi {

	private final VelocityPlugin plugin;
	private final ProxyServer proxy;

	public LocalApi(VelocityPlugin plugin) {
		this.plugin = plugin;
		this.proxy = plugin.getProxy();
	}

	@Override
	public void scheduleTask(Runnable runnable, int delay, int repeat, TimeUnit unit) {
		this.proxy.getScheduler().buildTask(this.plugin, runnable)
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