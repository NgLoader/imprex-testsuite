package de.imprex.testsuite.local;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.api.ConnectionResult;
import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.api.TestsuiteServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public record LocalPlayer(UUID uuid, String name) implements TestsuitePlayer {

	@Override
	public void sendMessage(Component component) {
		TestsuiteLogger.info(this.getName() + ": " + PlainTextComponentSerializer.plainText().serialize(component));
	}

	@Override
	public CompletableFuture<ConnectionResult> connect(TestsuiteServer server) {
		return CompletableFuture.completedFuture(ConnectionResult.SUCCESS);
	}

	@Override
	public void sendPlayerList(Component header, Component footer) {
	}

	@Override
	public void sendPlayerListHeader(Component header) {
	}

	@Override
	public void sendPlayerListFooter(Component footer) {
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public UUID getUUID() {
		return this.uuid;
	}

	@Override
	public TestsuiteServer getServer() {
		return null;
	}
}
