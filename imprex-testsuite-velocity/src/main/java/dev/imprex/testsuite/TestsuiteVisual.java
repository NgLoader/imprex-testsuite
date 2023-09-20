package dev.imprex.testsuite;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.PteroServerStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class TestsuiteVisual implements Runnable {

	private final ProxyServer proxy;
	private final ServerManager serverManager;

	public TestsuiteVisual(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.serverManager = plugin.getServerManager();
	}

	@Override
	public void run() {
		Component component = Component.text(" | ");
		Component componentSpacer = Component.text(" | ");

		for (ServerInstance instance : this.serverManager.getServers()) {
			if (instance.getStatus() == UtilizationState.OFFLINE &&
					instance.getServerStatus() == PteroServerStatus.READY) {
				continue;
			}

			Component componentInstance = Component.text(instance.getName());

			componentInstance.color(switch (instance.getServerStatus()) {
			case INSTALLING -> TextColor.color(200, 40, 200);
			default -> switch (instance.getStatus()) {
					case STARTING -> TextColor.color(0, 200, 0);
					case RUNNING -> TextColor.color(60, 180, 60);
					case STOPPING -> TextColor.color(200, 40, 40);
					default -> TextColor.color(100, 100, 100);
				};
			});
			component.append(componentInstance);
			component.append(componentSpacer);
		}
		this.proxy.getAllPlayers().forEach(player -> player.sendPlayerListFooter(component));
	}
}