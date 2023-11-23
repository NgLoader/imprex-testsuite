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

	private final OverrideParserRegistry parserRegistry = new OverrideParserRegistry();

	public OverrideHandler() {
		this.parserRegistry.register(OverrideYamlParser.class, "yaml", "yml");
		this.parserRegistry.register(OverridePropertiesParser.class, "properties");
	}

	public boolean overrideFile(Path file, OverrideConfig overrideConfig) {
		try {
			Files.createDirectories(file.getParent());

			OverrideParser parser = this.parserRegistry.createParser(overrideConfig.parser());
			if (parser == null) {
				System.out.println("Unable to create parser \"" + overrideConfig.parser() + "\" for \"" + file + "\"");
				return false;
			}

			try (BufferedReader bufferedReader = Files.newBufferedReader(file)) {
				if (!parser.load(bufferedReader)) {
					System.out.println("Unable to load parser \"" + overrideConfig.parser() + "\" for \"" + file + "\"");
					return false;
				}
			}

			for (Entry<String, Object> entry : overrideConfig.find().entrySet()) {
				String field = entry.getKey();
				Object value = entry.getValue();
				parser.setValue(field, value);
			}

			try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file)) {
				parser.save(bufferedWriter);
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * No stream will be closed after handling!
	 * 
	 * @param bufferedReader
	 * @param overrideConfig
	 * @return
	 */
	public boolean overrideFile(BufferedReader bufferedReader, BufferedWriter bufferedWriter, OverrideConfig overrideConfig) {
		OverrideParser parser = this.parserRegistry.createParser(overrideConfig.parser());
		if (parser == null) {
			System.out.println("Unable to create parser \"" + overrideConfig.parser() + "\"");
			return false;
		}

		if (!parser.load(bufferedReader)) {
			System.out.println("Unable to load parser \"" + overrideConfig.parser() + "\"");
			return false;
		}

		for (Entry<String, Object> entry : overrideConfig.find().entrySet()) {
			String field = entry.getKey();
			Object value = entry.getValue();
			parser.setValue(field, value);
		}

		parser.save(bufferedWriter);
		return true;
	}
}
