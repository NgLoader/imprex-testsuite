package dev.imprex.testsuite;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TestsuitePlugin extends JavaPlugin {

	@Override
	public void onEnable() {
	}

	@Override
	public void onDisable() {
		Bukkit.getServer();
		//CraftServer
	}
}