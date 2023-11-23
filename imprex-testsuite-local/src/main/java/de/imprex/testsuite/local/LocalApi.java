package de.imprex.testsuite.local;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import dev.imprex.testsuite.api.TestsuiteApi;
import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.api.TestsuiteServer;

public class LocalApi implements TestsuiteApi {

	private final Map<String, TestsuitePlayer> playerCache = new HashMap<>();
	private final Map<String, TestsuiteServer> serverCache = new HashMap<>();

	@Override
	public void scheduleTask(Runnable runnable, int delay, int repeat, TimeUnit unit) {
		// TODO start threads
	}

	@Override
	public TestsuitePlayer getPlayer(String name) {
		return this.playerCache.computeIfAbsent(name, value -> new LocalPlayer(UUID.randomUUID(), name));
	}

	@Override
	public List<TestsuitePlayer> getPlayers() {
		return this.playerCache.values().stream()
				.filter(Objects::nonNull)
				.toList();
	}

	@Override
	public TestsuiteServer getServer(String name) {
		return this.serverCache.get(name);
	}

	@Override
	public TestsuiteServer createServer(String name, String ip, int port) {
		return this.serverCache.computeIfAbsent(name, value -> new LocalServer(name, ip, port));
	}

	@Override
	public boolean deleteServer(String name) {
		return this.serverCache.remove(name) != null;
	}

	@Override
	public List<TestsuiteServer> getServers() {
		return this.serverCache.values().stream()
				.filter(Objects::nonNull)
				.toList();
	}
}