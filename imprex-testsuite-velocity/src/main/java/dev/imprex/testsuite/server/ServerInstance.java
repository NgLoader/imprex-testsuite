package dev.imprex.testsuite.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.Directory;
import com.mattmalec.pterodactyl4j.client.managers.UploadFileAction;
import com.mattmalec.pterodactyl4j.client.managers.WebSocketManager;
import com.mattmalec.pterodactyl4j.entities.Allocation;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.template.ServerTemplate;
import dev.imprex.testsuite.template.ServerTemplateList;
import dev.imprex.testsuite.util.PteroUtil;

public class ServerInstance {

	private final ServerManager manager;
	private final ClientServer server;

	private final ProxyServer proxyServer;
	private final ServerInfo serverInfo;

	private WebSocketManager webSocketManager;

	private ServerTemplate template;
	private AtomicReference<UtilizationState> state = new AtomicReference<>(UtilizationState.OFFLINE);

	public ServerInstance(ServerManager manager, ClientServer server) {
		this.manager = manager;
		this.server = server;
		this.proxyServer = manager.getPlugin().getProxy();

		ServerTemplateList templateList = this.manager.getPlugin().getTemplateList();
		this.template = templateList.getTemplate(this.server.getDescription());

		Allocation allocation = this.server.getPrimaryAllocation();
		this.serverInfo = new ServerInfo(this.getName(), new InetSocketAddress(allocation.getIP(), allocation.getPortInt()));
		this.proxyServer.registerServer(this.serverInfo);
	}

	void changeState(UtilizationState state) {
		this.state.getAndSet(state);
	}

	public CompletableFuture<Void> setupServer() {
		if (!this.hasTemplate()) {
			return CompletableFuture.failedFuture(new NullPointerException("No template instance"));
		}

		CompletableFuture<Void> future = new CompletableFuture<>();

		List<Path> fileList = new CopyOnWriteArrayList<>(this.template.getFiles());
		
		this.server.retrieveDirectory().executeAsync(directoryRoot -> {
			this.updateDirectory(fileList, directoryRoot).whenComplete((__, error) -> {
				if (error != null) {
					future.completeExceptionally(error);
				} else {
					future.complete(null);
				}
				System.gc(); // TODO test phase
			});
		}, error -> {
			error.printStackTrace();
		});

		return future;
	}

	private CompletableFuture<Void> updateDirectory(List<Path> fileList, Directory directoryRoot) {
		int pathPrefix = this.template.getPath().toString().length();

		String directoryPath = directoryRoot.getPath();
		if (directoryPath.startsWith("/")) {
			directoryPath = directoryPath.substring(1);
		}

		UploadFileAction uploadFileAction = directoryRoot.upload();
		boolean needUpload = false;

		List<CompletableFuture<Void>> futureList = new ArrayList<>();
		for (Path file : fileList) {
			String testPath = file.getParent().toString().substring(pathPrefix);
			String tylPath = testPath.startsWith("/") ? testPath.substring(1) : testPath;

			if (!directoryPath.equals(tylPath)) {
				continue;
			}
			fileList.remove(file);

			CompletableFuture<Void> future = new CompletableFuture<>();
			futureList.add(future);

			if (Files.isDirectory(file)) {
				String direcoryName = file.getFileName().toString();
				String fullTylPath = directoryPath.length() == 0 ? direcoryName : (directoryPath + "/" + direcoryName);
				directoryRoot.createFolder(direcoryName).executeAsync(__ -> {
					this.server.retrieveDirectory(fullTylPath).executeAsync(directory -> {
						this.updateDirectory(fileList, directory).whenComplete((___, error) -> {
							if (error != null) {
								future.completeExceptionally(error);
							} else {
								future.complete(null);
							}
						});
					}, error -> {
						future.completeExceptionally(error);
					});
				}, error -> {
					future.completeExceptionally(error);
				});
			} else {
				try {
					InputStream inputStream = Files.newInputStream(file);
					uploadFileAction.addFile(inputStream, file.getFileName().toString());
					needUpload = true;
					future.complete(null);
				} catch (IOException e) {
					future.completeExceptionally(e);
				}
			}
		}

		if (needUpload) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			uploadFileAction.executeAsync(__ -> {
				uploadFileAction.clearFiles(); // we need to clear all files because the input stream will not close after execution !?
				future.complete(null);
			},
			error -> {
				future.completeExceptionally(error);
			});
			futureList.add(future);
		}

		return CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new));
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

	public CompletableFuture<Void> delete() {
		return this.manager.deleteInstance(this.getIdentifier());
	}

	public void close() {
		TestsuiteLogger.info("Removing server instance \"{0}\"", this.getName());
		this.unsubscribe();
		this.proxyServer.unregisterServer(this.serverInfo);
	}

	public UtilizationState getState() {
		return this.state.get();
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