package dev.imprex.testsuite.bungeecord;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import dev.imprex.testsuite.api.TestsuiteApi;
import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.api.TestsuiteServer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

public class BungeecordApi implements TestsuiteApi {

	private final BungeecordPlugin plugin;
	private final ProxyServer proxy = ProxyServer.getInstance();

	public BungeecordApi(BungeecordPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void scheduleTask(Runnable runnable, int delay, int repeat, TimeUnit unit) {
		this.proxy.getScheduler().schedule(this.plugin, runnable, delay, repeat, unit);
	}

	@Override
	public TestsuitePlayer getPlayer(String name) {
		return BungeecordPlayer.get(name);
	}

	@Override
	public List<TestsuitePlayer> getPlayers() {
		return this.proxy.getPlayers().stream()
				.map(BungeecordPlayer::get)
				.filter(Objects::nonNull)
				.toList();
	}

	@Override
	public TestsuiteServer getServer(String name) {
		return BungeecordServer.get(name);
	}

	@Override
	public TestsuiteServer createServer(String name, String ip, int port) {
		ServerInfo serverInfo = this.proxy.constructServerInfo(name, new InetSocketAddress(ip, port), "", false);
		this.proxy.getServers().put(name, serverInfo);
		BungeecordServer.add(serverInfo);
		return BungeecordServer.get(serverInfo);
	}

	@Override
	public boolean deleteServer(String name) {
		ServerInfo serverInfo = this.proxy.getServerInfo(name);
		if (serverInfo != null) {
			BungeecordServer.remove(serverInfo);
			this.proxy.getServers().remove(serverInfo.getName());
			return true;
		}
		return false;
	}

	@Override
	public List<TestsuiteServer> getServers() {
		return this.proxy.getServers().values().stream()
				.map(BungeecordServer::get)
				.filter(Objects::nonNull)
				.toList();
	}
}