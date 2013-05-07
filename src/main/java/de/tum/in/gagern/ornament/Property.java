package de.tum.in.gagern.ornament;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class describing the geometric properties of a crystallographic group.
 * Those properties all have the ability to draw themselves.
 *
 * @author Martin von Gagern
 */

public abstract class Property {

    /**
     * The transitivity class of this property
     */
    protected int tc;

    protected Property(int tc) {
        this.tc=tc;
    }

    public int getTC() {
        return tc;
    }

    /**
     * Draws a symbol for this property.
     * @param g the graphics context to draw the symbol in
     * @param t the transform specifying size and offset of the tile to paint
     */
    public abstract void paint(Graphics2D g, AffineTransform t);

    /**
     * Common class for point-like properties.
     */
    private abstract static class Point extends Property {
        static final int SYMB_SIZE=4;
        Point2D ab, xy;
        Point(double a, double b, int tc) {
            super(tc);
            ab=new Point2D.Double(a, b);
            xy=new Point2D.Double();
        }
        public void paint(Graphics2D g, AffineTransform t) {
            t.transform(ab, xy);
            paint(g, (int)Math.round(xy.getX()), (int)Math.round(xy.getY()));
        }
        public abstract void paint(Graphics2D g, int x, int y);
    }

    /**
     * Property describing a rotation by 180 degrees.
     */
    public static class Turn2 extends Point {
        public Turn2(double a, double b) {
            this(a, b, 0);
        }
        public Turn2(double a, double b, int tc) {
            super(a, b, tc);
        }
        public void paint(Graphics2D g, int x, int y) {
            g.drawOval(x-SYMB_SIZE, y-SYMB_SIZE, 2*SYMB_SIZE, 2*SYMB_SIZE);
        }
    }

    /**
     * Property describing a rotation by 120 degrees.
     */
    public static class Turn3 extends Point {
        public Turn3(double a, double b) {
            this(a, b, 0);
        }
        public Turn3(double a, double b, int tc) {
            super(a, b, tc);
        }
        public void paint(Graphics2D g, int x, int y) {
            g.fillOval(x-SYMB_SIZE, y-SYMB_SIZE, 2*SYMB_SIZE+1, 2*SYMB_SIZE+1);
        }
    }

    /**
     * Property describing a rotation by 90 degrees.
     */
    public static class Turn4 extends Point {
        public Turn4(double a, double b) {
            this(a, b, 0);
        }
        public Turn4(double a, double b, int tc) {
            super(a, b, tc);
        }
        public void paint(Graphics2D g, int x, int y) {
            g.drawRect(x-SYMB_SIZE, y-SYMB_SIZE, 2*SYMB_SIZE, 2*SYMB_SIZE);
        }
    }

    /**
     * Property describing a rotation by 60 degrees.
     */
    public static class Turn6 extends Point {
        public Turn6(double a, double b) {
            this(a, b, 0);
        }
        public Turn6(double a, double b, int tc) {
            super(a, b, tc);
        }
        public void paint(Graphics2D g, int x, int y) {
            g.fillRect(x-SYMB_SIZE, y-SYMB_SIZE, 2*SYMB_SIZE+1, 2*SYMB_SIZE+1);
        }
    }

    /**
     * Common class for line-like properties.
     */
    private abstract static class Line extends Property {
        Point2D ab1, ab2, xy1, xy2;
        Line2D line;
        Line(double a1, double b1, double a2, double b2, int tc) {
            super(tc);
            ab1=new Point2D.Double(a1, b1);
            ab2=new Point2D.Double(a2, b2);
            xy1=new Point2D.Double();
            xy2=new Point2D.Double();
            line=new Line2D.Double();
        }
        public void paint(Graphics2D g, AffineTransform t) {
            t.transform(ab1, xy1);
            t.transform(ab2, xy2);
            line.setLine(xy1, xy2);
            paint(g, line);
        }
        public abstract void paint(Graphics2D g, Line2D line);
    }

    /**
     * Property describing a mirror axis.
     */
    public static class Mirror extends Line {
        public Mirror(double a1, double b1, double a2, double b2) {
            this(a1, b1, a2, b2, 0);
        }
        public Mirror(double a1, double b1, double a2, double b2, int tc) {
            super(a1, b1, a2, b2, tc);
        }
        public void paint(Graphics2D g, Line2D line) {
            g.draw(line);
        }
    }

    /**
     * Property describing a glide reflection axis.
     */
    public static class Glide extends Line {
        static final BasicStroke STROKE=
            new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                            0.f, new float[]{3, 10}, 0.f);
        public Glide(double a1, double b1, double a2, double b2) {
            this(a1, b1, a2, b2, 0);
        }
        public Glide(double a1, double b1, double a2, double b2, int tc) {
            super(a1, b1, a2, b2, tc);
        }
        public void paint(Graphics2D g, Line2D line) {
            Stroke oldStroke=g.getStroke();
            g.setStroke(STROKE);
            g.draw(line);
            g.setStroke(oldStroke);
        }
    }

}
