package dev.imprex.testsuite;

import java.io.IOException;
import java.nio.file.Path;

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

@Plugin(
		id = "imprex-testsuite",
		name = "Imprex Test suite",
		version = "1.0.0",
		authors = { "NgLoader" })
public class Testsuite {

	@Inject
	private ProxyServer proxy;

	@Inject Logger logger;

	private TestsuiteConfig config;

	private PteroApplication pteroApplication;
	private PteroClient pteroClient;
	private PteroServerCache serverCache;

	@Inject
	public Testsuite(@DataDirectory Path dataFolder) {
		this.config = new TestsuiteConfig(dataFolder);
	}

	@Subscribe
	public void onProxyInitialize(ProxyInitializeEvent event) {
		try {
			this.config.load();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		if (!this.config.validate()) {
			logger.warn("Invalid config!");
			return;
		}

		this.pteroApplication = PteroBuilder.createApplication(this.config.getUrl(), this.config.getApplicationToken());
		this.pteroClient = PteroBuilder.createClient(this.config.getUrl(), this.config.getClientToken());
		this.serverCache = new PteroServerCache(this);

		CommandManager commandManager = this.proxy.getCommandManager();
		CommandMeta commandMeta = commandManager.metaBuilder("testsuite")
				.aliases("test")
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

	public PteroServerCache getServerCache() {
		return this.serverCache;
	}

	public TestsuiteConfig getConfig() {
		return this.config;
	}

	public ProxyServer getProxy() {
		return this.proxy;
	}
}