package dev.imprex.testsuite.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestsuiteConfig {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private final Path path;

	private PterodactylConfig pterodactylConfig;
	private AllocationConfig allocationConfig;
	private ServerConfig serverConfig;

	public TestsuiteConfig(Path path) {
		this.path = path;

		this.load();
	}

	private void load() {
		if (Files.notExists(this.path)) {
			System.out.println("Generating default config");
			this.save();
			return;
		}

		try (BufferedReader bufferedReader = Files.newBufferedReader(this.path)) {
			JsonObject root = JsonParser.parseReader(bufferedReader).getAsJsonObject();

			JsonObject pterodactylObject = root.getAsJsonObject("pterodactyl");
			this.pterodactylConfig = GSON.fromJson(pterodactylObject, PterodactylConfig.class);

			JsonObject allocationObject = root.getAsJsonObject("allocation");
			this.allocationConfig = GSON.fromJson(allocationObject, AllocationConfig.class);

			JsonObject serverObject = root.getAsJsonObject("server");
			this.serverConfig = GSON.fromJson(serverObject, ServerConfig.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void save() {
		JsonObject root = new JsonObject();

		this.pterodactylConfig = new PterodactylConfig("url", "application-token", "client-token");
		root.add("pterodactyl", GSON.toJsonTree(this.pterodactylConfig));

		this.allocationConfig = new AllocationConfig("node", "testsuite", "127.0.0.1", 25400, 25499);
		root.add("allocation", GSON.toJsonTree(this.allocationConfig));

		this.serverConfig = new ServerConfig(6, 6);
		root.add("server", GSON.toJsonTree(this.serverConfig));

		try {
			Files.createDirectories(this.path.getParent());

			try (BufferedWriter bufferedWriter = Files.newBufferedWriter(this.path)) {
				GSON.toJson(root, bufferedWriter);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public PterodactylConfig getPterodactylConfig() {
		return this.pterodactylConfig;
	}

	public AllocationConfig getAllocationConfig() {
		return this.allocationConfig;
	}

	public ServerConfig getServerConfig() {
		return this.serverConfig;
	}
}