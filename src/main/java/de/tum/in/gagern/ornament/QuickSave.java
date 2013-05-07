package de.tum.in.gagern.ornament;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;

class QuickSave implements ActionListener {

    private static final int TEXTFIELD_COLS = 35;

    private static final String ENABLED_KEY = "quicksaveEnabled";

    private static final String WIDTH_KEY = "quicksaveWidth";

    private static final int WIDTH_DEFAULT = 768;

    private static final String HEIGHT_KEY = "quicksaveHeight";

    private static final int HEIGHT_DEFAULT = 768;

    private static final String DIR_KEY = "quicksaveDir";

//    private static final String DIR_DEFAULT = "/home/mima/snaps";
    private static final String DIR_DEFAULT = I18n._(DIR_KEY);

    private static final String PATTERN_KEY = "quicksavePattern";

    private static final String PATTERN_DEFAULT =
        "morenaments_{0,date,yyyyMMdd_HHmmss}_";

    private static final String FORMAT_KEY = "quicksaveFormat";

    private static final String FORMAT_DEFAULT = "JPG";

    private static final String EXEC_KEY = "quicksaveExec";
    private static final String EXEC_DEFAULT = I18n._(EXEC_KEY);

	protected static final long DISABLE_DELAY = 5000;

    
    private final Ornament main;

    private final Preferences prefs;

    private JComponent configPanel;

    private JCheckBox master;

    private JFormattedTextField width;

    private JFormattedTextField height;

    private JTextField dir;

    private JTextField pattern;

    private JComboBox format;

    private JTextField exec;

	private Container projectionPane = new JPanel() {
        ImageIcon icon = new ImageIcon(QuickSave.class.getResource("projection.png"));
        public void paint(Graphics g) {
        	icon.paintIcon(null, g, 0, 0);
        }
	};

	private JFrame fullscreenframe;

    public QuickSave(Ornament main, JFrame fullScreenWindow) {
        this.main = main;
        this.fullscreenframe = fullScreenWindow;
        prefs = Preferences.userNodeForPackage(QuickSave.class);
        
        projectionPane.setSize(800, 600);
        
    }

    public JComponent getConfigPanel() {
        if (configPanel == null)
            configPanel = createConfigPanel();
        return configPanel;
    }

    private JComponent createConfigPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        GridBagConstraints gbcfw = ((GridBagConstraints)gbc.clone());
        gbcfw.weightx = 1.;
        GridBagConstraints gbcln = ((GridBagConstraints)gbc.clone());
        gbcln.gridwidth = GridBagConstraints.REMAINDER;
        GridBagConstraints gbcfwln = ((GridBagConstraints)gbcfw.clone());
        gbcfwln.gridwidth = GridBagConstraints.REMAINDER;
        GridBagConstraints gbc1fwln = ((GridBagConstraints)gbcfwln.clone());
        gbc1fwln.gridx = 1;

        JPanel panel = new JPanel(), panel2;
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
            I18n._("quicksave")));
        panel.setLayout(new GridBagLayout());

        master = new JCheckBox(I18n._("quicksave.enable"));
        panel.add(master, gbcfwln);

        panel.add(new JLabel(I18n._("quicksave.size")), gbc);
        panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(panel2, gbcfwln);
        width = new JFormattedTextField(NumberFormat.getIntegerInstance());
        width.setColumns(5);
        panel2.add(width);
        panel2.add(new JLabel(I18n._("x")));
        height = new JFormattedTextField(NumberFormat.getIntegerInstance());
        height.setColumns(5);
        panel2.add(height);
        panel2.add(new JLabel(I18n._("px")));

        panel.add(new JLabel(I18n._("quicksave.dir")), gbc);
        dir = new JTextField(TEXTFIELD_COLS);
        panel.add(dir, gbcfw);
        panel.add(new JButton(new BrowseAction(
            dir,
            JFileChooser.DIRECTORIES_ONLY,
            new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }                
                public String getDescription() {
                    return "Directories";
                }                
            })), gbcln);

        panel.add(new JLabel(I18n._("quicksave.pattern")), gbc);
        pattern = new JTextField(TEXTFIELD_COLS);
        panel.add(pattern, gbcfw);
        panel.add(new JButton(new AbstractAction(I18n._("Default")) {
                public void actionPerformed(ActionEvent evnt) {
                    pattern.setText(PATTERN_DEFAULT);
                }                
            }), gbcln);

        panel.add(new JLabel(I18n._("quicksave.format")), gbc);
        format = new JComboBox(new Object[] { "PNG", "JPEG" });
        format.setEditable(false);
        format.setSelectedIndex(0);
        panel.add(format, gbcln);

        panel.add(new JLabel(I18n._("quicksave.exec")), gbc);
        exec = new JTextField(TEXTFIELD_COLS);
        panel.add(exec, gbcfw);
        panel.add(new JButton(new BrowseAction(
            exec, JFileChooser.FILES_ONLY, null)), gbcln);
        panel.add(new JLabel(I18n._("quicksave.aboutexec")), gbc1fwln);

        return panel;
    }

    private class BrowseAction extends AbstractAction {

        private final JTextField textField;

        private final int selectionMode;

        private final FileFilter filter;

        protected JFileChooser fileChooser;

        public BrowseAction(JTextField textField,
                            int selectionMode,
                            FileFilter filter) {
            super(I18n._("Browse"));
            this.textField = textField;
            this.selectionMode = selectionMode;
            this.filter = filter;
        }

        public void actionPerformed(ActionEvent evnt) {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
                prepareFileChooser();
            }
            String str = textField.getText();
            if (str != null && !"".equals(str)) {
                File file = new File(str);
                if (file.exists() && filter.accept(file)) {
                    fileChooser.setSelectedFile(file);
                }
                else {
                    File dir = file.getParentFile();
                    if (dir != null && dir.isDirectory()) {
                        fileChooser.setCurrentDirectory(dir);
                    }
                }
            }
            if (showFileChooser() != JFileChooser.APPROVE_OPTION)
                return;
            File selected = getSelectedFile();
            textField.setText(selected.toString());
        }

        protected void prepareFileChooser() {
            if (filter != null) {
                fileChooser.setAcceptAllFileFilterUsed(false);
                fileChooser.setFileFilter(filter);
            }
            fileChooser.setFileSelectionMode(selectionMode);
        }

        protected int showFileChooser() {
            return fileChooser.showOpenDialog(configPanel);
        }

        protected File getSelectedFile() {
            return fileChooser.getSelectedFile();
        }

    }

    public boolean isEnabled() {
        return prefs.getBoolean(ENABLED_KEY, true);
    }

    private int getWidth() {
        return prefs.getInt(WIDTH_KEY, WIDTH_DEFAULT);
    }

    private int getHeight() {
        return prefs.getInt(HEIGHT_KEY, HEIGHT_DEFAULT);
    }

    private File getDir() {
        return new File(prefs.get(DIR_KEY, DIR_DEFAULT));
    }

    private String getPattern() {
        return prefs.get(PATTERN_KEY, PATTERN_DEFAULT);
    }

    private String getFormat() {
        return prefs.get(FORMAT_KEY, FORMAT_DEFAULT);
    }

    private String getExec() {
        return prefs.get(EXEC_KEY, EXEC_DEFAULT);
    }

    public void loadConfig() {
        master.setSelected(isEnabled());
        width.setValue(new Integer(getWidth()));
        height.setValue(new Integer(getHeight()));
        dir.setText(getDir().toString());
        pattern.setText(getPattern());
        format.setSelectedItem(getFormat());
        exec.setText(getExec());
    }

    public void saveConfig() {
        prefs.putBoolean(ENABLED_KEY, master.isSelected());
        prefs.putInt(WIDTH_KEY, ((Number)width.getValue()).intValue());
        prefs.putInt(HEIGHT_KEY, ((Number)height.getValue()).intValue());
        prefs.put(DIR_KEY, dir.getText());
        prefs.put(PATTERN_KEY, pattern.getText());
        prefs.put(FORMAT_KEY, format.getSelectedItem().toString());
        prefs.put(EXEC_KEY, exec.getText());
    }

    public String checkConfig() {
        if (!master.isSelected())
            return null;
        if (((Number)width.getValue()).intValue() <= 0)
            return I18n._("Width must be positive");
        if (((Number)height.getValue()).intValue() <= 0)
            return I18n._("Height must be positive");
        String s = dir.getText();
        if ("".equals(s))
            return I18n._("No file name specified");
        File f = new File(s);
        if (!f.exists())
            return I18n._("directory_does_not_exist",
                          new Object[] { f.getAbsolutePath() });
        if (!f.isDirectory())
            return I18n._("not_a_directory",
                          new Object[] { f.getAbsolutePath() });
        try {
            MessageFormat mf = new MessageFormat(pattern.getText());
            Format[] formats = mf.getFormatsByArgumentIndex();
            if (formats.length > 1)
                return I18n._("Only one argument allowed");
            formats = mf.getFormats();
            for (int i = 0; i < formats.length; ++i)
                if (formats[i] instanceof DateFormat == false)
                    return I18n._("All formats must be date formats");
        }
        catch (IllegalArgumentException e) {
            s = e.getMessage();
            if (s != null)
                return s;
            return I18n._("Bad file name pattern");
        }
        return null;
    }

    private BufferedImage constructImage() {
        int w = getWidth(), h = getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        PlaneView pv = new PlaneView(main);
        pv.setSize(w, h);
        pv.recalcTiling();
        Graphics2D g = img.createGraphics();
        pv.paint(g);
        g.dispose();
        return img;
    }

    public void actionPerformed(ActionEvent evnt) {
        if (!isEnabled()) return;
        try {
        	File dir = getDir();
        	System.out.println("trying to write snapshot to \""+dir+"\" isDir: "+dir.isDirectory());
            if (!dir.isDirectory()) return;
            String prefix = MessageFormat.format(getPattern(),
                                                 new Object[] { new Date() });
            if (prefix.length() < 3) return;
            if (prefix.indexOf(File.separatorChar) != -1) return;
            String format = getFormat();
            String ext = "." + format.toLowerCase();
            if (".jpeg".equals(ext)) ext = ".jpg";
            File file = File.createTempFile(prefix, ext, dir);
            System.out.println("trying to write snapshot: "+file.getAbsolutePath());
            ImageIO.write(constructImage(), format, file);
            String execStr = getExec();
            System.out.println("execStr: "+execStr);
            if (!"".equals(execStr))
                new CommandExecutor(execStr, file);
            
            final JButton button = (JButton) evnt.getSource();
            
            final Container rp = fullscreenframe.getContentPane();
            
            fullscreenframe.setContentPane(projectionPane);
            projectionPane.validate();
            fullscreenframe.doLayout();
            fullscreenframe.repaint();
            
            new Thread(new Runnable() {
            	public void run() {
            		try {
            			Thread.sleep(DISABLE_DELAY);
            		} catch (Exception e) {}
            		fullscreenframe.setContentPane(rp);
            	}
            }).start();
            
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class CommandExecutor implements Runnable {
        private String[] args;
        private ThreadGroup tg;
        public CommandExecutor(String cmd, File file) {
            /*
        	StringTokenizer st = new StringTokenizer(cmd);
            args = new String[st.countTokens() + 1];
            for (int i = 0; i < args.length - 1; ++i)
                args[i] = st.nextToken();
            args[args.length - 1] = file.getAbsolutePath();
            */
        	String finalCommand = cmd.replace("%FILE%", file.getAbsolutePath());
        	
        	System.out.println("Executing cmd: "+finalCommand);
        	
        	args = finalCommand.split(" ");
            tg = new ThreadGroup(file.getName());
            (new Thread(tg, this, "run")).start();
        }
        public void run() {
            Process p;
            Runtime rt = Runtime.getRuntime();
            try {
                p = rt.exec(args, null);
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
            new Thread(tg, new CommandPiper(p.getInputStream(), System.out),
                       "out").start();
            new Thread(tg, new CommandPiper(p.getErrorStream(), System.err),
                       "err").start();
            try {
                p.waitFor();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class CommandPiper implements Runnable {
        private InputStream in;
        private OutputStream out;
        public CommandPiper(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }
        public void run() {
            int c;
            try {
                while ((c = in.read()) != -1)
                    out.write((byte)c);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

}
