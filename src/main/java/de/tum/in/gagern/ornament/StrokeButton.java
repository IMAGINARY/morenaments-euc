package de.tum.in.gagern.ornament;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Component;
import java.awt.BasicStroke;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JToggleButton;

/**
 * User interface element to select a line width.
 *
 * @author Martin von Gagern
 */
public class StrokeButton extends JToggleButton
    implements Icon, ActionListener {

    Ornament main;
    int w;
    BasicStroke s;
    RenderingHints renderingHints;

    public StrokeButton (Ornament ornament, int width) {
        main=ornament;
        w=width;
        s=new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        setIcon(this);
        addActionListener(this);
        renderingHints=new RenderingHints(null);
        renderingHints.add(main.renderingHints);
        renderingHints.put(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        renderingHints.put(RenderingHints.KEY_STROKE_CONTROL,
                           RenderingHints.VALUE_STROKE_PURE);
    }

    public void paintIcon(Component comp, Graphics g, int x, int y) {
        Color oc=g.getColor();
        Graphics2D g2d= (g instanceof Graphics2D) ? (Graphics2D)g : null;
        RenderingHints orh= (g2d!=null) ? g2d.getRenderingHints() : null;
        if (g2d!=null) g2d.setRenderingHints(renderingHints);
        g.setColor(Color.black);
        g.fillOval(x+1, y+1, w+2, w+2);
        if (g2d!=null) g2d.setRenderingHints(orh);
        g.setColor(oc);
    }

    public int getIconWidth() {
        return w+4;
    }

    public int getIconHeight() {
        return w+4;
    }

    public void actionPerformed(ActionEvent e) {
        main.setStroke(s);
    }

}
