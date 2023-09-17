package dev.imprex.testsuite.server;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.mattmalec.pterodactyl4j.DataType;
import com.mattmalec.pterodactyl4j.EnvironmentValue;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationAllocation;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationEgg;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationServer;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationUser;
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.common.ServerType;
import dev.imprex.testsuite.config.ServerConfig;
import dev.imprex.testsuite.template.ServerTemplate;

public class ServerManager implements Runnable {

	private static final long UPDATE_TIME = TimeUnit.SECONDS.toMillis(30);

	private final TestsuitePlugin plugin;
	private final PteroApplication pteroApplication;
	private final PteroClient pteroClient;

	private final ServerConfig serverConfig;
	private final AllocationAssignment allocationAssignment;

	private final Map<String, ServerInstance> serverInstances = new ConcurrentHashMap<>();

	private AtomicLong lastUpdate = new AtomicLong(0);

	public ServerManager(TestsuitePlugin plugin) {
		this.plugin = plugin;
		this.pteroApplication = plugin.getPteroApplication();
		this.pteroClient = plugin.getPteroClient();

		this.serverConfig = plugin.getConfig().getServerConfig();
		this.allocationAssignment = new AllocationAssignment(plugin, plugin.getConfig().getAllocationConfig());
	}

	@Override
	public void run() {
		if (this.lastUpdate.get() > System.currentTimeMillis()) {
			return;
		}
		this.lastUpdate.getAndSet(System.currentTimeMillis() + UPDATE_TIME);

		this.pteroClient.retrieveServers().executeAsync((serverList) -> {
			for (ClientServer server : serverList) {
				if (server.isSuspended()) {
					continue;
				}

				String identifier = server.getIdentifier();
				if (this.serverInstances.get(identifier) == null) {
					ServerInstance instance = new ServerInstance(this, server);
					this.serverInstances.put(identifier, instance);

					TestsuiteLogger.info("Detected server instance \"{0}\"", instance.getName());
				}
			}

			for (Iterator<ServerInstance> iterator = this.serverInstances.values().iterator(); iterator.hasNext(); ) {
				ServerInstance instance = iterator.next();
				boolean found = false;
				for (ClientServer server : serverList) {
					if (server.getIdentifier().equals(instance.getIdentifier())) {
						found = true;
						break;
					}
				}

				if (!found) {
					try {
						instance.close();
					} finally {
						iterator.remove();
					}
				}
			}
		}, (error) -> {
			error.printStackTrace();
		});
	}

	public CompletableFuture<Void> deleteInstance(String identifier) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		this.pteroApplication.retrieveServerById(identifier).executeAsync(server -> {
			server.getController().delete(false).executeAsync((__) -> {
				this.lastUpdate.getAndSet(0);
				future.complete(__);
			}, future::completeExceptionally);
		}, future::completeExceptionally);

		return future;
	}

	public CompletableFuture<ApplicationServer> create(ServerTemplate template, ServerType type, String version) {
		return this.create(
				String.format("%s-%s-%s", template.getName(), type.name().toLowerCase(), version),
				template.getName(),
				type,
				version);
	}

	public CompletableFuture<ApplicationServer> create(String name, ServerType type, String version) {
		return this.create(
				name,
				"",
				type,
				version);
	}

	private CompletableFuture<ApplicationServer> create(String name, String description, ServerType type, String version) {
		CompletableFuture<ApplicationServer> future = new CompletableFuture<>();

		CompletableFuture<ApplicationUser> futureUser = this.receiveUser();
		CompletableFuture<ApplicationEgg> futureEgg = this.receiveEgg(type);
		CompletableFuture<ApplicationAllocation> futureAllocation = this.allocationAssignment.createAllocation();
		CompletableFuture.allOf(futureUser, futureEgg, futureAllocation).whenComplete((__, error) -> {
			if (error != null) {
				future.completeExceptionally(error);
				return;
			}

			ApplicationUser user = futureUser.getNow(null);
			if (user == null) {
				future.completeExceptionally(new IllegalArgumentException("unable to locale user"));
				return;
			}

			ApplicationEgg egg = futureEgg.getNow(null);
			if (egg == null) {
				future.completeExceptionally(new IllegalArgumentException("unable to locate egg"));
				return;
			}

			ApplicationAllocation allocation = futureAllocation.getNow(null);
			if (allocation == null) {
				future.completeExceptionally(new IllegalArgumentException("unable to create allocation"));
				return;
			}

			Map<String, EnvironmentValue<?>> environment = new HashMap<>();
			environment.put("MINECRAFT_VERSION", EnvironmentValue.of(version));

			this.pteroApplication.createServer()
				.setName(name)
				.setDescription(description)
				.setOwner(user)
				.setEgg(egg)
				.setAllocation(allocation)
				.setEnvironment(environment)
				.setDisk(serverConfig.storage(), DataType.GB)
				.setMemory(serverConfig.memory(), DataType.GB)
				.executeAsync((server) -> {
					this.lastUpdate.getAndSet(0);
					future.complete(server);
				}, future::completeExceptionally);
		});

		return future;
	}

	private CompletableFuture<ApplicationEgg> receiveEgg(ServerType type) {
		CompletableFuture<ApplicationEgg> future = new CompletableFuture<>();
		this.pteroApplication.retrieveEggs().executeAsync(
			eggs -> {
				for (ApplicationEgg egg : eggs) {
					if (egg.getName().equalsIgnoreCase(type.getEggName())) {
						future.complete(egg);
						return;
					}
				}
				future.complete(null);
			}, future::completeExceptionally);
		return future;
	}

	private CompletableFuture<ApplicationUser> receiveUser() {
		CompletableFuture<ApplicationUser> future = new CompletableFuture<>();
		this.pteroClient.retrieveAccount().executeAsync(
			account -> {
				this.pteroApplication.retrieveUserById(account.getId()).executeAsync(
						future::complete,
						future::completeExceptionally);
			}, future::completeExceptionally);
		return future;
	}

	public ServerInstance getServer(String name) {
		for (ServerInstance instance : this.serverInstances.values()) {
			if (instance.getName().equalsIgnoreCase(name)) {
				return instance;
			}
		}
		return null;
	}

	public Collection<ServerInstance> getServers() {
		return Collections.unmodifiableCollection(this.serverInstances.values());
	}

	public TestsuitePlugin getPlugin() {
		return this.plugin;
	}
}