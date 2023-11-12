package dev.imprex.testsuite.bungeecord;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import dev.imprex.testsuite.api.ConnectionResult;
import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.api.TestsuiteServer;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;

public class BungeecordServer implements TestsuiteServer {

	private static final Map<String, TestsuiteServer> CACHE = new ConcurrentHashMap<>();

	static void add(ServerInfo server) {
		BungeecordServer.CACHE.put(server.getName().toLowerCase(), new BungeecordServer(server));
	}

	static void remove(ServerInfo server) {
		BungeecordServer.CACHE.remove(server.getName().toLowerCase());
	}

	public static TestsuiteServer get(String name) {
		return BungeecordServer.CACHE.get(name.toLowerCase());
	}

	public static TestsuiteServer get(Server serverConnection) {
		return BungeecordServer.get(serverConnection.getInfo());
	}

	public static TestsuiteServer get(ServerInfo serverInfo) {
		return BungeecordServer.get(serverInfo.getName());
	}

	private final ServerInfo serverInfo;

	private BungeecordServer(ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}

	@Override
	public CompletableFuture<ConnectionResult> connect(TestsuitePlayer player) {
		return player.connect(this);
	}

	@Override
	public void broadcast(Component component) {
		this.serverInfo.getPlayers().stream()
				.map(BungeecordPlayer::get)
				.filter(Objects::nonNull)
				.forEach(player -> player.sendMessage(component));
	}

	@Override
	public String getName() {
		return this.serverInfo.getName();
	}

	@Override
	public String getAddress() {
		return ((InetSocketAddress) this.serverInfo.getSocketAddress()).getHostName();
	}

	@Override
	public int getPort() {
		return ((InetSocketAddress) this.serverInfo.getSocketAddress()).getPort();
	}

	@Override
	public List<TestsuitePlayer> getPlayers() {
		return this.serverInfo.getPlayers().stream()
				.map(BungeecordPlayer::get)
				.filter(Objects::nonNull)
				.toList();
	}
}
