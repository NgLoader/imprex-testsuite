package dev.imprex.testsuite.server;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.Utilization;
import com.mattmalec.pterodactyl4j.client.managers.WebSocketManager;
import com.mattmalec.pterodactyl4j.entities.Allocation;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.common.override.OverrideHandler;
import dev.imprex.testsuite.template.ServerTemplate;
import dev.imprex.testsuite.template.ServerTemplateList;
import dev.imprex.testsuite.util.PteroUtil;
import dev.imprex.testsuite.util.PteroUtilization;

public class ServerInstance {

	private static final int STATS_UPDATE_TIME = 15;

	private final ServerManager manager;
	private final ClientServer server;
	private final OverrideHandler overrideHandler;

	private final ProxyServer proxyServer;
	private final ServerInfo serverInfo;

	private WebSocketManager webSocketManager;

	private ServerTemplate template;
	private AtomicReference<Utilization> stats = new AtomicReference<>(new PteroUtilization());
	private AtomicReference<UtilizationState> status = new AtomicReference<>(UtilizationState.OFFLINE);

	public ServerInstance(ServerManager manager, ClientServer server) {
		this.manager = manager;
		this.server = server;
		this.overrideHandler = manager.getPlugin().getOverrideHandler();
		this.proxyServer = manager.getPlugin().getProxy();

		ServerTemplateList templateList = this.manager.getPlugin().getTemplateList();
		this.template = templateList.getTemplate(this.server.getDescription());

		Allocation allocation = this.server.getPrimaryAllocation();
		this.serverInfo = new ServerInfo(this.getName(), new InetSocketAddress(allocation.getIP(), allocation.getPortInt()));
		this.proxyServer.registerServer(this.serverInfo);

		this.scheduleStatsUpdate(STATS_UPDATE_TIME);
	}

	private void scheduleStatsUpdate(int delay) {
		this.server.retrieveUtilization().delay(delay, TimeUnit.SECONDS).executeAsync(
			stats -> {
				this.updateStats(stats);
				this.scheduleStatsUpdate(STATS_UPDATE_TIME);
			},
			error -> {
				TestsuiteLogger.error(error, "Unable to fetch stats from server {0} waiting {0} seconds.",
						this.getName(),
						STATS_UPDATE_TIME * 10);
				this.scheduleStatsUpdate(STATS_UPDATE_TIME * 10);
			});
	}

	void updateStats(Utilization stats) {
		this.stats.getAndSet(stats);
		this.updateStatus(stats.getState());
	}

	void updateStatus(UtilizationState status) {
		if (this.status.get() != status) {
			this.status.getAndSet(status);
			TestsuiteLogger.info("[{0}] State: {1}", this.getName(), status.name());
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

	public void subscribe() {
		if (this.webSocketManager == null) {
			this.webSocketManager = this.server.getWebSocketBuilder()
					.addEventListeners(new ServerListener(this))
					.build();
		}
	}

	public void unsubscribe() {
		if (this.webSocketManager != null) {
			this.webSocketManager.shutdown();
			this.webSocketManager = null;
		}
	}

	public CompletableFuture<Void> executeCommand(String command) {
		return PteroUtil.execute(this.server.sendCommand(command));
	}

	public CompletableFuture<Void> start() {
		return PteroUtil.execute(this.server.start());
	}

	public CompletableFuture<Void> restart() {
		return PteroUtil.execute(this.server.restart());
	}

	public CompletableFuture<Void> stop() {
		return PteroUtil.execute(this.server.stop());
	}

	public CompletableFuture<Void> kill() {
		return PteroUtil.execute(this.server.kill());
	}

	public CompletableFuture<Void> reinstall() {
		return PteroUtil.execute(this.server.getManager().reinstall());
	}

	public CompletableFuture<Void> delete() {
		return this.manager.deleteInstance(this.getIdentifier());
	}

	public void close() {
		TestsuiteLogger.info("Removing server instance \"{0}\"", this.getName());
		this.unsubscribe();
		this.proxyServer.unregisterServer(this.serverInfo);
	}

	public Utilization getStats() {
		return this.stats.get();
	}

	public UtilizationState getStatus() {
		return this.status.get();
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

	public String getName() {
		return this.server.getName();
	}
}