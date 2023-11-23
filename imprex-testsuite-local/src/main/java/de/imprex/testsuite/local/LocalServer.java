package de.imprex.testsuite.local;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dev.imprex.testsuite.api.ConnectionResult;
import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.api.TestsuiteServer;
import net.kyori.adventure.text.Component;

public record LocalServer(String name, String address, int port) implements TestsuiteServer {

	@Override
	public CompletableFuture<ConnectionResult> connect(TestsuitePlayer player) {
		return player.connect(this);
	}

	@Override
	public void broadcast(Component component) {
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getAddress() {
		return this.address;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public List<TestsuitePlayer> getPlayers() {
		return Collections.emptyList();
	}
}
