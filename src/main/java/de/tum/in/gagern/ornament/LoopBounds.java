package de.tum.in.gagern.ornament;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

public class LoopBounds {

    private AffineTransform tr;

    private Point2D.Double pt = new Point2D.Double();

    private int minA, maxA, minB, maxB;

    public LoopBounds() {
        tr = new AffineTransform();
    }

    public LoopBounds(AffineTransform transform)
            throws NoninvertibleTransformException {
        tr = transform.createInverse();
    }

    public LoopBounds(double ax, double ay, double bx, double by)
            throws NoninvertibleTransformException {
        tr = new AffineTransform(ax, ay, bx, by, 0, 0);
        tr = tr.createInverse();
    }

    public AffineTransform getTransform() {
        return tr;
    }

    public int getMinA() {
        return minA;
    }

    public int getMaxA() {
        return maxA;
    }

    public int getMinB() {
        return minB;
    }

    public int getMaxB() {
        return maxB;
    }

    public int getCount() {
        return (maxA - minA + 1)*(maxB - minB + 1);
    }

    public void setTransform(double ax, double ay, double bx, double by)
            throws NoninvertibleTransformException {
        tr.setTransform(ax, ay, bx, by, 0, 0);
        tr = tr.createInverse();
    }

    public void boundsFor(double minX, double minY, double maxX, double maxY) {
        initTo(minX, minY);
        expandTo(maxX, maxY);
        expandTo(maxX, minY);
        expandTo(minX, maxY);
    }

    public void boundsFor(RectangularShape rect) {
        boundsFor(rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY());
    }

    /**
     * @param r the rectangle that will be translated to achieve the cover
     * @param s the rectangle that should be covered
     */
    public void boundsFor(RectangularShape r, RectangularShape s) {
        boundsFor(s.getMinX() - r.getMaxX(), s.getMinY() - r.getMaxY(),
                  s.getMaxX() - r.getMinX(), s.getMaxY() - r.getMinY());
    }

    public void boundsFor(double width, double height) {
        boundsFor(0, 0, width, height);
    }

    private void initTo(double x, double y) {
        pointTo(x, y);
        double a = pt.getX(), b = pt.getY();
        minA = (int)Math.ceil(a);
        maxA = (int)Math.floor(a);
        minB = (int)Math.ceil(b);
        maxB = (int)Math.floor(b);
    }

    private void expandTo(double x, double y) {
        pointTo(x, y);
        double a = pt.getX(), b = pt.getY();
        if (minA > a) minA = (int)Math.ceil(a);
        if (maxA < a) maxA = (int)Math.floor(a);
        if (minB > b) minB = (int)Math.ceil(b);
        if (maxB < b) maxB = (int)Math.floor(b);
    }

    public void stepLowerBounds() {
        --minA;
        --minB;
    }

    private void pointTo(double x, double y) {
        pt.setLocation(x, y);
        tr.transform(pt, pt);
    }

}
