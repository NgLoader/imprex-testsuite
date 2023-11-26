package dev.imprex.testsuite.override;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.override.parser.OverrideParser;
import dev.imprex.testsuite.override.parser.OverrideParserRegistry;
import dev.imprex.testsuite.override.parser.OverridePropertiesParser;
import dev.imprex.testsuite.override.parser.OverrideYamlParser;

public class OverrideHandler {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	public static Map<Path, OverrideConfig> loadOverride(Path path) {
		if (!Files.notExists(path)) {
			return Collections.emptyMap();
		}

		Map<Path, OverrideConfig> overrideFiles = new HashMap<>();
		try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
			JsonObject root = JsonParser.parseReader(bufferedReader).getAsJsonObject();
			for (Entry<String, JsonElement> entry : root.entrySet()) {
				String file = entry.getKey();
				OverrideConfig overrideConfig = GSON.fromJson(entry.getValue(), OverrideConfig.class);
				overrideFiles.put(Path.of(file), overrideConfig);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return overrideFiles;
	}

	public static Map<String, OverrideConfig> loadOverride(String content) {
		Map<String, OverrideConfig> overrideFiles = new HashMap<>();
		try (BufferedReader bufferedReader = new BufferedReader(new StringReader(content))) {
			JsonObject root = JsonParser.parseReader(bufferedReader).getAsJsonObject();
			for (Entry<String, JsonElement> entry : root.entrySet()) {
				String file = entry.getKey();
				OverrideConfig overrideConfig = GSON.fromJson(entry.getValue(), OverrideConfig.class);
				overrideFiles.put(file, overrideConfig);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return overrideFiles;
	}

	private static final OverrideParserRegistry PARSER_REGISTRY = new OverrideParserRegistry();

	static {
		PARSER_REGISTRY.register(OverrideYamlParser.class, "yaml", "yml");
		PARSER_REGISTRY.register(OverridePropertiesParser.class, "properties");
	}

	public static int overrideFile(Path file, OverrideConfig overrideConfig) {
		try {
			Files.createDirectories(file.getParent());

			OverrideParser parser = PARSER_REGISTRY.createParser(overrideConfig.parser());
			if (parser == null) {
				TestsuiteLogger.info("Unable to create parser \"{0}\" for \"{1}\"", overrideConfig.parser(), file);
				return 0;
			}

			try (BufferedReader bufferedReader = Files.newBufferedReader(file)) {
				if (!parser.load(bufferedReader)) {
					TestsuiteLogger.info("Unable to load parser \"{0}\" for \"{1}\"", overrideConfig.parser(), file);
					return 0;
				}
			}

			int changes = 0;
			for (Entry<String, Object> entry : overrideConfig.find().entrySet()) {
				String field = entry.getKey();
				Object value = entry.getValue();
				if (parser.setValue(field, value)) {
					changes++;
				}
			}

			if (changes != 0) {
				try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file)) {
					parser.save(bufferedWriter);
				}
			}

			return changes;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * No stream will be closed after handling!
	 * 
	 * @param bufferedReader
	 * @param overrideConfig
	 * @return
	 */
	public static int overrideFile(BufferedReader bufferedReader, BufferedWriter bufferedWriter, OverrideConfig overrideConfig) {
		OverrideParser parser = PARSER_REGISTRY.createParser(overrideConfig.parser());
		if (parser == null) {
			TestsuiteLogger.info("Unable to create parser \"{0}\"", overrideConfig.parser());
			return 0;
		}

		if (!parser.load(bufferedReader)) {
			TestsuiteLogger.info("Unable to load parser \"{0}\"", overrideConfig.parser());
			return 0;
		}

		int changes = 0;
		for (Entry<String, Object> entry : overrideConfig.find().entrySet()) {
			String field = entry.getKey();
			Object value = entry.getValue();
			if (parser.setValue(field, value)) {
				changes++;
			}
		}

		parser.save(bufferedWriter);
		return changes;
	}
}
