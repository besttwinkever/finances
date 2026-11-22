package me.slkuni.config.parser;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class YamlParser implements ConfigParser {
    private Map<?, ?> config = Map.of();

    @Override
    public void parseConfig(String path) throws IOException {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));

        try (InputStream input = Files.newInputStream(Path.of(path))) {
            if (!(yaml.load(input) instanceof Map<?, ?> values)) {
                throw new IllegalArgumentException("Ожидается YAML-объект в " + path);
            }
            config = values;
        } catch (YAMLException e) {
            throw new IllegalArgumentException("Некорректный YAML в " + path, e);
        }
    }

    @Override
    public String getString(String key) {
        Object value = config;
        for (String part : key.split("\\.")) {
            if (!(value instanceof Map<?, ?> section) || !section.containsKey(part)) {
                throw new IllegalArgumentException("Не найден параметр " + key);
            }
            value = section.get(part);
        }
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(key + " должен быть непустой строкой.");
        }
        return text.strip();
    }
}
