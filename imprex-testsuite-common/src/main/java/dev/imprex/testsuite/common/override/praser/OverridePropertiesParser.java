package dev.imprex.testsuite.common.override.praser;

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
	public void setValue(String key, Object value) {
		this.properties.setProperty(key, value.toString());
	}
}