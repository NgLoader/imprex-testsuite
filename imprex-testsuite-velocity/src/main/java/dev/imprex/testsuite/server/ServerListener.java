package dev.imprex.testsuite.server;

import com.mattmalec.pterodactyl4j.client.ws.events.StatusUpdateEvent;
import com.mattmalec.pterodactyl4j.client.ws.events.install.InstallCompletedEvent;
import com.mattmalec.pterodactyl4j.client.ws.events.install.InstallStartedEvent;
import com.mattmalec.pterodactyl4j.client.ws.hooks.ClientSocketListenerAdapter;

import dev.imprex.testsuite.util.PteroServerStatus;

public class ServerListener extends ClientSocketListenerAdapter {

	private final ServerInstance instance;

	public ServerListener(ServerInstance instance) {
		this.instance = instance;
	}

	@Override
	public void onInstallStarted(InstallStartedEvent event) {
		instance.updateServerStatus(PteroServerStatus.INSTALLING);
	}

	@Override
	public void onInstallCompleted(InstallCompletedEvent event) {
		instance.updateServerStatus(PteroServerStatus.READY);
	}

	@Override
	public void onStatusUpdate(StatusUpdateEvent event) {
		this.instance.updateStatus(event.getState());
	}
}