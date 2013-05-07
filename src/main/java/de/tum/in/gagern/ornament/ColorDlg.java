package de.tum.in.gagern.ornament;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.Enumeration;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

/**
 * Dialog class to select grid colors.
 *
 * @author Martin von Gagern
 */
class ColorDlg implements Constants, TreeSelectionListener {
    Ornament main;
    Controls controls;
    Color[] colors;
    Color color;
    JDialog dlg;
    JColorChooser cc;
    JTree tree;
    JLabel sub;
    int index;
    boolean changes;

    ColorDlg(Ornament main, Controls controls) {
        this.main=main;
        this.controls=controls;
        for (Component c=main; dlg==null && c!=null; c=c.getParent()) {
            if (c instanceof Frame) dlg=new JDialog((Frame)c);
            else if (c instanceof Dialog) dlg=new JDialog((Dialog)c);
        }
        if (dlg==null) dlg=new JDialog();
        cc=new JColorChooser();
        tree=new JTree(new Node(new Node[] {
            new Node(COLOR_PEN, I18n._("Pen")),
            new Node(COLOR_BACKGROUND, I18n._("Background")),
            new Node(I18n._("Grid"), new Node[] {
                new Node(COLOR_TILE, I18n._("Tile")),
                new Node(COLOR_CELL, I18n._("Cell")),
                new Node(COLOR_BUFFER, I18n._("Buffer")),
                new Node(COLOR_PROPERTIES, I18n._("Properties"), new Node[] {
                    new Node(COLOR_PROPERTIES+1, I18n._("Class A")),
                    new Node(COLOR_PROPERTIES+2, I18n._("Class B")),
                    new Node(COLOR_PROPERTIES+3, I18n._("Class C")),
                    new Node(COLOR_PROPERTIES+4, I18n._("Class D"))})})}));
        DefaultTreeCellRenderer renderer=new DefaultTreeCellRenderer() {
                public Component getTreeCellRendererComponent(JTree tree,
                                Object value, boolean sel, boolean expanded,
                                boolean leaf, int row, boolean focus) {
                    return super.getTreeCellRendererComponent(tree, value, sel,
                                expanded, ((Node)value).index>=0, row, focus);
                }
            };
        URL imgurl=getClass().getResource("ColorDlgIcon.gif");
        ImageIcon icon=new ImageIcon(imgurl);
        renderer.setLeafIcon(icon);
        renderer.setBackgroundNonSelectionColor(cc.getBackground());
        for (int r=0; r<tree.getRowCount(); ++r) tree.expandRow(r);
        tree.setCellRenderer(renderer);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setBackground(cc.getBackground());
        tree.addTreeSelectionListener(this);
        JPanel buttons=new JPanel();
        buttons.setLayout(new FlowLayout());
        buttons.add(new JButton(new AbstractAction(I18n._("OK")) {
                public void actionPerformed(ActionEvent evnt) {
                    onOK();
                }
            }));
        buttons.add(new JButton(new AbstractAction(I18n._("Cancel")) {
                public void actionPerformed(ActionEvent evnt) {
                    dlg.setVisible(false);
                }
            }));
        sub=new JLabel(I18n._("Select a subsection"), JLabel.CENTER);
        dlg.setBackground(cc.getBackground());
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(tree, BorderLayout.WEST);
        dlg.getContentPane().add(buttons, BorderLayout.SOUTH);
        dlg.getContentPane().add(sub);
        dlg.getContentPane().add(cc, BorderLayout.EAST);
        sub.setVisible(false);
        dlg.pack();
    }

    public void show() {
        colors=(Color[])main.colors.clone();
        changes=false;
        if (tree.getSelectionCount()!=1)
            tree.setSelectionRow(0);
        index=((Node)tree.getSelectionPath().getLastPathComponent()).index;
        cc.setColor(color=colors[index]);
        dlg.setVisible(true);
    }

    public void onOK() {
        Color clr=cc.getColor();
        if (index>=0 && color!=null && !color.equals(clr)) {
            colors[index]=clr;
            changes=true;
        }
        dlg.setVisible(false);
        if (changes) {
            main.setColors(colors);
        }
    }

    public void valueChanged(TreeSelectionEvent evnt) {
        Color clr=cc.getColor();
        if (index>=0 && color!=null && !color.equals(clr)) {
            colors[index]=clr;
            changes=true;
        }
        int i=((Node)evnt.getPath().getLastPathComponent()).index;
        if (i>=0) {
            cc.setColor(color=colors[i]);
            cc.setVisible(true);
            sub.setVisible(false);
        }
        else {
            color=null;
            cc.setVisible(false);
            sub.setVisible(true);
        }
        index=i;
    }

    class Node implements TreeNode {
        int index;
        String name;
        Node[] children;
        Node parent;

        Node(Node[] c) {
            this(null, c);
        }

        Node(String n, Node[] c) {
            this(-1, n, c);
        }

        Node(int i, String n) {
            this(i, n, null);
        }

        Node(int i, String n, Node[] c) {
            index=i;
            name=(n==null ? "" : n);
            children=c;
            if (c!=null)
                for (int j=0; j<c.length; ++j) c[j].parent=this;
        }

        public TreeNode getChildAt(int childIndex) {
            return children[childIndex];
        }

        public int getChildCount() {
            return children==null ? 0 : children.length;
        }

        public TreeNode getParent() {
            return parent;
        }

        public int getIndex(TreeNode node) {
            if (children==null) return -1;
            for (int i=0; i<children.length; ++i)
                if (node==children[i]) return i;
            return -1;
        }

        public boolean getAllowsChildren() {
            return children!=null;
        }

        public boolean isLeaf() {
            return children==null;
        }

        public Enumeration children() {
            return new Enumeration() {
                    int i=0;
                    public boolean hasMoreElements() {
                        return i<getChildCount();
                    }
                    public Object nextElement() {
                        return getChildAt(i++);
                    }
                };
        }

        public String toString() {
            return name;
        }

    }
}
