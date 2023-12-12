package dev.imprex.testsuite.override;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.Directory;
import com.mattmalec.pterodactyl4j.client.entities.File;

import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.template.ServerTemplate;
import dev.imprex.testsuite.util.PteroUtil;

public class OverrideAction {

	public static final String OVERRIDE_FILE_NAME = "override.yml";
	public static final String OVERRIDE_LOCAL_FILE_NAME = "override.local.yml";

	public static CompletableFuture<Integer> override(ServerInstance server, boolean overrideAfterStart) {
		return new OverrideAction(server, overrideAfterStart).override();
	}

	private final AtomicInteger overrideCount = new AtomicInteger();

	private final ClientServer server;
	private final ServerTemplate template;

	private final boolean serverRunning;

	private OverrideAction(ServerInstance instance, boolean serverRunning) {
		this.server = instance.getClientServer();
		this.template = instance.getTemplate();
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
				.thenAccept(__ -> System.gc()) // TODO test phase (memory issue with pterodactyl4j api)
				.thenApply(__ -> this.overrideCount.getAcquire());
	}

	private Stream<OverrideActionFile> createOverrideRequests(Map<String, OverrideConfig> configFiles) {
		return configFiles.entrySet().stream()
				.filter(entry -> entry.getValue() != null && entry.getValue().isValid())
				.filter(entry -> entry.getValue().overrideAfterStart() ? this.serverRunning : true)
				.map(entry -> new OverrideActionFile(this.server, entry.getValue(), entry.getKey()));
	}

	private CompletableFuture<String> retrieveOverrideFile(Directory directory) {
		Optional<File> optionalOverrideLocalFile = directory.getFileByName(OVERRIDE_LOCAL_FILE_NAME);
		if (optionalOverrideLocalFile.isPresent()) {
			return PteroUtil.execute(optionalOverrideLocalFile.get().retrieveContent());
		}

		CompletableFuture<String> future = new CompletableFuture<>();
		if (this.template != null) {
			try {
				Optional<Path> searchResult = Files.walk(this.template.getPath(), 1)
					.filter(file -> file.getFileName().toString().equals(OVERRIDE_FILE_NAME))
					.findFirst();
				
				if (searchResult.isPresent()) {
					try {
						future.complete(new String(Files.readAllBytes(searchResult.get())));
					} catch (IOException e) {
						future.completeExceptionally(e);
					}
				} else {
					future.completeExceptionally(new OverrideException("Unable to find any override file!"));
				}
			} catch (IOException e) {
				future.completeExceptionally(e);
			}
		} else {
			Optional<File> optionalOverrideFile = directory.getFileByName(OVERRIDE_FILE_NAME);
			if (optionalOverrideFile.isPresent()) {
				return PteroUtil.execute(optionalOverrideFile.get().retrieveContent());
			} else {
				future.completeExceptionally(new OverrideException("Unable to find any override file!"));
			}
		}
		
		return future;
	}
}