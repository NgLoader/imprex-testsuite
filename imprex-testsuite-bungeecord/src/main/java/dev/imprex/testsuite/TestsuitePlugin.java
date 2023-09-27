package dev.imprex.testsuite;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.mattmalec.pterodactyl4j.PteroBuilder;
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;

import dev.imprex.testsuite.command.CommandConnect;
import dev.imprex.testsuite.command.CommandExecute;
import dev.imprex.testsuite.command.CommandReconnect;
import dev.imprex.testsuite.command.CommandRestart;
import dev.imprex.testsuite.command.CommandStop;
import dev.imprex.testsuite.command.CommandTestsuite;
import dev.imprex.testsuite.command.brigadier.BrigadierCommandManager;
import dev.imprex.testsuite.common.ServerVersionCache;
import dev.imprex.testsuite.common.override.OverrideHandler;
import dev.imprex.testsuite.config.PterodactylConfig;
import dev.imprex.testsuite.config.TestsuiteConfig;
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.template.ServerTemplateList;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.event.EventHandler;
import okhttp3.OkHttpClient;

public class TestsuitePlugin extends Plugin implements Listener {

	private TestsuiteConfig config;

	private PteroApplication pteroApplication;
	private PteroClient pteroClient;

	private OverrideHandler overrideHandler;

	private ServerVersionCache versionCache;
	private ServerTemplateList templateList;
	private ServerManager serverManager;

	private BrigadierCommandManager commandManager;

	private TestsuiteVisual testsuiteVisual;

	@Override
	public void onLoad() {
		TestsuiteLogger.initialize(this.getLogger());
	}

	@Override
	public void onEnable() {
		// Initialize configuration
		this.config = new TestsuiteConfig(this.getDataFolder().toPath().resolve("config.json"));
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

		this.versionCache = new ServerVersionCache(this.getFolder().resolve("version_cache.json"));
		this.templateList = new ServerTemplateList(this, this.getFolder().resolve("template"));
		this.serverManager = new ServerManager(this);

		this.testsuiteVisual = new TestsuiteVisual(this);

		// Start scheduler
		TaskScheduler scheduler = this.getProxy().getScheduler();
		scheduler.schedule(this, this.versionCache, 1, 1, TimeUnit.MINUTES);
		scheduler.schedule(this, this.serverManager, 1, 1, TimeUnit.SECONDS);
		scheduler.schedule(this, this.testsuiteVisual, 4, 4, TimeUnit.SECONDS);

		// Register listener
		this.getProxy().getPluginManager().registerListener(this, this);

		// Register commands
		this.commandManager = new BrigadierCommandManager(this);
		this.commandManager.register(new CommandConnect(this).brigadierCommand());
		this.commandManager.register(new CommandExecute(this).brigadierCommand());
		this.commandManager.register(new CommandReconnect(this).brigadierCommand());
		this.commandManager.register(new CommandRestart(this).brigadierCommand());
		this.commandManager.register(new CommandStop(this).brigadierCommand());
		this.commandManager.register(new CommandTestsuite(this).brigadierCommand());
	}

	@Override
	public void onDisable() {
	}

	@EventHandler
	public void onPlayerServerChange(ServerConnectEvent event) {
		Server server = event.getPlayer().getServer();
		if (server != null) {
			ServerInstance pteroServer = this.serverManager.getServer(server.getInfo().getName());
			if (pteroServer == null) {
				return;
			}

			TestsuiteLogger.debug("Reset inactive time on server {1} because {0} disconnected.", event.getPlayer().getName(), pteroServer.getName());
			pteroServer.resetInactiveTime();
		}
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

	public BrigadierCommandManager getCommandManager() {
		return this.commandManager;
	}

	public TestsuiteConfig getConfig() {
		return this.config;
	}

	public Path getFolder() {
		return this.getDataFolder().toPath();
	}
}
