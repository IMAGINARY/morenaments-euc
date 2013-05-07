package de.tum.in.gagern.ornament;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.event.MouseInputAdapter;

class ColorChooser extends JComponent {

    public static final String CHOSEN_COLOR_PROPERTY = "chosenColor";

    private static final int BUFSIZE = 400;

    private static final double TWOPI = Math.PI*2.;

    private static final double TWOTHIRDPI = Math.PI*2./3.;

    private static final double TRIPART = 0.75;

    private RenderingHints renderingHints;

    private double hue;

    /** X coordinate of fully saturated triangle corner */
    private double cx;

    /** Y coordinate of fully saturated triangle corner */
    private double cy;

    /** X coordinate of black triangle corner */
    private double bx;

    /** Y coordinate of black triangle corner */
    private double by;

    /** X coordinate of white triangle corner */
    private double wx;

    /** Y coordinate of white triangle corner */
    private double wy;

    /** Barycentric coordinate of the fully saturated corner (cx, cy) */
    private double cf = 1.;

    /** Barycentric coordinate of the black corner */
    private double bf = 0.;

    /** Barycentric coordinate of the white corner */
    private double wf = 0.;

    /** Barycentric coordinate of (cx,cy) is cfx*x + cfy*y + cfo. */
    private double cfx, cfy, cfo;

    /** Barycentric coordinate of (bx,by) is bfx*x + bfy*y + bfo. */
    private double bfx, bfy, bfo;

    /** Barycentric coordinate of (wx,wy) is wfx*x + wfy*y + wfo. */
    private double wfx, wfy, wfo;

    /** Red component of fully saturated corner */
    private double rFull;

    /** Green component of fully saturated corner */
    private double gFull;

    /** Blue component of fully saturated corner */
    private double bFull;

    private Color chosenColor;

    private BufferedImage buf;

    private boolean needRepaintBuffer;

    public ColorChooser() {
        renderingHints=new RenderingHints(null);
        renderingHints.put(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        renderingHints.put(RenderingHints.KEY_DITHERING,
                           RenderingHints.VALUE_DITHER_DISABLE);
        renderingHints.put(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        setHue(0.);
        MouseHandler mh = new MouseHandler();
        addMouseListener(mh);
        addMouseMotionListener(mh);
        addPropertyChangeListener("background", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evnt) {
                    needRepaintBuffer = true;
                    repaint();
                }
            });
    }

    public Dimension getPreferredSize() {
        return new Dimension(200, 200);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (buf == null) {
            buf = new BufferedImage(BUFSIZE, BUFSIZE,
                                    BufferedImage.TYPE_INT_RGB);
            needRepaintBuffer = true;
        }
        if (needRepaintBuffer) {
            repaintBuffer();
        }
        int w = getWidth(), h = getHeight();
        g.setColor(getBackground());
        g.fillRect(0, 0, w, h);
        int size = Math.min(w, h);
        int x = (getWidth() - size)/2;
        int y = (getHeight() - size)/2;
        int circSize = size / 7;
        g.setColor(getChosenColor());
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D)g.create();
            g2d.setRenderingHints(renderingHints);
            g2d.drawImage(buf, x, y, size, size, this);
            g2d.fillOval(x + 2, y + 2, circSize, circSize);
            g2d.dispose();
        }
        else {
            g.drawImage(buf, x, y, size, size, this);
            g.fillOval(x + 2, y + 2, circSize, circSize);
        }
        int px = (int)Math.round(x + size/2.*(cf*cx + wf*wx + bf*bx + 1.));
        int py = (int)Math.round(y + size/2.*(cf*cy + wf*wy + bf*by + 1.));
        g.setColor(Color.WHITE);
        g.fillOval(px-3, py-3, 6, 6);
        g.setColor(Color.BLACK);
        g.drawOval(px-3, py-3, 6, 6);
    }

    private int hueToRGB(double hue) {
        return Color.HSBtoRGB((float)(hue/TWOPI), 1.f, 1.f);
    }

    private int factorsToRGB(double cf, double wf) {
        int ri = (int)(cf*rFull + wf*255. + 0.5);
        int gi = (int)(cf*gFull + wf*255. + 0.5);
        int bi = (int)(cf*bFull + wf*255. + 0.5);
        return 0xff000000 | (ri << 16) | (gi << 8) | bi;
    }

    public Color getChosenColor() {
        return chosenColor;
    }

    public void setChosenColor(Color color) {
        if (chosenColor.equals(color)) return;
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int white = Math.min(Math.min(r, g), b);
        wf = white/255.;
        bf = (255 - Math.max(Math.max(r, g), b))/255.;
        cf = 1. - wf - bf;
        if (cf < 0.5/255.) { // completely unsaturated, keep hue
            updateColor();
        }
        else {
            rFull = (r - white)/cf;
            gFull = (g - white)/cf;
            bFull = (b - white)/cf;
            float[] hsb = Color.RGBtoHSB((int)(rFull + 0.5),
                                         (int)(gFull + 0.5),
                                         (int)(bFull + 0.5),
                                         null);
            setHue(hsb[0]*TWOPI);
        }
        if (!chosenColor.equals(color))
            throw new AssertionError("Error changing color");
    }

    public void setHue(double hue) {
        /* Initial calculation for the triangle. It has three corners:
         * c - the colored corner with full saturation and brightness
         * b - the black corner with zero saturation and zero brightness
         * w - the white corner with zero saturation and full brightness
         *
         * For each of these we have a set of numbers:
         * ?a - the angle of the corner
         * ?x - the x coordinate of the corner
         * ?y - the y coordinate of the corner
         * ?f - the barycentric coordinate of the current pixel
         *      calculated as ?fx*x + ?fy*y + ?fo
         *
         * To calculate the barycentric coordinates, we basically have
         * to solve a set of linear equations:
         *  cf*cx + bf*bx + wf*wx = x
         *  cf*cy + bf*by + wf*wy = y
         *  cf    + bf    + wf    = 1
         *
         * We do solve this using Cramer's rule, and then split the
         * generic solutions for each ?f into a part ?fx linear in x,
         * a part ?fy linear in y, and a constant offset ?fo. This
         * allows quick calculation of the ?f factors for different
         * values of x and y.
         */
        this.hue = hue;
        double ca = hue;
        cx = Math.cos(ca)*TRIPART;
        cy = Math.sin(ca)*TRIPART;
        double ba = ca-TWOTHIRDPI;
        bx = Math.cos(ba)*TRIPART;
        by = Math.sin(ba)*TRIPART;
        double wa = ca+TWOTHIRDPI;
        wx = Math.cos(wa)*TRIPART;
        wy = Math.sin(wa)*TRIPART;
        double d = cx*by + bx*wy + wx*cy - cx*wy - wx*by - bx*cy;
        cfx = (by - wy)/d;
        cfy = (wx - bx)/d;
        cfo = (bx*wy - wx*by)/d;
        bfx = (wy - cy)/d;
        bfy = (cx - wx)/d;
        bfo = (wx*cy - cx*wy)/d;
        wfx = (cy - by)/d;
        wfy = (bx - cx)/d;
        wfo = (cx*by - bx*cy)/d;
        int rgb = hueToRGB(hue);
        rFull = (rgb>>16)&0xff;
        gFull = (rgb>> 8)&0xff;
        bFull =  rgb     &0xff;
        needRepaintBuffer = true;
        updateColor();
    }

    private void updateColor() {
        Color oldColor = chosenColor;
        Color newColor = new Color(factorsToRGB(cf, wf));
        chosenColor = newColor;
        firePropertyChange(CHOSEN_COLOR_PROPERTY, oldColor, newColor);
        repaint();
    }

    private void repaintBuffer() {
        int bg = 0xff000000 | getBackground().getRGB();
        WritableRaster raster = buf.getRaster();
        int[] row = new int[BUFSIZE];
        for (int yi = 0; yi < BUFSIZE; ++yi) {
            double y = (yi + 0.5)/(BUFSIZE/2) - 1.;
            for (int xi = 0; xi < BUFSIZE; ++xi) {
                double x = (xi + 0.5)/(BUFSIZE/2) - 1.;
                int c = bg;
                double rsq = x*x + y*y;
                if (rsq > TRIPART*TRIPART) {
                    if (rsq < 1.) {
                        c = hueToRGB(Math.atan2(y, x));
                    }
                }
                else {
                    double cf = cfx*x + cfy*y + cfo;
                    double bf = bfx*x + bfy*y + bfo;
                    double wf = wfx*x + wfy*y + wfo;
                    if (cf >= 0. && bf >= 0. && wf >= 0.)
                        c = factorsToRGB(cf, wf);
                }
                row[xi] = c;
            }
            raster.setDataElements(0, yi, BUFSIZE, 1, row);
        }
    }

    private class MouseHandler extends MouseInputAdapter {

        private static final int MODE_NONE = 0;

        private static final int MODE_HUE = 1;

        private static final int MODE_TRI = 2;

        private int mode = 0;

        private double x;

        private double y;

        private void pos(MouseEvent evnt) {
            int size = Math.min(getWidth(), getHeight());
            int dx = (getWidth() - size)/2;
            int dy = (getHeight() - size)/2;
            x = (evnt.getX() - dx + 0.5)/size*2. - 1.;
            y = (evnt.getY() - dy + 0.5)/size*2. - 1.;
        }

        public void mousePressed(MouseEvent evnt) {
            pos(evnt);
            double rsq = x*x + y*y;
            if (mode != MODE_NONE)
                return;
            mode = MODE_NONE;
            if (rsq > TRIPART*TRIPART) {
                if (rsq < 1.) {
                    mode = MODE_HUE;
                }
            }
            else {
                double cf = cfx*x + cfy*y + cfo;
                double bf = bfx*x + bfy*y + bfo;
                double wf = wfx*x + wfy*y + wfo;
                if (cf >= 0. && bf >= 0. && wf >= 0.) {
                    mode = MODE_TRI;
                }
            }
            mouseDragged(evnt);
        }

        public void mouseReleased(MouseEvent evnt) {
            mode = MODE_NONE;
        }

        public void mouseDragged(MouseEvent evnt) {
            pos(evnt);
            switch(mode) {
            case MODE_HUE:
                setHue(Math.atan2(y, x));
                break;
            case MODE_TRI:
                double cf = cfx*x + cfy*y + cfo;
                double bf = bfx*x + bfy*y + bfo;
                double wf = wfx*x + wfy*y + wfo;
                if (cf < 0 || bf < 0 || wf < 0) {
                    if (cf < 0) cf = 0;
                    if (bf < 0) bf = 0;
                    if (wf < 0) wf = 0;
                    double sum = cf + bf + wf;
                    cf /= sum;
                    bf /= sum;
                    wf /= sum;
                }
                ColorChooser.this.cf = cf;
                ColorChooser.this.bf = bf;
                ColorChooser.this.wf = wf;
                updateColor();
                break;
            }
        }

    }

}
