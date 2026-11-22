package me.slkuni.config;

import me.slkuni.config.parser.ConfigParser;

import java.io.IOException;
import java.nio.file.Path;

public class Config {
    private final Path dataFile;

    public Config(String path, ConfigParser parser) throws IOException {
        parser.parseConfig(path);
        dataFile = Path.of(parser.getString("storage.file"));
    }

    public Path getDataFile() {
        return dataFile;
    }
}
