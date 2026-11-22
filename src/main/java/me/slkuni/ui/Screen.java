package me.slkuni.ui;

import java.io.IOException;

public interface Screen {
    ScreenId show() throws IOException;
}
