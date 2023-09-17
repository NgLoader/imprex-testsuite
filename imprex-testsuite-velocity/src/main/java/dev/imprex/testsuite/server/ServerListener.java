package dev.imprex.testsuite.server;

import com.mattmalec.pterodactyl4j.client.ws.events.StatusUpdateEvent;
import com.mattmalec.pterodactyl4j.client.ws.hooks.ClientSocketListenerAdapter;

public class ServerListener extends ClientSocketListenerAdapter {

	private final ServerInstance instance;

	public ServerListener(ServerInstance instance) {
		this.instance = instance;
	}

	@Override
	public void onStatusUpdate(StatusUpdateEvent event) {
		this.instance.changeState(event.getState());
	}
}