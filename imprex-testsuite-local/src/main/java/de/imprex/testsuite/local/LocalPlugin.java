package de.imprex.testsuite.local;

import java.nio.file.Path;
import java.util.logging.Logger;

import dev.imprex.testsuite.TestsuitePlugin;

public class LocalPlugin extends TestsuitePlugin {

	public static void main(String[] args) {
		new LocalPlugin(args);
	}

	public LocalPlugin(String[] args) {
		super.load(Logger.getGlobal(), Path.of("./temp"));
		super.enable(new LocalApi());

		// TODO register commands and implement system console
		// TODO create loop and wait for destruction
	}
}
