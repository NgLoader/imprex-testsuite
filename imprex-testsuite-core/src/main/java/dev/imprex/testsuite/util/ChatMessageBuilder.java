package dev.imprex.testsuite.util;

import java.util.Objects;
import java.util.function.Function;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.api.TestsuiteSender;
import dev.imprex.testsuite.api.TestsuiteServer;
import dev.imprex.testsuite.config.PterodactylConfig;
import dev.imprex.testsuite.server.ServerInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class ChatMessageBuilder<T extends ChatMessageBuilder<?>> {

	private boolean prefix;
	private TestsuiteServer server;

	private Component component = Component.empty();

	private Component constructed = null;
	private boolean dirty = false;

	ChatMessageBuilder() {
		this(true, null);
	}

	ChatMessageBuilder(boolean prefix) {
		this(prefix, null);
	}

	ChatMessageBuilder(TestsuiteServer instance) {
		this(true, instance);
	}

	ChatMessageBuilder(boolean prefix, TestsuiteServer server) {
		this.prefix(prefix);
		this.server(server);
	}

	private ChatMessageBuilder(Component component) {
		if (component != null && !this.component.equals(component)) {
			this.component = component;
			this.dirty = true;
		}
	}

	@SuppressWarnings("unchecked")
	protected T setDirty() {
		this.dirty = true;
		return (T) this;
	}

	public ChatMessageSenderBuilder sender(CommandContext<TestsuiteSender> context) {
		return this.sender(context.getSource());
	}

	public ChatMessageSenderBuilder sender(TestsuiteSender sender) {
		return new ChatMessageSenderBuilder(this.component, sender);
	}

	@SuppressWarnings("unchecked")
	public T prefix(boolean prefix) {
		this.prefix = prefix;
		this.setDirty();
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T server(TestsuiteServer server) {
		this.server = server;
		this.setDirty();
		return (T) this;
	}

	public T append(Component append) {
		return this.modify(component -> component.append(append));
	}

	public T append(String message, Object... args) {
		return this.modify(
				component -> component.append(Component.text(Chat.format(message, args)).color(Chat.Color.GRAY)));
	}

	@SuppressWarnings("unchecked")
	public T modify(Function<Component, Component> modify) {
		this.component = modify.apply(this.component);
		this.setDirty();
		return (T) this;
	}

	public int send(CommandContext<TestsuiteSender> context) {
		Objects.requireNonNull(context, "CommandContext is null");
		return this.send(context.getSource());
	}

	public int send(TestsuiteSender sender) {
		Objects.requireNonNull(sender, "Sender is null");

		sender.sendMessage(this.build());
		return Command.SINGLE_SUCCESS;
	}

	public int broadcast() {
		TestsuiteLogger.broadcast(this);
		return Command.SINGLE_SUCCESS;
	}

	public Component build() {
		if (!this.dirty && this.constructed != null) {
			return this.constructed;
		}
		this.dirty = false;

		Component component = Component.empty();

		if(this.prefix) {
			component = component.append(Chat.PREFIX);
		}

		if (this.server instanceof ServerInstance serverInstance) {
			TestsuitePlugin plugin = serverInstance.getManager().getPlugin();
			PterodactylConfig pterodactylConfig = plugin.getConfig().getPterodactylConfig();
			String serverUrl = pterodactylConfig.url();
			if (!serverUrl.endsWith("/")) {
				serverUrl = serverUrl + "/";
			}
			serverUrl = serverUrl + "server/" + server.getIdentifier();

			Component serverComponent = Component.empty()
					.append(Component.text("[").color(Chat.Color.DARK_GRAY))
					.append(Component.text(server.getName()).color(Chat.Color.GRAY))
					.append(Component.text("]").color(Chat.Color.DARK_GRAY))
					.hoverEvent(HoverEvent.showText(Component.text("Click to open webpanel").color(Chat.Color.LIGHT_GREEN)))
					.clickEvent(ClickEvent.openUrl(serverUrl))
					.appendSpace();

			component = component.append(serverComponent);
		}

		this.constructed = component.append(this.component);
		return this.constructed;
	}

	public static class ChatMessageSenderBuilder extends ChatMessageBuilder<ChatMessageSenderBuilder> {

		private TestsuiteSender sender;

		ChatMessageSenderBuilder(TestsuiteSender sender) {
			this(null, sender);
		}

		private ChatMessageSenderBuilder(Component component, TestsuiteSender sender) {
			super(component);
			this.sender = sender;
		}

		public ChatMessageSenderBuilder sender(TestsuiteSender sender) {
			this.sender = sender;
			return this;
		}

		public int send() {
			return this.send(this.sender);
		}
	}
}