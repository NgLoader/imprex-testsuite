package dev.imprex.testsuite.override;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mattmalec.pterodactyl4j.PteroAction;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.Directory;
import com.mattmalec.pterodactyl4j.client.entities.File;
import com.mattmalec.pterodactyl4j.client.managers.UploadFileAction;

import dev.imprex.testsuite.util.Pair;
import dev.imprex.testsuite.util.PteroUtil;
import kotlin.text.Charsets;

class OverrideActionFile {

	private final ClientServer clientServer;
	private final OverrideConfig config;

	private final String filename;
	private final String[] directoryPath;

	public OverrideActionFile(ClientServer clientServer, OverrideConfig config, String filePath) {
		this.clientServer = clientServer;
		this.config = config;

		String[] fileSplit = filePath.split("/");
		this.filename = fileSplit[fileSplit.length - 1];
		this.directoryPath = Arrays.copyOf(fileSplit, fileSplit.length - 1);
	}

	public CompletableFuture<Integer> overrideFile(Directory rootDirectory) {
		return this.retrieveDirectory(rootDirectory).thenCompose(this::overrideAndUploadFile);
	}

	private CompletableFuture<Integer> overrideAndUploadFile(Directory directory) {
		if  (directory == null) {
			return CompletableFuture.completedFuture(0);
		}

		return this.loadContent(directory)
				.thenApply(content -> this.overrideContent(directory, content))
				.thenCompose(writer -> this.uploadFile(directory, writer));
	}

	private CompletableFuture<Integer> uploadFile(Directory directory, Pair<StringWriter, Integer> result) {
		if (result == null) {
			return CompletableFuture.completedFuture(0);
		}
		StringWriter writer = result.getOne();

		UploadFileAction uploadFileAction = directory.upload();
		uploadFileAction.addFile(writer.getBuffer().toString().getBytes(Charsets.UTF_8), this.filename);
		return PteroUtil.execute(uploadFileAction)
				.thenAccept(__ -> uploadFileAction.clearFiles())
				.thenAccept(__ -> {
					try {
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				})
				.thenApply(__ -> result.getTwo());
	}

	private Pair<StringWriter, Integer> overrideContent(Directory directory, String content) {
		if (content == null) {
			if (!this.config.createFileWhenNotExist()) {
				return null;
			}

			// set content into a empty string so the reader is able to read
			content = "";
		}
		
		try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
			StringWriter stringWriter = new StringWriter(1024);

			try (BufferedWriter bufferedWriter = new BufferedWriter(stringWriter)) {
				int changes = OverrideHandler.overrideFile(reader, bufferedWriter, this.config);
				return changes != 0 ? new Pair<>(stringWriter, changes) : null;
			}
		} catch (IOException e) {
			throw new OverrideException("Unable to read content", e);
		}
	}

	private CompletableFuture<String> loadContent(Directory directory) {
		Optional<File> optionalFile = directory.getFileByName(this.filename);
		if (optionalFile.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		return PteroUtil.execute(optionalFile.get().retrieveContent());
	}

	private CompletableFuture<Directory> retrieveDirectory(Directory directory) {
		CompletableFuture<Directory> future = new CompletableFuture<>();
		this.retrieveDirectory(future, directory, 0);
		return future;
	}

	private void retrieveDirectory(CompletableFuture<Directory> future, Directory directory, int nextPath) {
		if (this.directoryPath.length == nextPath) {
			future.complete(directory);
			return;
		}

		String directoryName = this.directoryPath[nextPath];
		Optional<Directory> directoryChild = directory.getDirectoryByName(directoryName);
		if (directoryChild.isPresent()) {
			directory.into(directoryChild.get()).executeAsync(
					nextDirectory -> this.retrieveDirectory(future, nextDirectory, nextPath + 1),
					future::completeExceptionally);
			return;
		} else if (!this.config.createFileWhenNotExist()) {
			future.complete(null);
			return;
		}

		PteroAction<Void> reciveDirectory = directory.createFolder(this.directoryPath[nextPath]);
		reciveDirectory.executeAsync(__ -> {
			String path = String.join("/", Arrays.copyOf(this.directoryPath, nextPath));
			this.clientServer.retrieveDirectory(path).executeAsync(
					nextDirectory -> this.retrieveDirectory(future, nextDirectory, nextPath + 1),
					future::completeExceptionally);
		}, future::completeExceptionally);
	}
}
