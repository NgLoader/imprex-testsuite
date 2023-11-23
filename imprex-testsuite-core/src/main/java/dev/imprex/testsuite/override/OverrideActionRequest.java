package dev.imprex.testsuite.override;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

class OverrideActionRequest {

	private final CompletableFuture<Boolean> finalFuture = new CompletableFuture<>();

	private final String filename;
	private final String[] directory;

	public OverrideActionRequest(Entry<String, OverrideConfig> entry) {
		String file = entry.getKey();
		String[] fileSplit = file.split("/");
		this.filename = fileSplit[fileSplit.length - 1];
		this.directory = Arrays.copyOf(fileSplit, fileSplit.length - 2);
	}

	public CompletableFuture<Boolean> override(OverrideAction action) {
		// TODO do some magic here
		return this.finalFuture;
		}

	public String getFilename() {
		return this.filename;
	}

	public String[] getDirectory() {
		return this.directory;
	}

	public String getDirectoryString() {
		return String.join("/", this.directory);
	}
}
