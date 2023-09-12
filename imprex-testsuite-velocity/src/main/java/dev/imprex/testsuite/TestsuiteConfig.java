package dev.imprex.testsuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

public class TestsuiteConfig {

	private final Path file;
	private final YAMLConfigurationLoader loader;

	private String url;
	private String applicationToken;
	private String clientToken;

	public TestsuiteConfig(Path dataFolder) {
		this.file = dataFolder.resolve("config.yml");
		this.loader = YAMLConfigurationLoader.builder()
				.setPath(this.file)
				.build();
	}

	public void load() throws IOException {
		Files.createDirectories(this.file.getParent());

		if (Files.notExists(this.file)) {
			ConfigurationNode defaultNode = this.loader.createEmptyNode();
			defaultNode.getNode("url").setValue("pterodactyl-url");
			defaultNode.getNode("applicationToken").setValue("pterodactyl-application-token");
			defaultNode.getNode("clientToken").setValue("pterodactyl-client-token");
			this.loader.save(defaultNode);
		}

		ConfigurationNode node = this.loader.load();
		this.url = node.getNode("url").getString();
		this.applicationToken = node.getNode("applicationToken").getString();
		this.clientToken = node.getNode("clientToken").getString();
	}

	public boolean validate() {
		if (this.url.equals("pterodactyl-url") ||
				this.applicationToken.equals("pterodactyl-application-token") ||
				this.clientToken.equals("pterodactyl-client-token")) {
			return false;
		}
		return true;
	}

	public String getUrl() {
		return this.url;
	}

	public String getApplicationToken() {
		return this.applicationToken;
	}

	public String getClientToken() {
		return this.clientToken;
	}
}