package dev.imprex.testsuite.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.TestsuitePlugin;

public class ServerTemplateList {

	private final Path templatePath;

	private final Map<String, ServerTemplate> templates = new ConcurrentHashMap<>();

	public ServerTemplateList(TestsuitePlugin plugin, Path templatePath) {
		this.templatePath = templatePath;
		this.refreshTemplateList();
	}

	public void refreshTemplateList() {
		try {
			Files.createDirectories(this.templatePath);

			Files.walk(this.templatePath, 1).forEach(path -> {
				if (!Files.isDirectory(path) || this.templatePath.equals(path)) {
					return;
				}

				String name = path.getFileName().toString().toLowerCase();
				if (this.templates.containsKey(name)) {
					return;
				}

				ServerTemplate template = new ServerTemplate(this, path);
				this.templates.put(name, template);

				TestsuiteLogger.info("Detected new template \"{0}\"", template.getName());
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ServerTemplate getTemplate(String name) {
		return this.templates.get(name.toLowerCase());
	}

	public Collection<ServerTemplate> getTemplates() {
		return Collections.unmodifiableCollection(this.templates.values());
	}
}