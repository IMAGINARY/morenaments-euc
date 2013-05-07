package de.tum.in.gagern.ornament;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;

/**
 * Panel containing buttons to control the program.
 * These buttons are used to select the crystallographic group, drawing color,
 * line width and the like. Also the program's menu is created by a method
 * of this class.
 *
 * @author Martin von Gagern
 */
class Controls implements Constants {

    private static final String COLORS_FIXED = "COLORS_FIXED";
    private static final String COLORS_CHOOSER = "COLORS_CHOOSER";

    Ornament main;
    ButtonGroup bgGroup, bgColor, bgStroke;
    AbstractButton customColor;
    GroupButton[] grpButtons;
    ColorButton[] clrButtons;
    ColorChooser colorChooser;
    private JPanel colorPanel;
    private CardLayout colorCards;
    private Action bgImgAction;
    private AbstractButton bgImgItem;

    static final Font buttonFont=new Font("Dialog", 0, 16);
    static final int[] colors={
        0x000000, 0x404040, 0x808080, 0xc0c0c0, 0xffffff,
        0x808000, 0x008000, 0x00ff00, 0x00ff80, 0xffff00,
        0x800080, 0x000080, 0x0000ff, 0x0080ff, 0x00ffff,
        0x800000, 0xff00ff, 0xff0080, 0xff0000, 0xff8000};
    // 004030

    static final int[] strokeWidths={1, 2, 5, 8, 12};

    Controls(Ornament ornament) {
        main=ornament;
    }

    public JPanel createGroupButtons() {
        JPanel panel = new JPanel();
        final GridLayout layout = new GridLayout(0, 1);
        panel.setLayout(layout);
        AbstractButton btn;
        Group[] grps=Group.getGroups();
        grpButtons = new GroupButton[grps.length];
        clrButtons = new ColorButton[colors.length];
        bgGroup=new ButtonGroup();
        for (int i=0; i<grps.length; ++i) {
            grpButtons[i]=new GroupButton(main, grps[i]);
            btn=grpButtons[i];
            bgGroup.add(btn);
            if (i==Ornament.GROUP_P4MM) btn.doClick();
            format(btn);
            panel.add(btn);
        }
        return panel;
    }

    public JPanel createMiscControls() {
        JPanel panel = new JPanel();
        AbstractButton btn;
        panel.setLayout(new BorderLayout());

        // Buttons for specific actions
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        panel.add(buttons, BorderLayout.CENTER);

        // UNDO
        btn=new JButton(I18n._("btn.UNDO"));
        btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    java.util.List l=main.lines;
                    if (l.isEmpty()) return;
                    l.remove(l.size()-1);
                    main.repaintBuffer();
                }
            });
        format(btn);
        buttons.add(btn);

        // CLEAR
        String clear = I18n._("btn.CLEAR");
        if (clear.length() == 3) clear = "  " + clear + "  ";
        btn=new JButton(clear);
        btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    main.clearLines();
                }
            });
        format(btn);
        buttons.add(btn);

        // SNAP
        if (main.kioskMode != null) {
            QuickSave qs = new QuickSave(main, main.kioskMode.fullScreenWindow);
            if (qs.isEnabled()) {
                btn=new JButton(I18n._("btn.SNAP"));
                btn.addActionListener(qs);
                format(btn);
                JPanel pan = new JPanel(new FlowLayout());
                pan.add(btn);
                
                panel.add(pan, BorderLayout.SOUTH);
            }
        }

        // Widths
        JPanel widths = new JPanel();
        widths.setLayout(new GridLayout(0, 5));
        panel.add(widths, BorderLayout.NORTH);
        bgStroke=new ButtonGroup();
        for (int i=0; i<strokeWidths.length; ++i) {
            btn=new StrokeButton(main, strokeWidths[i]);
            bgStroke.add(btn);
            format(btn);
            widths.add(btn);
            if (i==1) btn.doClick();
        }

        return panel;
    }

    public JPanel createColorControls() {
        // Colors
        colorPanel = new JPanel();
        colorCards = new CardLayout();
        colorPanel.setLayout(colorCards);
        
        colorChooser = new ColorChooser();
        colorChooser.setChosenColor(main.colors[0]);
        colorChooser.setBackground(main.colors[1]);
        colorChooser.addPropertyChangeListener(
            ColorChooser.CHOSEN_COLOR_PROPERTY,
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evnt) {
                    main.setColor((Color)evnt.getNewValue());
                }
            });
        colorPanel.add(colorChooser, COLORS_CHOOSER);

        JPanel panel = new JPanel();
        AbstractButton btn;
        panel.setLayout(new GridLayout(0, 5));
        colorPanel.add(panel, COLORS_FIXED);
        bgColor=new ButtonGroup();
        for (int i=0; i<colors.length; ++i) {
            clrButtons[i]=new ColorButton(main, 9, 9, new Color(colors[i]));
            btn=clrButtons[i];
            bgColor.add(btn);
            format(btn);
            panel.add(btn);
            if (i==4) btn.doClick();
        }
        bgColor.add(customColor=new JRadioButton("custom color"));

        return colorPanel;
    }

    void format(AbstractButton btn) {
        btn.setFont(buttonFont);
        if (main.kioskMode != null)
            btn.setFocusPainted(false);
    }

    JMenuBar createMenu() {
        JMenuBar bar=new JMenuBar();
        JMenu m, m2;
        JMenuItem itm;
        ButtonGroup bgrp;

        m=new JMenu(I18n._("File"));
        if (main.kioskMode == null)
            bar.add(m);
        m.add(new JMenuItem(new ExportAction(main)));
        m.add(new JMenuItem(new RecogAction(main)));
        if (main.window != null) {
            m.add(new JMenuItem(new AbstractAction(
                I18n.dots(I18n._("file.Kiosk"))) {
                    KioskMode kioskMode;
                    public void actionPerformed(ActionEvent e) {
                        if (kioskMode==null) {
                            kioskMode=new KioskMode(main, Controls.this);
                            //kioskMode.showUrl(main.doesShowUrl());
                        }
                        kioskMode.configure();
                    }
                }));
        }

        m=new JMenu(I18n._("Grid"));
        bar.add(m);
        Action[] grids={new GridMode(Ornament.GRID_TILE, I18n._("Tile")),
                        new GridMode(Ornament.GRID_CELL, I18n._("Cell")),
                        new GridMode(Ornament.GRID_PROPS, I18n._("Properties")),
                        new GridMode(Ornament.GRID_BUFFER, I18n._("Buffer"))};
        for (int i=0; i<grids.length; ++i)
            m.add(new JCheckBoxMenuItem(grids[i]));

        m=new JMenu(I18n._("Settings"));
        bar.add(m);

        m2=new JMenu(I18n._("setting.Colors"));
        m.add(m2);
        bgrp=new ButtonGroup();
        Action[] colorModes={
            new ColorMode(COLORS_FIXED, I18n._("colors.fixed")),
            new ColorMode(COLORS_CHOOSER, I18n._("colors.chooser")),
        };
        for (int i=0; i<colorModes.length; ++i) {
            itm=new JRadioButtonMenuItem(colorModes[i]);
            bgrp.add(itm);
            m2.add(itm);
            if (i == 0) itm.setSelected(true);
        }

        itm = new JMenuItem(new AbstractAction(
            I18n.dots(I18n._("colors.configure"))) {
                ColorDlg cdlg;
                public void actionPerformed(ActionEvent e) {
                    if (cdlg==null) cdlg=new ColorDlg(main, Controls.this);
                    cdlg.show();
                }
            });
        if (main.kioskMode == null) {
            m2.addSeparator();
            m2.add(itm);
        }

        m2=new JMenu(I18n._("Naming"));
        m.add(m2);
        bgrp=new ButtonGroup();
        Action[] namings={
            new NamingSystem(NAMING_CRYST, I18n._("Crystallographic")),
            new NamingSystem(NAMING_ORBIFOLD_UNICODE, I18n._("Orbifold")),
            new NamingSystem(NAMING_ORBIFOLD_ASCII, I18n._("Orbifold (ASCII)")),
        };
        for (int i=0; i<namings.length; ++i) {
            itm=new JRadioButtonMenuItem(namings[i]);
            bgrp.add(itm);
            m2.add(itm);
            if (i == 0) itm.setSelected(true);
        }

        itm=new JCheckBoxMenuItem(new AbstractAction(
            I18n._("settings.Antialias")) {
                public void actionPerformed(ActionEvent e) {
                    boolean aa=((AbstractButton)e.getSource()).isSelected();
                    main.setAntiAlias(aa);
                }
            });
        itm.setSelected(true);
        m.add(itm);
        m.add(new JCheckBoxMenuItem(new AbstractAction(
            I18n._("setting.Ignore Line Widths")) {
                public void actionPerformed(ActionEvent e) {
                    main.useStrokes=
                        !((AbstractButton)e.getSource()).isSelected();
                    main.repaintBuffer();
                }
            }));
        m.add(new JCheckBoxMenuItem(new AbstractAction(
            I18n._("setting.Grid On Top")) {
                public void actionPerformed(ActionEvent e) {
                    main.gridOnTop=
                        ((AbstractButton)e.getSource()).isSelected();
                    main.repaintBuffer();
                }
            }));
        /*
        if (main.window!=null) {
            m.add(new JCheckBoxMenuItem(new AbstractAction(
            I18n._("setting.Fullscreen")) {
                    public void actionPerformed(ActionEvent e) {
                        Window w;
                        if (((AbstractButton)e.getSource()).isSelected())
                            w=main.window;
                        else w=null;
                        main.getGraphicsConfiguration().getDevice()
                            .setFullScreenWindow(w);
                        main.invalidate();
                        main.window.validate();
                    }
                }));
        }
        */
        m.add(new JCheckBoxMenuItem(new AbstractAction(
            I18n._("setting.Shaded Tiles")) {
                public void actionPerformed(ActionEvent e) {
                    main.tv.shadeMode=
                        ((AbstractButton)e.getSource()).isSelected();
                    main.tv.recalcTiling();
                    main.tv.repaint();
                }
            }));
        bgImgAction = new AbstractAction(I18n._("settings.Background")) {
                public void actionPerformed(ActionEvent e) {
                    boolean on = ((AbstractButton)e.getSource()).isSelected();
                    main.showBackground(on);
                }
            };
        bgImgItem = new JCheckBoxMenuItem(bgImgAction);
        hasBackground(false);
        m.add(bgImgItem);

        m=new JMenu(I18n._("Help"));
        if (main.kioskMode == null)
            bar.add(m);
        m.add(new JMenuItem(new AbstractAction(I18n._("help.Manual")) {
                Documentation doc;
                public void actionPerformed(ActionEvent e) {
                    if (doc==null) {
                        String path=I18n._("help.Manual.path");
                        doc=new Documentation(main, path);
                    }
                    else doc.show();
                }
            }));
        m.add(new JMenuItem(new AbstractAction(I18n._("help.About")) {
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(main,
                        new AboutDlg(), I18n._("about.title"),
                        JOptionPane.PLAIN_MESSAGE);
                }
            }));

        return bar;
    }

    class GridMode extends AbstractAction {
        int mode;
        GridMode(int mode, String str) {
            super(str);
            this.mode=mode;
        }
        public void actionPerformed(ActionEvent e) {
            main.setGrid(mode, ((AbstractButton)e.getSource()).isSelected());
        }
    }

    class NamingSystem extends AbstractAction {
        final int namingSystem;
        NamingSystem(int code, String str) {
            super(str);
            namingSystem=code;
        }
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < grpButtons.length; ++i) {
                grpButtons[i].setName(namingSystem);
                grpButtons[i].invalidate();
            }
        }
    }

    class ColorMode extends AbstractAction {
        final String colorMode;
        ColorMode(String mode, String str) {
            super(str);
            colorMode = mode;
        }
        public void actionPerformed(ActionEvent e) {
            colorCards.show(colorPanel, colorMode);
        }
    }

    public void setGroup(Group group) {
        for (int i = 0; i < grpButtons.length; ++i) {
            GroupButton b = grpButtons[i];
            Group g = b.getSymmetryGroup();
            if (g.equals(group)) {
                b.doClick();
                return;
            }
        }
    }

    void setColor(Color color) {
        colorChooser.setChosenColor(color);
        for (int i = 0; i < clrButtons.length; ++i) {
            ColorButton b = clrButtons[i];
            if (b.c.equals(color)) {
                b.doClick();
                return;
            }
        }
        customColor.doClick();
    }

    void setBackgroundColor(Color color) {
        colorChooser.setBackground(color);
    }

    public void hasBackground(boolean hasBackground) {
        if (bgImgAction == null) return;
        bgImgAction.setEnabled(hasBackground);
        bgImgItem.setSelected(hasBackground);
        /*
        bgImgAction.putValue(Action.SELECTED_KEY,
                             Boolean.valueOf(hasBackground));
        */
    }

}
