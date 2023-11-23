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
	private final boolean overrideAfterStart;

	private CompletableFuture<Integer> finalFuture;

	private OverrideAction(ClientServer server, boolean overrideAfterStart) {
		this.server = server;
		this.overrideAfterStart = overrideAfterStart;
	}

	private CompletableFuture<Integer> override() {
		if (this.finalFuture != null) {
			throw new IllegalStateException("Override action was already called!");
		}

		PteroUtil.execute(this.server.retrieveDirectory())
				.thenApply(this::selectOverrideFile)
				.thenApply(file -> PteroUtil.execute(file.retrieveContent()))
				.whenComplete(this::handleOverrideConfig);

		return this.finalFuture = new CompletableFuture<>();
	}

	private void handleOverrideConfig(CompletableFuture<String> config, Throwable error) {
		this.handleException(error);

		config.thenApply(OverrideHandler::loadOverride)
				.thenApply(this::createOverrideRequests)
				.whenComplete(this::handleOverrideRequests);
	}

	private void handleOverrideRequests(Stream<OverrideActionRequest> requests, Throwable error) {
		this.handleException(error);

		// TODO fetch directory requests and merge together
		CompletableFuture.allOf(requests
						.map(action -> action.override(this))
						.map(action -> action.thenAccept(this::handleOverrideEntryResult))
						.toArray(CompletableFuture[]::new))
				.whenComplete(this::handleOverrideResult);
	}

	private void handleOverrideResult(Void __, Throwable error) {
		this.handleException(error);

		this.finalFuture.complete(this.overrideCount.getAcquire());
	}

	private void handleOverrideEntryResult(Boolean result) {
		if (result) {
			this.overrideCount.incrementAndGet();
		}
	}

	private Stream<OverrideActionRequest> createOverrideRequests(Map<String, OverrideConfig> configFiles) {
		return configFiles.entrySet().stream()
				.filter(entry -> entry.getValue().overrideAfterFirstStart() ? this.overrideAfterStart : true)
				.map(OverrideActionRequest::new);
	}

	private File selectOverrideFile(Directory directory) {
		Optional<File> optionalOverrideLocalFile = directory.getFileByName("override.local.yml");
		if (optionalOverrideLocalFile.isPresent()) {
			return optionalOverrideLocalFile.get();
		}

		Optional<File> optionalOverrideFile = directory.getFileByName("override.yml");
		if (optionalOverrideFile.isPresent()) {
			return optionalOverrideFile.get();
		}

		throw new NullPointerException("Unable to find any override file!");
	}

	private void handleException(Throwable error) {
		if (error != null) {
			this.finalFuture.completeExceptionally(error);
			throw new OverrideException(error); // maybe find a better way to cancel the current workflow
		}
	}
}