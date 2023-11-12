package dev.imprex.testsuite.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.kyori.adventure.text.Component;

public interface TestsuitePlayer extends TestsuiteSender {

	CompletableFuture<ConnectionResult> connect(TestsuiteServer server);

	void sendPlayerList(Component header, Component footer);
	void sendPlayerListHeader(Component header);
	void sendPlayerListFooter(Component footer);

	String getName();

	UUID getUUID();

	TestsuiteServer getServer();
}