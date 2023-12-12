package dev.imprex.testsuite.api;

import java.util.List;

import net.kyori.adventure.text.Component;

public interface TestsuiteServer {

	String getIdentifier();

	String getName();

	String getAddress();

	int getPort();

	void broadcast(Component component);

	List<TestsuitePlayer> getPlayers();
}