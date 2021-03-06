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
package io.github.markbernard.jnotepad;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

import io.github.markbernard.jnotepad.action.EditAction;
import io.github.markbernard.jnotepad.action.FileAction;
import io.github.markbernard.jnotepad.dialog.SearchDialog;
import io.github.markbernard.jnotepad.dialog.GoToDialog;

/**
 * Main application class.
 * 
 * @author Mark Bernard
 */
public class JNotepad extends JPanel implements WindowListener, DocumentListener {
    private static final long serialVersionUID = -2119311360500754201L;
    private static final String APPLICATION_TITLE = "JNotepad";
    private static final String NEW_FILE_NAME = "Untitled";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("hh:mm aa yyyy-MM-dd");
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private JFrame parentFrame;
    private JTextArea textArea;
    private UndoManager undoManager;
    
    private boolean dirty;
    private String fileName;
    private PrintRequestAttributeSet printRequestAttributeSet;
    private String findTerm;
    private String replaceTerm;
    private boolean findDownDirection;
    private boolean matchCase;

    /**
     * Set up the application before showing the window.
     * 
     * @param parentFrame The main application window.
     */
    public JNotepad(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        fileName = NEW_FILE_NAME;
        ApplicationPreferences.loadPrefs(parentFrame);
        dirty = false;
        setTitle();
        parentFrame.addWindowListener(this);
        setLayout(new BorderLayout());
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.getDocument().addDocumentListener(this);
        undoManager = new UndoManager();
        textArea.getDocument().addUndoableEditListener(undoManager);
        removeKeyStrokes(textArea);
        JScrollPane scroll = new JScrollPane(textArea);
        add(scroll, BorderLayout.CENTER);
        createMenus();
        try {
            List<Image> icons = new ArrayList<>();
            icons.add(ImageIO.read(getClass().getResourceAsStream("/res/icons/JNotepadIconSmall.png")));
            icons.add(ImageIO.read(getClass().getResourceAsStream("/res/icons/JNotepadIcon.png")));
            parentFrame.setIconImages(icons);
        } catch (IOException e) {
            //as long as the image is part of the project this exception should not occur
            e.printStackTrace();
        }
    }
    
    private void createMenus() {
        JMenuBar bar = new JMenuBar();
        parentFrame.setJMenuBar(bar);
        
        JMenu fileMenu = new JMenu(new FileAction());
        bar.add(fileMenu);
        JMenuItem fileNewItem = new JMenuItem(new FileAction.NewAction(this));
        fileMenu.add(fileNewItem);
        JMenuItem fileOpenItem = new JMenuItem(new FileAction.OpenAction(this));
        fileMenu.add(fileOpenItem);
        JMenuItem fileSaveItem = new JMenuItem(new FileAction.SaveAction(this));
        fileMenu.add(fileSaveItem);
        JMenuItem fileSaveAsItem = new JMenuItem(new FileAction.SaveAsAction(this));
        fileMenu.add(fileSaveAsItem);
        fileMenu.addSeparator();
        JMenuItem filePageSetupItem = new JMenuItem(new FileAction.PageSetupAction(this));
        fileMenu.add(filePageSetupItem);
        JMenuItem filePrintItem = new JMenuItem(new FileAction.PrintAction(this));
        fileMenu.add(filePrintItem);
        fileMenu.addSeparator();
        JMenuItem fileExitItem = new JMenuItem(new FileAction.ExitAction(this));
        fileMenu.add(fileExitItem);
        
        JMenu editMenu = new JMenu(new EditAction());
        bar.add(editMenu);
        JMenuItem editUndoItem = new JMenuItem(new EditAction.UndoAction(this));
        editMenu.add(editUndoItem);
        editMenu.addSeparator();
        JMenuItem editCutItem = new JMenuItem(new EditAction.CutAction(this));
        editMenu.add(editCutItem);
        JMenuItem editCopyItem = new JMenuItem(new EditAction.CopyAction(this));
        editMenu.add(editCopyItem);
        JMenuItem editPasteItem = new JMenuItem(new EditAction.PasteAction(this));
        editMenu.add(editPasteItem);
        JMenuItem editDeleteItem = new JMenuItem(new EditAction.DeleteAction(this));
        editMenu.add(editDeleteItem);
        editMenu.addSeparator();
        JMenuItem editFindItem = new JMenuItem(new EditAction.FindAction(this));
        editMenu.add(editFindItem);
        JMenuItem editFindNextItem = new JMenuItem(new EditAction.FindNextAction(this, null));
        editMenu.add(editFindNextItem);
        JMenuItem editReplaceItem = new JMenuItem(new EditAction.ReplaceAction(this));
        editMenu.add(editReplaceItem);
        JMenuItem editGoToItem = new JMenuItem(new EditAction.GoToAction(this));
        editMenu.add(editGoToItem);
        editMenu.addSeparator();
        JMenuItem editSelectAllItem = new JMenuItem(new EditAction.SelectAllAction(this));
        editMenu.add(editSelectAllItem);
        JMenuItem editTimeDateItem = new JMenuItem(new EditAction.TimeDateAction(this));
        editMenu.add(editTimeDateItem);
    }

    @Override
    public void windowActivated(WindowEvent e) {}

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) {
        exit();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void changedUpdate(DocumentEvent e) {
        if (!dirty) {
            dirty = true;
            setTitle();
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        if (!dirty) {
            dirty = true;
            setTitle();
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        if (!dirty) {
            dirty = true;
            setTitle();
        }
    }

    /**
     * Exits the application with cleanup.
     */
    public void exit() {
        DirtyStatus status = isDirty();
        
        boolean saveCompleted = true;
        if (status.equals(DirtyStatus.SAVE_FILE)) {
            saveCompleted = save();
        } else if (status.equals(DirtyStatus.CANCEL_ACTION)) {
            saveCompleted = false;
        }

        if (saveCompleted) {
            ApplicationPreferences.savePrefs(parentFrame);
            System.exit(0);
        }
    }
    
    /**
     * Create a new document while checking to see if the current document is saved.
     */
    public void newDocument() {
        DirtyStatus status = isDirty();
        
        boolean saveCompleted = true;
        if (status.equals(DirtyStatus.SAVE_FILE)) {
            saveCompleted = save();
        } else if (status.equals(DirtyStatus.CANCEL_ACTION)) {
            saveCompleted = false;
        }

        if (saveCompleted) {
            fileName = NEW_FILE_NAME;
            textArea.setText("");
            dirty = false;
            setTitle();
        }
    }
    
    /**
     * Show a file dialog and load the selected file.
     */
    public void load() {
        DirtyStatus result = isDirty();
        
        boolean saveSuccessful = true;
        if (result.equals(DirtyStatus.SAVE_FILE)) {
            saveSuccessful = save();
        } else if (result.equals(DirtyStatus.CANCEL_ACTION)) {
            saveSuccessful = false;
        }
        
        if (saveSuccessful) {
            String filePath = ApplicationPreferences.getCurrentFilePath();
            JFileChooser fileChooser = new JFileChooser(filePath);
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                fileName = selectedFile.getName();
                ApplicationPreferences.setCurrentFilePath(
                        selectedFile.getParentFile().getAbsolutePath());
                loadFile(selectedFile);
            }
        }
    }
    
    /**
     * Save an existing document. If there is no existing document then call saveAs.
     * If a call to saveAs is made and the user cancels during the file save dialog 
     * then false will be returned.
     * 
     * @return true if the save was not interrupted, false otherwise
     */
    public boolean save() {
        if (fileName.equals(NEW_FILE_NAME)) {
            return saveAs();
        } else {
            saveFile(ApplicationPreferences.getCurrentFilePath() + FILE_SEPARATOR + fileName);
            dirty = false;
            setTitle();
            
            return true;
        }
    }
    
    private void saveFile(String path) {
        final JComponent parentComponent = this;
        SwingUtilities.invokeLater(() -> {
            Writer out = null;
            
            try {
                out = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);
                out.write(textArea.getText());
            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(parentComponent, "Unable to create the file: " + path + "\n" + e.getMessage(), "Error loading file", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parentComponent, "Unable to save the file: " + path, "Error loading file", JOptionPane.ERROR_MESSAGE);
            } finally {
                ResourceCleanup.close(out);
            }
        });
    }
    
    /**
     * Display a dialog to the user asking for a location and name of the file
     * to save. If the user cancels the dialog then return false.
     * 
     * @return false if the user cancels the file save dialog, true otherwise
     */
    public boolean saveAs() {
        boolean result = true;
        
        String filePath = ApplicationPreferences.getCurrentFilePath();
        JFileChooser fileChooser = new JFileChooser(filePath);
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            fileName = selectedFile.getName();
            ApplicationPreferences.setCurrentFilePath(
                    selectedFile.getParentFile().getAbsolutePath().replace("\\", "/"));
            saveFile(selectedFile.getAbsolutePath());
            dirty = false;
            setTitle();
        } else {
            result = false;
        }
        
        return result;
    }

    private DirtyStatus isDirty() {
        DirtyStatus result = DirtyStatus.DONT_SAVE_FILE;
        
        if (dirty) {
            String filePath = (fileName.equals(NEW_FILE_NAME) ? fileName : 
                ApplicationPreferences.getCurrentFilePath() + FILE_SEPARATOR + fileName);
            int choice = JOptionPane.showOptionDialog(this, 
                    "Do you want to save changes to " + filePath + "?", 
                    "JNotepad", 
                    JOptionPane.YES_NO_CANCEL_OPTION, 
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    new String[] {"Save", "Don't Save", "Cancel"}, 
                    "Save");
            if (choice == JOptionPane.YES_OPTION) {
                result = DirtyStatus.SAVE_FILE;
            } else if (choice == JOptionPane.NO_OPTION) {
                result = DirtyStatus.DONT_SAVE_FILE;
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                result = DirtyStatus.CANCEL_ACTION;
            }
        }
        
        return result;
    }
    
    /**
     * Load the file at the specified path while checking to see if the current document is saved.
     * 
     * @param path
     */
    public void loadFile(File path) {
        InputStreamReader in = null;
        
        try {
            in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8);
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[32768];
            int read = -1;
            while ((read = in.read(buffer)) > -1) {
                content.append(buffer, 0, read);
            }
            textArea.setText(content.toString());
            undoManager.discardAllEdits();
            dirty = false;
            setTitle();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Unable to find the file: " + path, "Error loading file", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Unable to load the file: " + path, "Error loading file", JOptionPane.ERROR_MESSAGE);
        } finally {
            ResourceCleanup.close(in);
        }
    }

    private void setTitle() {
        SwingUtilities.invokeLater(() -> parentFrame.setTitle((dirty ? "*" : "") + fileName + " - " + APPLICATION_TITLE));
    }

    /**
     * Show the user the page setup dialog and store the users settings.
     */
    public void pageSetup() {
        if (printRequestAttributeSet == null) {
            printRequestAttributeSet = new HashPrintRequestAttributeSet();
        }
        PrinterJob.getPrinterJob().pageDialog(printRequestAttributeSet);
    }

    /**
     * Perform the printing request.
     */
    public void doPrint() {
        try {
            if (printRequestAttributeSet == null) {
                printRequestAttributeSet = new HashPrintRequestAttributeSet();
            }
            textArea.print(null, 
                    new MessageFormat("page {0}"), 
                    true, 
                    PrinterJob.getPrinterJob().getPrintService(), 
                    printRequestAttributeSet, 
                    true);
        } catch (PrinterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undo the last text input
     */
    public void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
    }
    
    /**
     * Cut the selected text and place it in the system clipboard
     */
    public void cut() {
        SwingUtilities.invokeLater(() -> {
            int start = textArea.getSelectionStart();
            textArea.cut();
            textArea.setSelectionStart(start);
            textArea.setSelectionEnd(start);
        });
    }
    
    /**
     * Copy the selected text and place it in the system clipboard
     */
    public void copy() {
        SwingUtilities.invokeLater(() -> textArea.copy());
    }
    
    /**
     * Take contents of the system clipboard, if it is text, and place it at the cursor.
     */
    public void paste() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        DataFlavor[] flavors = clipboard.getAvailableDataFlavors();
        for (DataFlavor flavor : flavors) {
            if (flavor.isFlavorTextType() && flavor.isMimeTypeSerializedObject()) {
                performPaste(flavor, clipboard);
            }
        }
    }
    
    private void performPaste(DataFlavor flavor, Clipboard clipboard) {
        SwingUtilities.invokeLater(() -> {
            try {
                String data = (String)clipboard.getData(flavor);
                int start = textArea.getSelectionStart();
                int end = textArea.getSelectionEnd();
                int length = end - start;
                Document doc = textArea.getDocument();
                try {
                    if (length > 0) {
                        doc.remove(start, length);
                    }
                    doc.insertString(start, data, null);
                    int location = start + data.length();
                    textArea.setSelectionStart(location);
                    textArea.setSelectionEnd(location);
                } catch (BadLocationException e) {
                    //looks like there is nothing to remove
                    //if a mistake occurs we can still try standard paste
                    textArea.paste();
                }
            } catch (UnsupportedFlavorException e) {
                // generally this should not happen since we checked before hand if the flavor passed in was available.
                //if a mistake occurs we can still try standard paste
                textArea.paste();
            } catch (IOException e) {
                //if a mistake occurs we can still try standard paste
                textArea.paste();
            }
        });
    }

    /**
     * Delete the selected text.
     */
    public void delete() {
        SwingUtilities.invokeLater(() -> {
            int start = textArea.getSelectionStart();
            int end = textArea.getSelectionEnd();
            if (start != end) {
                textArea.replaceRange("", start, end);
                textArea.setSelectionEnd(start);
                textArea.setSelectionStart(start);
            }
        });
    }
    
    /**
     * Display a dialog for the user to search the text for something
     */
    public void find() {
        JNotepad self = this;
        SwingUtilities.invokeLater(() -> {
            SearchDialog dialog = new SearchDialog(parentFrame, self, false);
            dialog.setVisible(true);
        });
    }
    
    /**
     * Due to nesting issues this method does not use SwingUtilities.invokeLater.
     * Calling methods should wrap any calls to this method in SwingUtilities.invokeLater to make sure it runs on the EDT
     */
    public void findNext() {
        if (findTerm != null && !findTerm.isEmpty()) {
            String localFindTerm = findTerm;
            if (!matchCase) {
                localFindTerm = localFindTerm.toLowerCase();
            }
            if (findDownDirection) {
                int findStart = textArea.getSelectionEnd();
                String text = textArea.getText();
                if (!matchCase) {
                    text = text.toLowerCase();
                }
                int index = text.indexOf(localFindTerm, findStart);
                if (index > -1) {
                    textArea.setSelectionStart(index);
                    textArea.setSelectionEnd(index + findTerm.length());
                }
            } else {
                int findStart = textArea.getSelectionStart();
                String text = textArea.getText().substring(0, findStart);
                if (!matchCase) {
                    text = text.toLowerCase();
                }
                int index = text.lastIndexOf(localFindTerm);
                if (index > -1) {
                    textArea.setSelectionStart(index);
                    textArea.setSelectionEnd(index + findTerm.length());
                }
            }
        } else {
            find();
        }
    }

    /**
     * 
     */
    public void replace() {
        JNotepad self = this;
        SwingUtilities.invokeLater(() -> {
            SearchDialog dialog = new SearchDialog(parentFrame, self, true);
            dialog.setVisible(true);
        });
    }
    
    /**
     * 
     */
    public void performReplace() {
        SwingUtilities.invokeLater(() -> {
            if (findTerm != null && replaceTerm != null && !findTerm.isEmpty() && 
                    textArea.getSelectionStart() != textArea.getSelectionEnd()) {
                String selectedText = textArea.getSelectedText();
                if ((matchCase && findTerm.equals(selectedText)) || 
                        (!matchCase && findTerm.equalsIgnoreCase(selectedText))) {
                    textArea.replaceSelection(replaceTerm);
                    textArea.setSelectionStart(textArea.getSelectionEnd());
                    findNext();
                }
            }
        });
    }
    
    /**
     * 
     */
    public void replaceAll() {
        SwingUtilities.invokeLater(() -> {
            if (findTerm != null && replaceTerm != null && !findTerm.isEmpty()) {
                textArea.setCaretPosition(0);
                findDownDirection = true;
                findNext();
                while (findTerm.equals(textArea.getSelectedText())) {
                    textArea.replaceSelection(replaceTerm);
                    textArea.setSelectionStart(textArea.getSelectionEnd());
                    findNext();
                }
            }
        });
    }
    
    /**
     * Place the cursor on the beginning of the line number select by the user.
     */
    public void goTo() {
        JNotepad self = this;
        SwingUtilities.invokeLater(() -> {
            GoToDialog goToDialog = new GoToDialog(parentFrame, self);
            if (goToDialog.showDialog()) {
                int lineNumber = goToDialog.getLineNumber() - 1;
                
                if (lineNumber >= 0 && lineNumber <= textArea.getLineCount()) {
                    try {
                        textArea.setCaretPosition(textArea.getLineStartOffset(lineNumber));
                    } catch (BadLocationException e) {
                        // should not occur since we already 
                        // checked if the lineNumber is in range.
                        e.printStackTrace();
                    }
                }
            }
            goToDialog.dispose();
        });
    }

    /**
     * Selects all text in the JTextArea.
     */
    public void selectAll() {
        SwingUtilities.invokeLater(() -> {
            textArea.setSelectionStart(0);        
            textArea.setSelectionEnd(textArea.getText().length());
        });
    }
    
    /**
     * Insert the time and date into the text a the current cursor location.
     */
    public void timeDate() {
        SwingUtilities.invokeLater(() -> {
            String timeDateString = DATE_FORMAT.format(new Date());
            int start = textArea.getSelectionStart();
            textArea.replaceSelection(timeDateString);
            textArea.setCaretPosition(start + timeDateString.length());
        });
    }

    /**
     * @param findTerm the findTerm to set
     */
    public void setFindTerm(String findTerm) {
        this.findTerm = findTerm;
    }

    /**
     * @param replaceTerm the replaceTerm to set
     */
    public void setReplaceTerm(String replaceTerm) {
        this.replaceTerm = replaceTerm;
    }

    /**
     * @param matchCase the matchCase to set
     */
    public void setMatchCase(boolean matchCase) {
        this.matchCase = matchCase;
    }

    /**
     * @param findDownDirection the findDownDirection to set
     */
    public void setFindDownDirection(boolean findDownDirection) {
        this.findDownDirection = findDownDirection;
    }

    private void removeKeyStrokes(JTextArea textArea) {
        InputMap targetMap = null;
        InputMap parentMap = textArea.getInputMap();
        
        while (parentMap != null) {
            targetMap = parentMap;
            parentMap = targetMap.getParent();
        }
        
        targetMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
        targetMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        targetMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        targetMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
    }
    /**
     * Main application starting point.
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            // Make the application look native.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // System look and feel is always present.
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            JNotepad jNotepad = new JNotepad(frame);
            frame.add(jNotepad, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }
    
    enum DirtyStatus {
        SAVE_FILE, DONT_SAVE_FILE, CANCEL_ACTION;
    }
}
