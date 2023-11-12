package dev.imprex.testsuite.api;

import net.kyori.adventure.text.Component;

public interface TestsuiteSender {

	default String getName() {
		return "CONSOLE";
	}

	void sendMessage(Component component);
}