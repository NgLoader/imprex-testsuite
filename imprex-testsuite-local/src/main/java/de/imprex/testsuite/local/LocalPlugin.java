package de.imprex.testsuite.local;

import java.nio.file.Path;
import java.util.logging.Logger;

import dev.imprex.testsuite.TestsuitePlugin;

public class LocalPlugin extends TestsuitePlugin {

	public static void main(String[] args) {
		new LocalPlugin(args);
	}

	public LocalPlugin(String[] args) {
		super.load(Logger.getGlobal(), Path.of("./tmp"));
		super.enable(new LocalApi());

		LocalCommand command = new LocalCommand(this);
		command.readConsole();
	}
}
