package de.tum.in.gagern.ornament;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class MinGrid {

    private final int a1, b1, a2, b2;

    private final double x1, y1, x2, y2;

    public MinGrid(double ax, double ay, double bx, double by) {
        boolean swapped = false;
        double ad = ax*ax + ay*ay, bd = bx*bx + by*by;
        if (bd < ad) {
            // Swap vectors such that distance of a is minimal
            double t;
            t = ad; ad = bd; bd = t;
            t = ax; ax = bx; bx = t;
            t = ay; ay = by; by = t;
            swapped = true;
        }
        debug("a=" + ax + "/" + ay + ", b=" + bx + "/" + by);

        // the number of loop iterations is limited such that the distance
        // of the examined parallel of a never exceeds the length of a
        int nb = (int)Math.ceil(Math.abs((ax*ax + ay*ay)/(ax*by - bx*ay)));
        debug("nb = " + nb);

        double cd = ad;
        int ca = 1, cb = 0;
        double cx = ax, cy = ay;
        for (int db = 1; db < nb; ++db) {
            // To find best da on line da*a + db*b,
            // minimize absolute component parallel to a, ideally to zero:
            // <da*a + db*b, a> = 0
            // <da*a, a> + <db*b, a> = 0
            // da <a, a> + db <b, a> = 0
            // da = -db * <b, a> / <a, a>
            int da = -(int)Math.round(db * (ax*bx + ay*by) / ad);
            double dx = da*ax + db*bx, dy = da*ay + db*by, dd = dx*dx + dy*dy;
            debug("d=" + da + "*a+" + db + "*b=" + dx + "/" + dy);
            if (dd < cd) {
                cd = dd;
                ca = da;
                cb = db;
                cx = dx;
                cy = dy;
            }
        }
        debug("c=" + ca + "*a+" + cb + "*b=" + cx + "/" + cy);
        // now c is a grid vector with minimal length

        int[] euc = extendedEuclid(ca, cb);
        int da = euc[2], db = euc[1];
        double dx = da*ax + db*bx, dy = da*ay + db*by;
        debug("d'=" + da + "*a+" + db + "*b=" + dx + "/" + dy);
        int shift = (int)Math.ceil(-(cx*dx + cy*dy)/cd);
        da += shift*ca;
        db += shift*cb;
        dx = da*ax + db*bx;
        dy = da*ay + db*by;
        double dd = dx*dx + dy*dy;
        debug("d=" + da + "*a+" + db + "*b=" + dx + "/" + dy);

        int ea = ca-da, eb = cb-db;
        double ex = ea*ax + eb*bx, ey = ea*ay + eb*by, ed = ex*ex + ey*ey;
        debug("e=" + ea + "*a+" + eb + "*b=" + ex + "/" + ey);
        if (ed < dd) {
            // Swap vector such that second shortest in triangle
            // is choosen second
            da = ea;
            db = eb;
            dx = ex;
            dy = ey;
            dd = ed;
            debug("d=" + da + "*a+" + db + "*b=" + dx + "/" + dy);
        }
        if (cx + dx < 0 || cx + dx == 0 && cy + dy > 0) {
            ca = -ca; cb = -cb; cx = -cx; cy = -cy;
            da = -da; db = -db; dx = -dx; dy = -dy;
            debug("c=" + ca + "*a+" + cb + "*b=" + cx + "/" + cy);
            debug("d=" + da + "*a+" + db + "*b=" + dx + "/" + dy);
        }

        if (swapped) {
            this.a1 = cb;
            this.b1 = ca;
            this.a2 = db;
            this.b2 = da;
        }
        else {
            this.a1 = ca;
            this.b1 = cb;
            this.a2 = da;
            this.b2 = db;
        }
        this.x1 = cx;
        this.y1 = cy;
        this.x2 = dx;
        this.y2 = dy;
    }

    public MinGrid(AffineTransform t) {
        this(t.getScaleX(), t.getShearY(), t.getShearX(), t.getScaleY());
    }

    /**
     * Extended euclidean algorithm.
     * Given two integers <i>a</i> and <i>b</i>,
     * this method calculates <i>g</i>, <i>x</i> and <i>y</i> such that
     * <ul>
     * <li><i>g</i> = gcd(<i>a</i>, <i>b</i>)</li>
     * <li><i>g</i> = <i>x</i>*<i>a</i> + <i>y</i>*<i>b</i></li>
     * <li><i>g</i> ≧ 0</li>
     * </ul>
     * @return the three element array [g, x, y]
     */
    private static int[] extendedEuclid (final int a, final int b) {
        int tmp, a1, x1, y1, a2, x2, y2;
        if (Math.abs(a) >= Math.abs(b)) {
            a1 = a; x1 = 1; y1 = 0;
            a2 = b; x2 = 0; y2 = 1;
        }
        else {
            a1 = b; x1 = 0; y1 = 1;
            a2 = a; x2 = 1; y2 = 0;
        }
        assert Math.abs(a1) >= Math.abs(a2);
        assert a1 == x1*a + y1*b;
        assert a2 == x2*a + y2*b;
        while (a2 != 0) {
            int q = a1 / a2;
            tmp = a1; a1 = a2; a2 = tmp - q*a2;
            tmp = x1; x1 = x2; x2 = tmp - q*x2;
            tmp = y1; y1 = y2; y2 = tmp - q*y2;
            assert a1 == x1*a + y1*b;
            assert a2 == x2*a + y2*b;
        }
        if (a1<0) return new int[] { -a1, -x1, -y1 };
        else return new int[] { a1, x1, y1 };
    }

    public int getA1() { return a1; }
    public int getB1() { return b1; }
    public int getA2() { return a2; }
    public int getB2() { return b2; }
    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }

    public AffineTransform getTransform() {
        return new AffineTransform(x1, y1, x2, y2, 0, 0);
    }

    public AffineTransform getTransform(boolean posDirection) {
        if ((x1*y2 - x2*y1 > 0.) == posDirection)
            return new AffineTransform(x1, y1, x2, y2, 0, 0);
        else
            return new AffineTransform(x2, y2, x1, y1, 0, 0);
    }

    public Point getAB1() {
        return new Point(a1, b1);
    }

    public Point getAB2() {
        return new Point(a2, b2);
    }

    public Point2D getXY1() {
        return new Point2D.Double(x1, y1);
    }

    public Point2D getXY2() {
        return new Point2D.Double(x2, y2);
    }

    public String toString() {
        return "MinGrid[" + x1 + "/" + y1 + ", " + x2 + "/" + y2 + "]";
    }

    private void debug(String msg) {
        // System.err.println(msg);
    }

    public static void main(String[] args) {
        new MinGrid(4, 0, 5, 1);
    }

}
