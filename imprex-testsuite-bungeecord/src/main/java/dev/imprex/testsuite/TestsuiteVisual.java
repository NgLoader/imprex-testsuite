package dev.imprex.testsuite;

import com.mattmalec.pterodactyl4j.UtilizationState;

import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;
import dev.imprex.testsuite.util.PteroServerStatus;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TestsuiteVisual implements Runnable {

	private static final BaseComponent[] HEADER = new ComponentBuilder()
			.append(Chat.PREFIX)
			.create();

	private static final BaseComponent SPACER = new ComponentBuilder()
			.append(" | ")
			.color(Chat.Color.GRAY)
			.create()[0];

	private final ProxyServer proxy;
	private final ServerManager serverManager;

	public TestsuiteVisual(TestsuitePlugin plugin) {
		this.proxy = plugin.getProxy();
		this.serverManager = plugin.getServerManager();
	}

	@Override
	public void run() {
		ComponentBuilder builder = new ComponentBuilder(SPACER);
		for (ServerInstance instance : this.serverManager.getServers()) {
			if (instance.getStatus() == UtilizationState.OFFLINE &&
					instance.getServerStatus() == PteroServerStatus.READY) {
				continue;
			}

			builder.append(new ComponentBuilder(instance.getName())
					.color(Chat.Color.statusColor(instance))
					.create());
			builder.append(SPACER);
		}

		BaseComponent[] footer = builder.create();
		for (ProxiedPlayer player : this.proxy.getPlayers()) {
			player.setTabHeader(HEADER, footer);
		}
	}
}