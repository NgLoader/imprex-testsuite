package dev.imprex.testsuite;

import com.mattmalec.pterodactyl4j.UtilizationState;

import dev.imprex.testsuite.api.TestsuitePlayer;
import dev.imprex.testsuite.server.ServerInstance;
import dev.imprex.testsuite.server.ServerManager;
import dev.imprex.testsuite.util.Chat;
import dev.imprex.testsuite.util.PteroServerStatus;
import net.kyori.adventure.text.Component;

public class TestsuiteVisual implements Runnable {

	private final TestsuitePlugin plugin;
	private final ServerManager serverManager;

	public TestsuiteVisual(TestsuitePlugin plugin) {
		this.plugin = plugin;
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

			int playerCount = instance.getPlayers().size();
			component = component
					.append(Component.text(instance.getName())
						.color(Chat.Color.statusColor(instance)))
					.appendSpace()
					.append(Component.text("(")
							.color(Chat.Color.DARK_GRAY))
					.append(Component.text(playerCount)
							.color(playerCount > 0 ? Chat.Color.LIGHT_GREEN : Chat.Color.GRAY))
					.append(Component.text(")")
							.color(Chat.Color.DARK_GRAY))
					.append(componentSpacer);
		}

		for (TestsuitePlayer player : this.plugin.getPlayers()) {
			player.sendPlayerListFooter(component);
		}
	}
}