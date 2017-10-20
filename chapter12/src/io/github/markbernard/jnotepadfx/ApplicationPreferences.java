/**
 * The MIT License (MIT)
 * Copyright (c) 2016 Mark Bernard
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in the 
 * Software without restriction, including without limitation the rights to use, copy, 
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, 
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.markbernard.jnotepadfx;

import java.util.prefs.Preferences;

import javafx.stage.Stage;

/**
 * Class to save and load application state between executions.
 * 
 * @author Mark Bernard
 */
public class ApplicationPreferences {
    private static final String WINDOW_X = "window.x";
    private static final String WINDOW_Y = "window.y";
    private static final String WINDOW_WIDTH = "window.width";
    private static final String WINDOW_HEIGHT = "window.height";
    private static final String WINDOW_MAXIMIZED = "window.maximized";
    
    /**
     * Load state from the user preferences store and apply that state to 
     * the provided application window. Use sensible defaults as required.
     * 
     * @param frame The application window.
     */
    public static void loadPrefs(Stage frame) {
        Preferences prefs = Preferences.userNodeForPackage(ApplicationPreferences.class);
        
        frame.setX(prefs.getDouble(WINDOW_X, 0));
        frame.setY(prefs.getDouble(WINDOW_Y, 0));
        frame.setWidth(prefs.getDouble(WINDOW_WIDTH, 800));
        frame.setHeight(prefs.getDouble(WINDOW_HEIGHT, 600));
        frame.setMaximized(prefs.getBoolean(WINDOW_MAXIMIZED, false));
    }

    /**
     * Read state from the provided application window and store it
     * to the user preferences store.
     * 
     * @param frame The main application window.
     */
    public static void savePrefs(Stage frame) {
        Preferences prefs = Preferences.userNodeForPackage(ApplicationPreferences.class);
        
        if (frame.isMaximized()) {
            prefs.putBoolean(WINDOW_MAXIMIZED, true);
        } else {
            prefs.putBoolean(WINDOW_MAXIMIZED, false);
            prefs.putDouble(WINDOW_X, frame.getX());
            prefs.putDouble(WINDOW_Y, frame.getY());
            prefs.putDouble(WINDOW_WIDTH, frame.getWidth());
            prefs.putDouble(WINDOW_HEIGHT, frame.getHeight());
        }
    }
}
