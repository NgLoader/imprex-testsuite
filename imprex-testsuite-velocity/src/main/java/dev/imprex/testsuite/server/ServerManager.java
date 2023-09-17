package dev.imprex.testsuite.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mattmalec.pterodactyl4j.DataType;
import com.mattmalec.pterodactyl4j.EnvironmentValue;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationAllocation;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationEgg;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationServer;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationUser;
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.common.ServerType;
import dev.imprex.testsuite.config.ServerConfig;

public class ServerManager {

	private final PteroApplication pteroApplication;
	private final PteroClient pteroClient;

	private final AllocationAssignment allocationAssignment;
	private final ServerConfig serverConfig;

	public ServerManager(TestsuitePlugin plugin) {
		this.pteroApplication = plugin.getPteroApplication();
		this.pteroClient = plugin.getPteroClient();
		this.serverConfig = plugin.getConfig().getServerConfig();
		this.allocationAssignment = new AllocationAssignment(plugin, plugin.getConfig().getAllocationConfig());
	}

	public CompletableFuture<ApplicationServer> create(String name, ServerType type, String version) {
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
				.setOwner(user)
				.setEgg(egg)
				.setAllocation(allocation)
				.setEnvironment(environment)
				.setDisk(serverConfig.storage(), DataType.GB)
				.setMemory(serverConfig.memory(), DataType.GB)
				.executeAsync(future::complete, future::completeExceptionally);
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
}