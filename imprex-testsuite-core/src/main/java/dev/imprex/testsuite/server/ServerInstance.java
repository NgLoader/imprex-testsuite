package dev.imprex.testsuite.server;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.Utilization;
import com.mattmalec.pterodactyl4j.client.managers.WebSocketManager;
import com.mattmalec.pterodactyl4j.entities.Allocation;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.api.TestsuiteServer;
import dev.imprex.testsuite.template.ServerTemplate;
import dev.imprex.testsuite.template.ServerTemplateList;
import dev.imprex.testsuite.util.PteroServerStatus;
import dev.imprex.testsuite.util.PteroUtil;
import dev.imprex.testsuite.util.EmptyUtilization;

public class ServerInstance implements Runnable {

	private static final long MAX_INACTIVE_TIME = TimeUnit.MINUTES.toMillis(5);

	private final ServerManager manager;
	private final ClientServer server;

	private final TestsuiteServer proxyServerInfo;

	private WebSocketManager webSocketManager;
	private Lock webSocketLock = new ReentrantLock();

	private ServerTemplate template;
	private AtomicReference<Utilization> stats = new AtomicReference<>(new EmptyUtilization());
	private AtomicReference<UtilizationState> status = new AtomicReference<>(UtilizationState.OFFLINE);
	private AtomicReference<PteroServerStatus> serverStatus = new AtomicReference<>(PteroServerStatus.UNKNOWN);

	private AtomicLong inactiveTime = new AtomicLong(System.currentTimeMillis());
	private AtomicBoolean idleTimeout = new AtomicBoolean(true);

	public ServerInstance(ServerManager manager, ClientServer server) {
		this.manager = manager;
		this.server = server;

		ServerTemplateList templateList = this.manager.getPlugin().getTemplateList();
		this.template = templateList.getTemplate(this.server.getDescription());

		Allocation allocation = this.server.getPrimaryAllocation();

		TestsuitePlugin plugin = this.manager.getPlugin();
		TestsuiteServer serverInfo = plugin.getServer(this.getName());
		if (serverInfo != null) {
			this.proxyServerInfo = serverInfo;
		} else {
			this.proxyServerInfo = plugin.createServer(
					this.getName(),
					allocation.getIP(),
					allocation.getPortInt());
		}

		this.server.retrieveUtilization().executeAsync(stats -> {
			this.updateStats(stats);
		}, error -> {
			TestsuiteLogger.error(error, "[{0}] Error fetching current server stats!", this.getName());
		});
	}

	@Override
	public void run() {
		if (this.template == null) {
			return;
		}

		this.webSocketLock.lock();
		try {
			if (this.webSocketManager == null) {
				return;
			}
		} finally {
			this.webSocketLock.unlock();
		}

		if (this.inactiveTime.get() > System.currentTimeMillis()) {
			return;
		}
		this.resetInactiveTime();

		UtilizationState status = this.status.get();
		if (status == UtilizationState.RUNNING &&
				this.idleTimeout.get() &&
				this.proxyServerInfo.getPlayers().isEmpty()) {
			TestsuiteLogger.broadcast("[{0}] Stopping duo to inactivity", this.getName());
			this.stop();
		} else if (status == UtilizationState.OFFLINE) {
			PteroServerStatus serverStatus = this.serverStatus.get();
			if (serverStatus != PteroServerStatus.INSTALLING) {
				this.unsubscribe();
			}
		}
	}

	void updateServerStatus(PteroServerStatus serverStatus) {
		if (this.serverStatus.getAndSet(serverStatus) != serverStatus) {
			TestsuiteLogger.broadcast("[{0}] Status: {1}", this.getName(), serverStatus.name());

			if (serverStatus == PteroServerStatus.INSTALLING) {
				this.subscribe();
			}
		}
	}

	void updateStats(Utilization stats) {
		this.stats.getAndSet(stats);
		this.updateStatus(stats.getState());
	}

	void updateStatus(UtilizationState status) {
		if (this.status.getAndSet(status) != status) {
			TestsuiteLogger.broadcast("[{0}] Status: {1}", this.getName(), status.name());

			if (status == UtilizationState.OFFLINE) {
				// Wait 5min. for some other actions and then close the web socket
//				this.unsubscribe();
			} else {
				this.subscribe();
			}
		}
	}

	public void subscribe() {
		this.resetInactiveTime();

		this.webSocketLock.lock();
		try {
			if (this.webSocketManager == null) {
				this.resetInactiveTime();
				this.webSocketManager = this.server.getWebSocketBuilder()
						.addEventListeners(new ServerListener(this))
						.build();
				TestsuiteLogger.broadcast("[{0}] Connecting to websocket...", this.getName());
			}
		} finally {
			this.webSocketLock.unlock();
		}
	}

	public void unsubscribe() {
		this.webSocketLock.lock();
		try {
			if (this.webSocketManager != null) {
				try {
					this.webSocketManager.shutdown();
				} catch (IllegalStateException e) {
					// Ignore already shutdown message
				}

				this.webSocketManager = null;
				TestsuiteLogger.broadcast("[{0}] Disonnected from websocket.", this.getName());
			}
		} finally {
			this.webSocketLock.unlock();
		}
	}

	public CompletableFuture<Void> setupServer() {
		if (!this.hasTemplate()) {
			return CompletableFuture.failedFuture(new NullPointerException("No template instance"));
		}

		CompletableFuture<Void> future = new CompletableFuture<>();

		List<Path> fileList = new CopyOnWriteArrayList<>(this.template.getFiles());
		int pathPrefix = this.template.getPath().toString().length();

		Optional<Path> overridePath = fileList.stream()
				.filter(file -> file.toString().substring(pathPrefix).contains("/override.yml"))
				.findFirst();

		PteroUtil.updateDirectory(this.server, pathPrefix, fileList).whenComplete((__, error) -> {
			if (error != null) {
				future.completeExceptionally(error);
			} else {
				future.complete(null);
			}
			System.gc(); // TODO test phase
		});

		if (overridePath.isPresent()) {
//			Map<Path, OverrideConfig> overrides = this.overrideHandler.loadOverride(overridePath.get());
//			for (Entry<Path, OverrideConfig> entry : overrides.entrySet()) {
//				Path path = entry.getKey();
//				OverrideConfig config = entry.getValue();
//
//				if (config.overrideAfterFirstStart()) {
//					continue;
//				}
//
//				Path parentPath = path.getParent();
//				this.server.retrieveDirectory(parentPath.toString()).executeAsync(directory -> {
//					
//				}, error -> {
//					
//				});
//			}
		}

		return future;
	}

	public CompletableFuture<Void> executeCommand(String command) {
		this.subscribe();
		return PteroUtil.execute(this.server.sendCommand(command));
	}

	public CompletableFuture<Void> start() {
		this.subscribe();
		return PteroUtil.execute(this.server.start());
	}

	public CompletableFuture<Void> restart() {
		this.subscribe();
		return PteroUtil.execute(this.server.restart());
	}

	public CompletableFuture<Void> stop() {
		this.subscribe();
		return PteroUtil.execute(this.server.stop());
	}

	public CompletableFuture<Void> kill() {
		this.subscribe();
		return PteroUtil.execute(this.server.kill());
	}

	public CompletableFuture<Void> reinstall() {
		this.subscribe();
		return PteroUtil.execute(this.server.getManager().reinstall());
	}

	public CompletableFuture<Void> delete() {
		return this.manager.deleteInstance(this.getIdentifier());
	}

	public void resetInactiveTime() {
		this.inactiveTime.getAndSet(System.currentTimeMillis() + MAX_INACTIVE_TIME);
	}

	public boolean toggleIdleTimeout() {
		boolean state = this.idleTimeout.get();
		return this.idleTimeout.compareAndSet(state, !state) ? !state : state;
	}

	public void close() {
		TestsuiteLogger.broadcast("Removing server instance \"{0}\"", this.getName());
		this.manager.getPlugin().deleteServer(this.getName());
		this.unsubscribe();
	}

	public WebSocketManager getWebSocketManager() {
		this.webSocketLock.lock();
		try {
			return this.webSocketManager;
		} finally {
			this.webSocketLock.unlock();
		}
	}

	public TestsuiteServer getCurrentServer() {
		return this.proxyServerInfo;
	}

	public Utilization getStats() {
		return this.stats.get();
	}

	public UtilizationState getStatus() {
		return this.status.get();
	}

	public PteroServerStatus getServerStatus() {
		return this.serverStatus.get();
	}

	public boolean hasTemplate() {
		return this.template != null;
	}

	public ServerTemplate getTemplate() {
		return this.template;
	}

	public String getIdentifier() {
		return this.server.getIdentifier();
	}

	public long getInactiveTime() {
		return this.inactiveTime.get();
	}

	public String getName() {
		return this.server.getName();
	}
}