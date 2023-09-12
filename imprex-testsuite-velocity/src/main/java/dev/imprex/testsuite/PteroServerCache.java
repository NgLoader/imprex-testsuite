package dev.imprex.testsuite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import com.velocitypowered.api.scheduler.Scheduler.TaskBuilder;

public class PteroServerCache implements Runnable {

	private final PteroClient pteroClient;

	private final TaskBuilder scheduler;

	private List<ClientServer> serverCache = new ArrayList<>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	public PteroServerCache(TestsuitePlugin plugin) {
		this.pteroClient = plugin.getPteroClient();

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
			} finally {
				this.lock.writeLock().unlock();
			}
		}

		this.scheduler.schedule();
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