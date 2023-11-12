package dev.imprex.testsuite.override.parser;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OverrideParserRegistry {

	private final Map<String, Constructor<? extends OverrideParser>> parser = new ConcurrentHashMap<>();

	public void register(Class<? extends OverrideParser> parserClass, String... aliases) {
		try {
			for (String alias : aliases) {
					this.parser.put(alias.toLowerCase(), parserClass.getConstructor());
			}
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}

	public OverrideParser createParser(String parserName) {
		try {
			Constructor<? extends OverrideParser> constructor = this.parser.get(parserName);
			if (constructor == null) {
				return null;
			}

			OverrideParser parser = constructor.newInstance();
			return parser;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
}
