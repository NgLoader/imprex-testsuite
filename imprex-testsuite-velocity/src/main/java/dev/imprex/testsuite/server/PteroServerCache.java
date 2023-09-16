package dev.imprex.testsuite.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mattmalec.pterodactyl4j.DataType;
import com.mattmalec.pterodactyl4j.EnvironmentValue;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationAllocation;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationEgg;
import com.mattmalec.pterodactyl4j.application.entities.ApplicationUser;
import com.mattmalec.pterodactyl4j.application.entities.Node;
import com.mattmalec.pterodactyl4j.application.entities.PteroApplication;
import com.mattmalec.pterodactyl4j.application.managers.ServerCreationAction;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.Scheduler.TaskBuilder;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.common.ServerType;

public class PteroServerCache implements Runnable {

	private final ProxyServer proxy;
	private final PteroApplication pteroApplication;
	private final PteroClient pteroClient;

	private final ApplicationUser pteroClientUser;
	private final Node pteroNode;

	private final TaskBuilder scheduler;

	private List<ClientServer> serverCache = new ArrayList<>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	public PteroServerCache(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.pteroClient = plugin.getPteroClient();
		this.pteroApplication = plugin.getPteroApplication();

		this.pteroClientUser = this.pteroApplication.retrieveUserById(this.pteroClient.retrieveAccount().execute().getId()).execute();	
		this.pteroNode = this.pteroApplication.retrieveNodeById(3).execute();

		this.scheduler = plugin.getProxy().getScheduler().buildTask(plugin, this)
				.delay(10, TimeUnit.SECONDS);

		this.run();
	}

	@Override
	public void run() {
		this.pteroClient.retrieveServers().executeAsync(this::handleResult);
	}

	private void handleResult(List<ClientServer> servers) {
		if (servers != null) {
			this.lock.writeLock().lock();
			try {
				this.serverCache.clear();
				this.serverCache.addAll(servers);

				for (ClientServer server : this.serverCache) {
					boolean exist = false;
					for (RegisteredServer existServer : this.proxy.getAllServers()) {
						if (existServer.getServerInfo().getName().equals(server.getName())) {
							exist = true;
							break;
						}
					}
					if (exist) {
						break;
					}

					ServerInfo info = new ServerInfo(server.getName(), new InetSocketAddress(server.getPrimaryAllocation().getIP(), server.getPrimaryAllocation().getPortInt()));
					this.proxy.registerServer(info);
				}

				for (RegisteredServer server : this.proxy.getAllServers()) {
					if (server.getServerInfo().getName().startsWith("lobby")) {
						continue;
					}

					if (this.getServer(server.getServerInfo().getName()) == null) {
						this.proxy.unregisterServer(server.getServerInfo());
					}
				}
			} finally {
				this.lock.writeLock().unlock();
			}
		}

		this.scheduler.schedule();
	}

	public ServerCreationAction createServer(String name, ServerType serverType, String version) {
		this.pteroNode.getAllocationManager().createAllocation()
			.setAlias("imprex-testsuite-intern")
			.setIP("172.19.0.1")
			.setPorts("25" + new Random().nextInt(999))
			.execute();

		ApplicationAllocation allocationResult = null;
		for (ApplicationAllocation allocation : this.pteroApplication.retrieveAllocationsByNode(pteroNode).execute()) {
			if (!allocation.isAssigned() && allocation.getAlias().equalsIgnoreCase("imprex-testsuite-intern")) {
				allocationResult = allocation;
				break;
			}
		}

		if (allocationResult == null) {
			System.out.println("allocation null");
			return null;
		}

		ApplicationEgg eggResult = null;
		for (ApplicationEgg egg : this.pteroApplication.retrieveEggs().execute()) {
			if (egg.getName().equalsIgnoreCase(serverType.getEggName())) {
				eggResult = egg;
				break;
			}
			System.out.println(egg.getName());
		}

		if (eggResult == null) {
			System.out.println("EGG null");
			return null;
		}

		ServerCreationAction action = this.pteroApplication.createServer();
		action.setName(name);
		action.setOwner(this.pteroClientUser);
		action.setAllocation(allocationResult);
		action.setMemory(4, DataType.GB);
		action.setDisk(2, DataType.GB);
		action.setEgg(eggResult);
		Map<String, EnvironmentValue<?>> env = new HashMap<>();
		env.put("MINECRAFT_VERSION", EnvironmentValue.of(version));
		action.setEnvironment(env);
		return action;
	}

	public ClientServer getServer(String name) {
		return this.serverCache.stream().filter(server -> server.getName().equalsIgnoreCase(name)).findFirst().orElseGet(() -> null);
	}

	public List<ClientServer> getServers() {
		this.lock.readLock().lock();
		try {
			return Collections.unmodifiableList(this.serverCache);
		} finally {
			this.lock.readLock().unlock();
		}
	}
}