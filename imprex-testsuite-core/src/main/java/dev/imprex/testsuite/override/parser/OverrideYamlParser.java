package dev.imprex.testsuite.override.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import org.yaml.snakeyaml.representer.Representer;

public class OverrideYamlParser implements OverrideParser {

	private static final String SEPERATOR = "\\.";

	private static final DumperOptions DUMPER_OPTIONS = new DumperOptions();
	private static final Representer REPRESENTER = new Representer(DUMPER_OPTIONS);
	private static final Yaml YAML = new Yaml(REPRESENTER, DUMPER_OPTIONS);

	static {
		DUMPER_OPTIONS.setIndent(1);
		DUMPER_OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		REPRESENTER.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		// Source start
		/*
		 * From:
		 * https://github.com/SpigotMC/Spigot-API/blob/master/src/main/java/org/bukkit/
		 * configuration/file/FileConfiguration.java
		 */
		byte[] testBytes = Base64Coder.decode("ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj9AQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVpbXF1eX2BhYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ent8fX4NCg==");
		String testString = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\r\n";
		Charset defaultCharset = Charset.defaultCharset();
		String resultString = new String(testBytes, defaultCharset);
		boolean trueUTF = defaultCharset.name().contains("UTF");
		boolean utf8Override = !testString.equals(resultString) || defaultCharset.equals(Charset.forName("US-ASCII"));
		// Source end

		DUMPER_OPTIONS.setAllowUnicode(trueUTF || utf8Override);
	}

	private Map<?, Object> rootSection;

	@Override
	public boolean load(BufferedReader inputStream) {
		try {
			this.rootSection = YAML.load(inputStream);
			if (this.rootSection == null) {
				this.rootSection = YAML.load("{}");
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean save(BufferedWriter outputStream) {
		try {
			YAML.dump(this.rootSection, outputStream);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean setValue(String key, Object value) {
		String[] keys = key.split(SEPERATOR);
		return this.setValue(keys, 0, this.rootSection, value);
	}

	@SuppressWarnings("unchecked")
	private boolean setValue(String[] keys, int index, Map<?, Object> section, Object newValue) {
		String key = keys[index];
		boolean lastSection = keys.length - 1 == index;

		for (Entry<?, Object> entry : section.entrySet()) {
			String entryKey = entry.getKey().toString();
			Object entryValue = entry.getValue();

			if (entryKey.equals(key)) {
				if (lastSection) {
					if (entryValue.equals(newValue)) {
						return false;
					}
					entry.setValue(newValue);
					return true;
				}

				if (entryValue instanceof Map<?, ?> entrySection) {
					return this.setValue(keys, index + 1, (Map<?, Object>) entrySection, newValue);
				}
			}
		}

		if (lastSection) {
			((Map<Object, Object>) section).put(key, newValue);
			return true;
		} else {
			Map<?, Object> newSection = new HashMap<>();
			((Map<Object, Object>) section).put(key, newSection);
			return this.setValue(keys, index + 1, newSection, newValue);
		}
	}
}