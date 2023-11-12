package dev.imprex.testsuite.velocity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import dev.imprex.testsuite.api.ConnectionResult;
import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.api.TestsuiteServer;
import net.kyori.adventure.text.Component;

public class VelocityServer implements TestsuiteServer {

	static ProxyServer proxy;

	private static final Map<String, TestsuiteServer> CACHE = new ConcurrentHashMap<>();

	static void add(RegisteredServer server) {
		VelocityServer.CACHE.put(server.getServerInfo().getName().toLowerCase(), new VelocityServer(server));
	}

	static void remove(RegisteredServer server) {
		VelocityServer.CACHE.remove(server.getServerInfo().getName().toLowerCase());
	}

	public static TestsuiteServer get(String name) {
		return VelocityServer.CACHE.get(name.toLowerCase());
	}

	public static TestsuiteServer get(ServerConnection serverConnection) {
		return VelocityServer.get(serverConnection.getServerInfo());
	}

	public static TestsuiteServer get(ServerInfo serverInfo) {
		return VelocityServer.get(serverInfo.getName());
	}

	private final RegisteredServer server;
	private final ServerInfo serverInfo;

	private VelocityServer(RegisteredServer server) {
		this.server = server;
		this.serverInfo = server.getServerInfo();
	}

	@Override
	public CompletableFuture<ConnectionResult> connect(TestsuitePlayer player) {
		return player.connect(this);
	}

	@Override
	public void broadcast(Component component) {
		this.server.getPlayersConnected().forEach(player -> player.sendMessage(component));
	}

	@Override
	public String getName() {
		return this.serverInfo.getName();
	}

	@Override
	public String getAddress() {
		return this.serverInfo.getAddress().getHostName();
	}

	@Override
	public int getPort() {
		return this.serverInfo.getAddress().getPort();
	}

	@Override
	public List<TestsuitePlayer> getPlayers() {
		return this.server.getPlayersConnected().stream()
				.map(Player::getUsername)
				.map(VelocityPlayer::get)
				.filter(Objects::nonNull)
				.toList();
	}
}
