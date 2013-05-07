package de.tum.in.gagern.ornament.export;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import javax.imageio.ImageIO;

import de.tum.in.gagern.ornament.Group;
import de.tum.in.gagern.ornament.LinPath;
import de.tum.in.gagern.ornament.LoopBounds;
import de.tum.in.gagern.ornament.Ornament;

public class BitmapExport extends Export {

    private boolean hasAlpha;

    private String format;

    private String description;

    private AffineTransform vectorTransform;

    private LoopBounds bounds;

    private BufferedImage img;

    private Graphics2D g2d;

    public BitmapExport(Ornament ornament, String format, boolean hasAlpha,
                        String description) {
        this(ornament, format, new String[] { format }, hasAlpha, description);
    }

    public BitmapExport(Ornament ornament, String format, String[] extensions,
                        boolean hasAlpha, String description) {
        super(ornament, format);
        this.format = format;
        this.hasAlpha = hasAlpha;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    private Collection getClosestPoints(int ax, int ay, int bx, int by,
                                        int maxLengthSq) {
        if (ax < 0) { ax = -ax; ay = -ay; }
        if (bx < 0) { bx = -bx; by = -by; }
        if (ax < bx) {
            int tmp;
            tmp = ax; ax = bx; bx = tmp;
            tmp = ay; ay = by; by = tmp;
        }
        HashSet set = new HashSet();
        for (int a = 1; ; ++a) {
            int b = (int)Math.floor(-a*ay/(double)by);
            int x1 = a*ax + b*bx, y1 = a*ay + b*by;
            if (x1 < 0) { x1 = -x1; y1 = -y1; }
            boolean s1 = (x1*x1 + y1*y1 <= maxLengthSq);
            if (s1) set.add(new Point(x1, y1));
            ++b;
            int x2 = a*ax + b*bx, y2 = a*ay + b*by;
            if (x2 < 0) { x2 = -x2; y2 = -y2; }
            boolean s2 = (x2*x2 + y2*y2 <= maxLengthSq);
            if (s2) set.add(new Point(x2, y2));
            if (!(s1 || s2)) break;
        }
        return set;
    }

    protected void init() throws NoninvertibleTransformException {
        super.init();

        int[] vec = new int[4];
        ornament.getVectors(vec);
        int ax0 = vec[0], ay0 = vec[1], bx0 = vec[2], by0 = vec[3];
        int det0 = ax0*by0 - ay0*bx0;
        int area = Math.abs(det0);
        int maxLengthSq = 12*12*area;
        Collection xPoints = getClosestPoints(ax0, ay0, bx0, by0, maxLengthSq);
        Collection yPoints = getClosestPoints(ay0, ax0, by0, bx0, maxLengthSq);
        // Points in yPoints have x and y swapped!
        int xx = 0, xy = 0, yx = 0, yy = 0;
        double minBadness = Double.POSITIVE_INFINITY;
        for (Iterator xi = xPoints.iterator(); xi.hasNext(); ) {
            Point xp = (Point)xi.next();
            for (Iterator yi = yPoints.iterator(); yi.hasNext(); ) {
                Point yp = (Point)yi.next();
                double cos2 = xp.x*yp.y + xp.y*yp.x;
                cos2 *= cos2;
                cos2 /= xp.x*xp.x + xp.y*xp.y;
                cos2 /= yp.x*yp.x + yp.y*yp.y;
                double badness = 0;
                badness += 1e3*cos2;
                badness += Math.abs(xp.y) + Math.abs(yp.y);
                badness += 1e-4*xp.x*yp.x;
                if (badness >= minBadness) continue;
                System.err.println("x=" + xp.x + "/" + xp.y +
                                   ", y=" + yp.y + "/" + yp.x +
                                   ", cos2=" + cos2 +
                                   ", xy=" + xp.y + ", yx=" + yp.y +
                                   ", area=" + xp.x*yp.x +
                                   ", badness=" + badness);
                minBadness = badness;
                xx = xp.x;
                xy = xp.y;
                yx = yp.y;
                yy = yp.x;
            }
        }
        // Now we have the x/y coordinates of two almost rectangular gridpoints
        int xa = (xx*by0 - bx0*xy)/det0;
        int xb = (ax0*xy - xx*ay0)/det0;
        int ya = (yx*by0 - bx0*yy)/det0;
        int yb = (ax0*yy - yx*ay0)/det0;
        // Now we have the a/b coordinates of these points
        double det1 = xa*yb - xb*ya;
        double ax1 = xx/det1*yb;
        double ay1 = -yy/det1*xb;
        double bx1 = -xx/det1*ya;
        double by1 = yy/det1*xa;
        System.err.println("Shifting from " +
                           ax0 + "/" + ay0 + ", " + bx0 + "/" + by0 + " to " +
                           ax1 + "/" + ay1 + ", " + bx1 + "/" + by1 +
                           " and area " + xx + "/" + yy);

        vectorTransform = new AffineTransform(ax1, ay1, bx1, by1, 0, 0);
        int imgType;
        if (hasAlpha) imgType = BufferedImage.TYPE_INT_ARGB;
        else imgType = BufferedImage.TYPE_INT_RGB;
        img = new BufferedImage(xx, yy, imgType);
        g2d = img.createGraphics();
        RenderingHints renderingHints = new RenderingHints(null);
        renderingHints.put(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        renderingHints.put(RenderingHints.KEY_DITHERING,
                           RenderingHints.VALUE_DITHER_DISABLE);
        renderingHints.put(RenderingHints.KEY_STROKE_CONTROL,
                           RenderingHints.VALUE_STROKE_NORMALIZE);
        renderingHints.put(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHints(renderingHints);
        bounds = new LoopBounds(vectorTransform);
        bounds.boundsFor(0, 0, img.getWidth(), img.getHeight());
    }

    protected void background(BufferedImage img) throws IOException {
        int na = bounds.getMaxA() - bounds.getMinA() + 1;
        int nb = bounds.getMaxB() - bounds.getMinB() + 1;
        BufferedImage multi = Ornament.tileImage(img, na, nb);
        AffineTransform tempTrans = new AffineTransform(vectorTransform);
        tempTrans.translate(bounds.getMinA(), bounds.getMinB());
        tempTrans.scale(1./img.getWidth(), 1./img.getHeight());
        g2d.drawImage(multi, tempTrans, null);
    }

    protected void tail() throws IOException {
        g2d.dispose();
        ImageIO.write(img, format, out);
    }

    protected void path(LinPath l) throws NoninvertibleTransformException {
        Group group = ornament.getGroup();
        GeneralPath tempPath = new GeneralPath();
        AffineTransform tempTrans = new AffineTransform();
        for (int i = 0; i < group.countTransforms(); ++i) {
            tempPath.reset();
            tempPath.append(l.getPathIterator(group.getTransform(i)), false);
            Rectangle2D bb=tempPath.getBounds2D();
            int minA = bounds.getMinA() - (int)Math.ceil(bb.getMaxX());
            int maxA = bounds.getMaxA() - (int)Math.floor(bb.getMinX());
            int minB = bounds.getMinB() - (int)Math.ceil(bb.getMaxY());
            int maxB = bounds.getMaxB() - (int)Math.floor(bb.getMinY());
            for (int da = minA; da <= maxA; ++da) {
                for (int db = minB; db <= maxB; ++db) {
                    // System.err.println("Drawing at " + da + "/" + db);
                    tempTrans.setTransform(vectorTransform);
                    tempTrans.translate(da, db);
                    g2d.draw(tempPath.createTransformedShape(tempTrans));
                }
            }
        }
    }

    protected void color(Color c) {
        g2d.setColor(c);
    }

    protected void stroke(BasicStroke s) {
        g2d.setStroke(s);
    }

}
