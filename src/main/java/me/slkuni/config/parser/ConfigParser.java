package me.slkuni.config.parser;

import java.io.IOException;

public interface ConfigParser {
    void parseConfig(String path) throws IOException;

    String getString(String key);
}
