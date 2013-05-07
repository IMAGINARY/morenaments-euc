package de.tum.in.gagern.ornament;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import de.tum.in.gagern.geom.Vec3R;
import de.tum.in.gagern.ornament.groups.*;

/**
 * Abstract Group representing a crystallographic group.
 *
 * @author Martin von Gagern
 */
public abstract class Group {

    protected static final double ALMOST_ZERO=1e-7;

    /**
     * Creates an array containing instances of all 17 crystallographic groups.
     * The indices into this array are named in {@link Constants}.
     */
    public static Group[] getGroups() {
        return new Group[] {new P1(), new P2(), new Pm(), new Pg(), new Cm(),
                                new Pmm(), new Pmg(), new Pgg(), new Cmm(),
                                new P4(), new P4mm(), new P4gm(), new P3(),
                                new P3m1(), new P31m(), new P6(), new P6mm()};
    }

    private String[] names;

    /**
     * The transformations creating all images in this group.
     * Generated from the first parameter of the
     * {@linkplain #Group constructor}.
     */
    protected AffineTransform[] trans;

    /**
     * Array of geometric properties of this group.
     * Reference to the second parameter of the
     * {@linkplain #Group constructor}.
     */
    protected Property[] props;

    protected Object[] snapVoronoi;

    /**
     * Common constructor for Groups.
     * {@link AffineTransform}s are generated from <code>trans</code>,
     * and a reference to the properties is saved.
     * The elements of <code>trans</code> must be arrays of six numbers
     * describing transformations as folloows:
     * <pre>
     * [ a' ]   [t1, t3, t5]   [ a ]
     * [ b' ] = [t2, t4, t6] * [ b ]
     *                         [ 1 ]
     * </pre>
     * @param names the name of the group in some naming schemes
     * @param trans an array of arrays of six doubles
     * @param props an array of the geometric properties of this group
     * @see #trans
     * @see #props
     */
    protected Group(String[] names, double[][] trans,
                    Property[] props, Object[] snapVoronoi) {
        if (names.length != 2)
            throw new IllegalArgumentException("Expect two names");
        this.names = names;
        this.trans=new AffineTransform[trans.length];
        for (int i=0; i<trans.length; ++i)
            this.trans[i]=new AffineTransform(trans[i]);
        this.props=props;
        this.snapVoronoi = snapVoronoi;
    }

    /**
     * The name describing this group.
     */
    public final String getName() {
        return getName(Constants.NAMING_CRYST);
    }

    /**
     * The name describing this group, in a given naming system.
     * @param namingSystem the system used for naming.
     * @see Constants#NAMING_CRYST
     * @see Constants#NAMING_ORBIFOLD_ASCII
     * @see Constants#NAMING_ORBIFOLD_UNICODE
     */
    public String getName(int namingSystem) {
        switch (namingSystem) {
        case Constants.NAMING_CRYST:
            return names[0];
        case Constants.NAMING_ORBIFOLD_ASCII:
            return names[1];
        case Constants.NAMING_ORBIFOLD_UNICODE:
            return names[1]
                .replace('x', '\u00d7')
                .replace('o', '\u2218')
                .replace('*', '\u2217');
        default:
            throw new IllegalArgumentException("Unknown naming scheme");
        }
    }

    /**
     * The kind of tile to be used for this group. One of:
     * <ul>
     *  <li>{@link Constants#TILETYPE_PARALLEL TILETYPE_PARALLEL}</li>
     *  <li>{@link Constants#TILETYPE_RECTANGLE TILETYPE_RECTANGLE}</li>
     *  <li>{@link Constants#TILETYPE_SQUARE TILETYPE_SQUARE}</li>
     *  <li>{@link Constants#TILETYPE_TRIANGLE TILETYPE_TRIANGLE}</li>
     *  <li>{@link Constants#TILETYPE_RHOMB TILETYPE_RHOMB}</li>
     * </ul>
     */
    public abstract int getTileType();

    /**
     * The number of transformed images in this group.
     */
    public int countTransforms() {
        return trans.length;
    }

    /**
     * The Transformation for the indicated image.
     * The index 0 usually describes the identity.
     * @param i the index of the requested transform
     * @throws ArrayIndexOutOfBoundsException if i is not the index of a
     *                                        transformation of this group
     * @see #countTransforms
     */
    public AffineTransform getTransform(int i) {
        return trans[i];
    }

    /**
     * The geometric properties of this group.
     */
    public Property[] getProperties() {
        return props;
    }

    public Point2D sensibleVoronoiPoint(Point2D request,
                                           AffineTransform vectorTransform)
        throws NoninvertibleTransformException
    {
        return request;
    }

    public final boolean fixedGrid() {
        return snapVoronoi == null;
    }

    protected Point2D snap(Point2D p, AffineTransform t, Object[] targets) {
        Point2D p1 = new Point2D.Double(), p2 = new Point2D.Double();
        Line2D l1 = new Line2D.Double(), l2;
        double minDist = 80.;
        Point2D bestPos = null;
        for (int i = 0; i < targets.length; ++i) {
            if (targets[i] instanceof Point2D) {
                t.transform((Point2D)targets[i], p1);
                double dist = p1.distanceSq(p);
                if (dist < minDist) {
                    minDist = dist;
                    bestPos = new Point2D.Double(p1.getX(), p1.getY());
                }
            }
            else if (targets[i] instanceof Line2D) {
                l2 = (Line2D)targets[i];
                t.transform(l2.getP1(), p1);
                t.transform(l2.getP2(), p2);
                l1.setLine(p1, p2);
                double dist = l1.ptLineDistSq(p) * 4.;
                if (dist < minDist) {
                    minDist = dist;
                    Vec3R v1 = new Vec3R(p1.getX(), p1.getY(), 1);
                    Vec3R v2 = new Vec3R(p2.getX(), p2.getY(), 1);
                    Vec3R vline = v1.cross(v2).normalize();
                    Vec3R vperp = new Vec3R(vline.x, vline.y, 0);
                    Vec3R vp = new Vec3R(p.getX(), p.getY(), 1);
                    Vec3R vb = vp.cross(vperp).cross(vline).normalize();
                    bestPos = vb.dehomog();
                }
            }
            else {
                throw new IllegalArgumentException("Unknown snap target");
            }
        }
        if (bestPos != null) p.setLocation(bestPos);
        else bestPos = p;
        return bestPos;
    }

    public Point2D snapVoronoi(Point p, AffineTransform t) {
        if (snapVoronoi == null) return p;
        else return snap(p, t, snapVoronoi);
    }

    public String toString() {
        return getClass().getName();
    }

    public int hashCode() {
        return getClass().hashCode();
    }

    public boolean equals(Object o) {
        return o != null && getClass().equals(o.getClass());
    }

}
