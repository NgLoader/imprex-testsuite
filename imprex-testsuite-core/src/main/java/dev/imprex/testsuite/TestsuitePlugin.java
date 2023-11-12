package dev.imprex.testsuite;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.mattmalec.pterodactyl4j.PteroBuilder;
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;

import dev.imprex.testsuite.command.CommandRegistry;
import dev.imprex.testsuite.command.suggestion.CommandSuggestion;
import dev.imprex.testsuite.common.ServerVersionCache;
import dev.imprex.testsuite.common.override.OverrideHandler;
import dev.imprex.testsuite.config.PterodactylConfig;
import dev.imprex.testsuite.config.TestsuiteConfig;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.template.ServerTemplateList;
import dev.imprex.testsuite.util.TestsuitePlayer;
import dev.imprex.testsuite.util.TestsuiteServer;
import okhttp3.OkHttpClient;

public abstract class TestsuitePlugin {

	private Path dataFolder;

	private TestsuiteConfig config;

	private PteroApplication pteroApplication;
	private PteroClient pteroClient;

	private OverrideHandler overrideHandler;

	private ServerVersionCache versionCache;
	private ServerTemplateList templateList;
	private ServerManager serverManager;

	private CommandSuggestion commandSuggestion;
	private CommandRegistry commandRegistry;

	private TestsuiteVisual testsuiteVisual;

	public void load(Logger logger, Path dataFolder) {
		TestsuiteLogger.initialize(this, logger);
		this.dataFolder = dataFolder;
	}

	public void enable() {
		// Initialize configuration
		this.config = new TestsuiteConfig(this.getPluginFolder().resolve("config.json"));
		PterodactylConfig tylConfig = this.config.getPterodactylConfig();

		if (!tylConfig.valid()) {
			TestsuiteLogger.info("!!! Invalid testsuite configuration !!!");
			return;
		}

		// Login into pterodactyl
		this.pteroApplication = PteroBuilder.createApplication(tylConfig.url(), tylConfig.applicationToken());
		this.pteroClient = PteroBuilder.create(tylConfig.url(), tylConfig.clientToken())
				.setWebSocketClient(new OkHttpClient.Builder()
						.pingInterval(30, TimeUnit.SECONDS)
						.build())
				.buildClient();

		// Register systems
		this.overrideHandler = new OverrideHandler();

		this.versionCache = new ServerVersionCache(this.getPluginFolder().resolve("version_cache.json"));
		this.templateList = new ServerTemplateList(this, this.getPluginFolder().resolve("template"));
		this.serverManager = new ServerManager(this);

		this.testsuiteVisual = new TestsuiteVisual(this);

		// Start scheduler
		this.scheduleTask(this.versionCache, 1, 1, TimeUnit.MINUTES);
		this.scheduleTask(this.serverManager, 1, 1, TimeUnit.SECONDS);
		this.scheduleTask(this.testsuiteVisual, 4, 4, TimeUnit.SECONDS);

		// Register commands
		this.commandSuggestion = new CommandSuggestion(this);
		this.commandRegistry = new CommandRegistry(this);
	}

	public void disable() {
	}

	public abstract void scheduleTask(Runnable runnable, int delay, int repeat, TimeUnit unit);

	public abstract TestsuitePlayer getPlayer(String name);

	public abstract List<TestsuitePlayer> getPlayers();

	public abstract TestsuiteServer getServer(String name);

	public abstract TestsuiteServer createServer(String name, String ip, int port);

	public abstract boolean deleteServer(String name);

	public abstract List<TestsuiteServer> getServers();

	public PteroApplication getPteroApplication() {
		return this.pteroApplication;
	}

	public PteroClient getPteroClient() {
		return this.pteroClient;
	}

	public OverrideHandler getOverrideHandler() {
		return this.overrideHandler;
	}

	public ServerVersionCache getVersionCache() {
		return this.versionCache;
	}

	public ServerTemplateList getTemplateList() {
		return this.templateList;
	}

	public ServerManager getServerManager() {
		return this.serverManager;
	}

	public TestsuiteVisual getTestsuiteVisual() {
		return this.testsuiteVisual;
	}

	public CommandSuggestion getCommandSuggestion() {
		return this.commandSuggestion;
	}

	public CommandRegistry getCommandRegistry() {
		return this.commandRegistry;
	}

	public TestsuiteConfig getConfig() {
		return this.config;
	}

	public Path getPluginFolder() {
		return this.dataFolder;
	}
}
