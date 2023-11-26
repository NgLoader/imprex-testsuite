package dev.imprex.testsuite.override;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.Directory;
import com.mattmalec.pterodactyl4j.client.entities.File;

import dev.imprex.testsuite.util.PteroUtil;

public class OverrideAction {

	public static CompletableFuture<Integer> override(ClientServer server, boolean overrideAfterStart) {
		return new OverrideAction(server, overrideAfterStart).override();
	}

	private final AtomicInteger overrideCount = new AtomicInteger();

	private final ClientServer server;
	private final boolean serverRunning;

	private OverrideAction(ClientServer server, boolean serverRunning) {
		this.server = server;
		this.serverRunning = serverRunning;
	}

	private CompletableFuture<Integer> override() {
		return PteroUtil.execute(this.server.retrieveDirectory())
				.thenCompose(this::retrieveOverrideFile) // load override file
				.thenApply(OverrideHandler::loadOverride) // read override settings
				.thenApply(this::createOverrideRequests) // create override requests
				.thenCompose(this::overrideFiles); // override all files
	}

	private CompletableFuture<Integer> overrideFiles(Stream<OverrideActionFile> requests) {
		// TODO fetch directory requests and merge together
		return PteroUtil.execute(this.server.retrieveDirectory())
				.thenCompose(directory -> {
					return CompletableFuture.allOf(requests
							.map(action -> action.overrideFile(directory)
									.thenAccept(changes -> this.overrideCount.getAndAdd(changes)))
							.toArray(CompletableFuture[]::new));
				})
				.thenApply(__ -> this.overrideCount.getAcquire());
	}

	private Stream<OverrideActionFile> createOverrideRequests(Map<String, OverrideConfig> configFiles) {
		return configFiles.entrySet().stream()
				.filter(entry -> entry.getValue().overrideAfterStart() ? this.serverRunning : true)
				.map(entry -> new OverrideActionFile(this.server, entry.getValue(), entry.getKey()));
	}

	private CompletableFuture<String> retrieveOverrideFile(Directory directory) {
		Optional<File> optionalOverrideLocalFile = directory.getFileByName("override.local.yml");
		if (optionalOverrideLocalFile.isPresent()) {
			return PteroUtil.execute(optionalOverrideLocalFile.get().retrieveContent());
		}

		Optional<File> optionalOverrideFile = directory.getFileByName("override.yml");
		if (optionalOverrideFile.isPresent()) {
			return PteroUtil.execute(optionalOverrideFile.get().retrieveContent());
		}

		throw new OverrideException("Unable to find any override file!");
	}
}