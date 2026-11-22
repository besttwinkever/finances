package me.slkuni.config;

import me.slkuni.config.parser.YamlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTest {
    @TempDir
    Path temp;

    @Test
    void readsStoragePathFromConfig() throws IOException {
        Path configFile = Files.writeString(Path.of(temp.toString(), "config.yaml"), "storage:\n  file: other/wallets.json\n");
        Config config = new Config(configFile.toString(), new YamlParser());
        assertEquals(Path.of("other/wallets.json"), config.getDataFile());
    }

    @Test
    void rejectsEmptyStoragePath() throws IOException {
        Path configFile = Files.writeString(Path.of(temp.toString(), "config.yaml"), "storage:\n  file: ''\n");
        assertThrows(IllegalArgumentException.class, () -> new Config(configFile.toString(), new YamlParser()));
    }
}
