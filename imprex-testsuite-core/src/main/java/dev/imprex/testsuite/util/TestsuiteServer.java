package dev.imprex.testsuite.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.kyori.adventure.text.Component;

public interface TestsuiteServer {

	CompletableFuture<ConnectionResult> connect(TestsuitePlayer player);

	void broadcast(Component component);

	String getName();

	String getAddress();
	int getPort();

	List<TestsuitePlayer> getPlayers();
}