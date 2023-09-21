package dev.imprex.testsuite;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.mattmalec.pterodactyl4j.PteroBuilder;
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;

import dev.imprex.testsuite.command.CommandReconnect;
import dev.imprex.testsuite.common.ServerVersionCache;
import dev.imprex.testsuite.common.override.OverrideHandler;
import dev.imprex.testsuite.config.PterodactylConfig;
import dev.imprex.testsuite.config.TestsuiteConfig;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.template.ServerTemplateList;
import okhttp3.OkHttpClient;

@Plugin(
		id = "imprex-testsuite",
		name = "Imprex Test suite",
		version = "1.0.0",
		authors = { "NgLoader" })
public class TestsuitePlugin {

	@Inject
	private ProxyServer proxy;

	private Path dataFolder;
	private TestsuiteConfig config;

	private PteroApplication pteroApplication;
	private PteroClient pteroClient;

	private OverrideHandler overrideHandler;

	private ServerVersionCache versionCache;
	private ServerTemplateList templateList;
	private ServerManager serverManager;

	private TestsuiteVisual testsuiteVisual;

	@Inject
	public TestsuitePlugin(Logger logger, @DataDirectory Path dataFolder) {
		TestsuiteLogger.initialize(logger);
		this.dataFolder = dataFolder;
	}

	@Subscribe
	public void onProxyInitialize(ProxyInitializeEvent event) {
		TestsuiteLogger.initialize(this.proxy);

		this.config = new TestsuiteConfig(this.dataFolder.resolve("config.json"));
		PterodactylConfig tylConfig = this.config.getPterodactylConfig();

		if (!tylConfig.valid()) {
			TestsuiteLogger.info("!!! Invalid testsuite configuration !!!");
			return;
		}

		this.pteroApplication = PteroBuilder.createApplication(tylConfig.url(), tylConfig.applicationToken());
		this.pteroClient = PteroBuilder.create(tylConfig.url(), tylConfig.clientToken())
				.setWebSocketClient(new OkHttpClient.Builder()
						.pingInterval(1, TimeUnit.SECONDS)
						.build())
				.buildClient();

		this.overrideHandler = new OverrideHandler();

		this.versionCache = new ServerVersionCache(dataFolder.resolve("version_cache.json"));
		this.templateList = new ServerTemplateList(this, this.dataFolder.resolve("template"));
		this.serverManager = new ServerManager(this);

		this.testsuiteVisual = new TestsuiteVisual(this);

		Scheduler scheduler = this.proxy.getScheduler();
		scheduler.buildTask(this, this.versionCache)
			.repeat(1, TimeUnit.MINUTES)
			.clearDelay()
			.schedule();

		scheduler.buildTask(this, this.serverManager)
			.repeat(1, TimeUnit.SECONDS)
			.schedule();

		scheduler.buildTask(this, this.testsuiteVisual)
			.repeat(4, TimeUnit.SECONDS)
			.schedule();

		CommandManager commandManager = this.proxy.getCommandManager();

		// reconnect
		CommandMeta commandMetaReconnect = commandManager.metaBuilder("reconnect")
				.aliases("rc")
				.plugin(this)
				.build();

		BrigadierCommand commandReconnect = new BrigadierCommand(new CommandReconnect(this).create());
		commandManager.register(commandMetaReconnect, commandReconnect);

		// testsuite
		CommandMeta commandMeta = commandManager.metaBuilder("testsuite")
			.aliases("test", "ts")
			.plugin(this)
			.build();

		BrigadierCommand command = new BrigadierCommand(new TestsuiteCommand(this).create());
		commandManager.register(commandMeta, command);
	}

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

	public TestsuiteConfig getConfig() {
		return this.config;
	}

	public Path getDataFolder() {
		return this.dataFolder;
	}

	public ProxyServer getProxy() {
		return this.proxy;
	}
}
