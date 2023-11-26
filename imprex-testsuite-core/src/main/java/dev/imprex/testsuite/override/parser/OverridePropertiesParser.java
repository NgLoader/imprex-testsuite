package dev.imprex.testsuite.override.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Properties;

public class OverridePropertiesParser implements OverrideParser {

	private Properties properties = new Properties();

	@Override
	public boolean load(BufferedReader inputStream) {
		try {
			this.properties.load(inputStream);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean save(BufferedWriter outputStream) {
		try {
			this.properties.store(outputStream, null);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean setValue(String key, Object value) {
		String currentValue = this.properties.getProperty(key);
		if (currentValue != null && currentValue.equals(value)) {
			return false;
		}

		this.properties.setProperty(key, value.toString());
		return true;
	}
}