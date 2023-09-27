package dev.imprex.testsuite.command.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class BrigadierCommandDispatcher extends CommandDispatcher<CommandSender> {

	public int executeCommand(CommandSender sender, String command, String label, boolean stripSlash) {
		StringReader stringReader = new StringReader(command);
		if (stripSlash && stringReader.canRead() && stringReader.peek() == '/') {
			stringReader.skip();
		}

		try {
			return super.execute(stringReader, sender);
		} catch (CommandSyntaxException e) {
			TextComponent syntaxComponent = new TextComponent(e.getRawMessage().getString());
			syntaxComponent.setColor(ChatColor.RED);
			sender.sendMessage(syntaxComponent);

			if (e.getInput() != null && e.getCursor() >= 0) {
				int length = Math.min(e.getInput().length(), e.getCursor());
				TextComponent component = new TextComponent("");
				component.setColor(ChatColor.GRAY);
				component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, label));
				if (length > 10) {
					component.addExtra("...");
				}

				component.addExtra(e.getInput().substring(Math.max(0, length - 10), length));
				if (length < e.getInput().length()) {
					TextComponent errorComponent = new TextComponent(e.getInput().substring(length));
					component.setColor(ChatColor.RED);
					errorComponent.setUnderlined(true);
					component.addExtra(errorComponent);
				}

				TranslatableComponent translateComponent = new TranslatableComponent("command.context.here");
				translateComponent.setColor(ChatColor.RED);
				translateComponent.setItalic(true);
				component.addExtra(translateComponent);

				sender.sendMessage(component);
			}
		} catch (Exception e) {
			Text text = new Text(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
			
			TranslatableComponent translateComponent = new TranslatableComponent("command.failed");
			translateComponent.setColor(ChatColor.RED);
			translateComponent.setItalic(true);
			translateComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text));
			sender.sendMessage(translateComponent);
		}
		return 0;
	}
}