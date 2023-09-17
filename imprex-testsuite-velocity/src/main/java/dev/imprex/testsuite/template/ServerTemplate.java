package dev.imprex.testsuite.template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ServerTemplate {

	private final Path path;
	private final String name;

	public ServerTemplate(ServerTemplateList templateList, Path path) {
		this.path = path;
		this.name = path.getFileName().toString();
	}

	public List<Path> getFiles() {
		List<Path> files = new ArrayList<>();
		try {
			Files.walk(this.path).forEach(file -> {
				if (this.path.equals(file)) {
					return;
				}

				files.add(file);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return files;
	}

	private void zipDirectory(Path file, String fileName, ZipOutputStream zipOut) throws IOException {
		if (Files.isHidden(file)) {
			return;
		}
		if (Files.isDirectory(file)) {
			if (fileName.endsWith("/")) {
				zipOut.putNextEntry(new ZipEntry(fileName));
				zipOut.closeEntry();
			} else {
				zipOut.putNextEntry(new ZipEntry(fileName + "/"));
				zipOut.closeEntry();
			}

			for (Path childFile : Files.walk(file).toList()) {
				this.zipDirectory(childFile, fileName + "/" + childFile.getFileName().toString(), zipOut);
			}
			return;
		}

		try (InputStream inputStream = Files.newInputStream(file)) {
			ZipEntry zipEntry = new ZipEntry(fileName);
			zipOut.putNextEntry(zipEntry);
			byte[] bytes = new byte[1024];
			for (int length = 0; (length = inputStream.read(bytes)) >= 0; ) {
				zipOut.write(bytes, 0, length);
			}
		}
	}

	public Path getPath() {
		return this.path;
	}

	public String getName() {
		return this.name;
	}
}