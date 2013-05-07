package de.tum.in.gagern.ornament;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.NumberFormat;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

class KioskMode
implements AWTEventListener, KeyEventDispatcher, ActionListener {

    private static final String RESET_TIMEOUT_KEY = "kioskResetTimeout";

    private static final int RESET_TIMEOUT_DEFAULT = 1000*60*10; // 10 minutes

    private static final int RESET_TIMEOUT_MIN = 1000; // 1 second

    private static final double MINUTE = 1000*60; // 1 minute

    private static final String CURSOR_INVISIBLE_KEY = "kioskCursorInvisible";

    private static final boolean CURSOR_INVISIBLE_DEFAULT = true;

    private final Ornament main;

    private final Controls controls;

    private final Preferences prefs;

    private final QuickSave quickSave;

    private JDialog dialog;

    private boolean showurl;

    private JCheckBox cursorInvisible;

    private JFormattedTextField timeoutField;

    private boolean active;

    JFrame fullScreenWindow;

    private GraphicsDevice gd;

    private Toolkit toolkit;

    private Cursor invisibleCursor;

    private Timer timer;

    public KioskMode(Ornament main, Controls controls) {
        this.main = main;
        this.controls = controls;
        prefs = Preferences.userNodeForPackage(KioskMode.class);
        quickSave = new QuickSave(main, fullScreenWindow);
        timer = new Timer(RESET_TIMEOUT_DEFAULT, this);
        timer.setRepeats(false);
    }

    private JDialog initDialog() {
        if (main.window == null ||
            main.window instanceof JFrame == false)
            return null;
        JDialog dlg = new JDialog((JFrame)main.window, I18n._("file.Kiosk"));
        Container cp = dlg.getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.PAGE_AXIS));
        JPanel panel;

        panel = new JPanel();
        cp.add(panel);
        cursorInvisible = new JCheckBox(I18n._("kiosk.cursor invisible"));
        panel.add(cursorInvisible);

        panel = new JPanel();
        cp.add(panel);
        panel.add(new JLabel(I18n._("kiosk.reset1")));
        NumberFormat format = NumberFormat.getInstance();
        format.setParseIntegerOnly(false);
        format.setMaximumFractionDigits(3);
        format.setMinimumFractionDigits(0);
        timeoutField = new JFormattedTextField(format);
        timeoutField.setColumns(6);
        panel.add(timeoutField);
        panel.add(new JLabel(I18n._("kiosk.reset2")));

        cp.add(quickSave.getConfigPanel());

        panel = new JPanel();
        cp.add(panel);
        panel.add(new JLabel(I18n._("kiosk.howtoexit")));

        panel = new JPanel();
        cp.add(panel);
        panel.add(new JButton(new AbstractAction(I18n._("kiosk.Start")) {
                public void actionPerformed(ActionEvent evnt) {
                    if (checkAndSave())
                        start();
                }
            }));
        panel.add(new JButton(new AbstractAction(I18n._("kiosk.Save")) {
                public void actionPerformed(ActionEvent evnt) {
                    checkAndSave();
                }
            }));
        panel.add(new JButton(new AbstractAction(I18n._("Cancel")) {
                public void actionPerformed(ActionEvent evnt) {
                    dialog.setVisible(false);
                }
            }));

        dlg.pack();
        return dlg;
    }

    public void showUrl(boolean showurl) {
        this.showurl = showurl;
    }

    private int getTimeout() {
        return prefs.getInt(RESET_TIMEOUT_KEY, RESET_TIMEOUT_DEFAULT);
    }

    private boolean isCursorInvisible() {
        return prefs.getBoolean(CURSOR_INVISIBLE_KEY, CURSOR_INVISIBLE_DEFAULT);
    }

    public void configure() {
        if (dialog == null) {
            dialog = initDialog();
            if (dialog == null)
                return;
        }
        loadConfig();
        dialog.setLocationRelativeTo(main);
        dialog.setVisible(true);
    }

    private void loadConfig() {
        cursorInvisible.setSelected(isCursorInvisible());
        timeoutField.setValue(new Double(getTimeout()/MINUTE));
        quickSave.loadConfig();
    }

    private void saveConfig() {
        prefs.putBoolean(CURSOR_INVISIBLE_KEY, cursorInvisible.isSelected());
        double timeoutMinutes = ((Number)timeoutField.getValue()).doubleValue();
        int timeout = (int)Math.round(timeoutMinutes*MINUTE);
        if (timeout < RESET_TIMEOUT_MIN)
            timeout = RESET_TIMEOUT_MIN;
        prefs.putInt(RESET_TIMEOUT_KEY, timeout);
        quickSave.saveConfig();
    }

    private boolean checkAndSave() {
        String error = quickSave.checkConfig();
        if (error != null) {
            JOptionPane.showMessageDialog(dialog, error, I18n._("Error"),
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        saveConfig();
        dialog.setVisible(false);
        return true;
    }

    public void start() {
        if (active) stop();
        if (main == null) {
            fullScreenWindow = new JFrame();
        }
        else {
            if (main.window == null ||
                main.window instanceof JFrame == false)
                return;
            JFrame mainFrame = (JFrame)main.window;
            GraphicsConfiguration gc = mainFrame.getGraphicsConfiguration();
            fullScreenWindow = new JFrame(gc);
        }
        gd = fullScreenWindow.getGraphicsConfiguration().getDevice();
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(this);
        fullScreenWindow.setUndecorated(true);
        fullScreenWindow.setVisible(true);
        toolkit = fullScreenWindow.getToolkit();
        
        gd.setFullScreenWindow(fullScreenWindow);
        active = true;
        actionPerformed(null);
        fullScreenWindow.requestFocus();
        fullScreenWindow.validate();
        fullScreenWindow.repaint();
        if (isCursorInvisible()) {
            if (invisibleCursor == null)
                invisibleCursor = makeInvisibleCursor();
            fullScreenWindow.setCursor(invisibleCursor);
        }
        toolkit.addAWTEventListener(this, AWTEvent.MOUSE_MOTION_EVENT_MASK);
        timer.setInitialDelay(getTimeout());
        timer.start();
    }

    public void stop() {
        if (!active) return;
        timer.stop();
        toolkit.removeAWTEventListener(this);
        gd.setFullScreenWindow(null);
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .removeKeyEventDispatcher(this);
        active = false;
        fullScreenWindow.dispose();
        fullScreenWindow = null;
        if (main == null)
            System.exit(0);
    }

    private Cursor makeInvisibleCursor() {
        BufferedImage img;
        img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        return toolkit.createCustomCursor(img, new Point(0, 0),
                                          I18n._("invisible cursor"));
    }

    // AWTEventListener

    public void eventDispatched(AWTEvent evnt) {
        if (active) timer.restart();
    }

    // KeyEventDispatcher

    public boolean dispatchKeyEvent(KeyEvent evnt) {
        if (active && evnt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            stop();
            return true;
        }
        return false;
    }

    // ActionListener for Timer

    public void actionPerformed(ActionEvent evnt) {
        if (!active) return;
        Ornament ornament = new Ornament(fullScreenWindow, false, this);
        //ornament.showUrl(showurl);
        fullScreenWindow.setContentPane(ornament);
        fullScreenWindow.validate();
        fullScreenWindow.repaint();
    }

}
