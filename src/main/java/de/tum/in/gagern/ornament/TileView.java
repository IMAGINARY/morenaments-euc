package de.tum.in.gagern.ornament;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * View displaying a single tile of the pattern.
 * The view allows the user to control the outline and orientation of the
 * tiles as well as the elementary cell grid where applicable.
 *
 * @author Martin von Gagern
 */
class TileView extends JPanel
    implements Constants, javax.swing.event.MouseInputListener {

    Ornament main;
    int ox, oy, ax, ay, bx, by;
    int mina, maxa, minb, maxb;
    int type, movingPoint;
    boolean gridMode, shadeMode;
    Group group;
    /* Length of one rectangle side while modifying the other */
    double otherLength;
    /* Direction of one rhomb side while modifying the other */
    double otherX, otherY;
    RenderingHints renderingHints;
    Polygon clip;
    Shape background;
    Point[] voronoiPoints;
    AffineTransform trans;

    static final double SQRT3HALF=Math.sqrt(3)/2;
    static final int PREFDIM=50;
    static final Color SHADE_COLOR=new Color(.5f, .5f, .5f, .85f);

    TileView(Ornament ornament) {
        main=ornament;
        ax=main.ax; ay=main.ay;
        bx=main.bx; by=main.by;
        recalcClipping();
        recalcTiling();
        ox=(PREFDIM-ax-bx)/2; oy=(PREFDIM-ay-by)/2;
        trans=new AffineTransform(ax, ay, bx, by, ox, oy);
        voronoiPoints=new Point[12];
        for (int i=0; i<voronoiPoints.length; ++i)
            voronoiPoints[i]=new Point();
        setPreferredSize(new Dimension(PREFDIM, PREFDIM));
        // setBackground(Color.gray);
        // setOpaque(true);
        setForeground(Color.black);
        addMouseListener(this);
        addMouseMotionListener(this);
        renderingHints=new RenderingHints(null);
        renderingHints.add(main.renderingHints);
                renderingHints.put(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    centerTile();
                    recalcVoronoi();
                    recalcTiling();
                    recalcClipping();
                    repaint();
                }
            });

        /* Children */
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc2=new GridBagConstraints();
        gbc2.weightx=1;
        gbc2.weighty=1;
        gbc2.fill=GridBagConstraints.BOTH;
        gbc2.gridwidth=GridBagConstraints.REMAINDER;
        gbc2.weighty=0;
        gbc2.insets=new Insets(2, 0, 2, 2);
        JLabel lbl=new JLabel("morenaments", JLabel.CENTER);
        lbl.setFont(new Font("Monospaced", 0, 20));
        lbl.setForeground(Color.darkGray.darker());
        add(lbl, gbc2);
        
        GridBagConstraints gbc=new GridBagConstraints();
        gbc.weightx=1;
        gbc.weighty=1;
        gbc.fill=GridBagConstraints.BOTH;
        gbc.gridwidth=GridBagConstraints.REMAINDER;
        add(Box.createGlue(), gbc);

    }

    void paintPoint(int x, int y, Graphics g, Color c) {
        g.setColor(c);
        g.fillOval(x-3, y-3, 6, 6);
        g.setColor(getForeground());
        g.drawOval(x-3, y-3, 6, 6);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // for (int i=0; i<info.length; ++i) info[i].paint(g);
        Graphics2D g2d=null;
        if (g instanceof Graphics2D) {
            g2d=(Graphics2D) g;
            g2d.setRenderingHints(renderingHints);
        }
        if (movingPoint==0 && g2d!=null) {
            Image buf=main.getBuffer();
            for (int da=mina; da<=maxa+1; ++da)
                for (int db=minb; db<=maxb+1; ++db) {
                    int x=ox+da*ax+db*bx, y=oy+da*ay+db*by;
                    if (shadeMode ?
                        g.hitClip(x, y, main.width, main.height) :
                        clip.intersects(x, y, main.width, main.height))
                        g2d.drawImage(buf, x, y, this);
                }
            g2d.setColor(shadeMode ? SHADE_COLOR : getBackground());
            g2d.fill(background);
            g2d.setColor(getForeground());
        }
        else if (movingPoint<=3) {
            g.drawLine(ox, oy, ox+ax, oy+ay);
            g.drawLine(ox, oy, ox+bx, oy+by);
            g.drawLine(ox+ax, oy+ay, ox+ax+bx, oy+ay+by);
            g.drawLine(ox+bx, oy+by, ox+ax+bx, oy+ay+by);
        }
        if (gridMode && movingPoint>3 && g2d!=null) {
            g2d.draw(main.voronoi.getCompactGrid().createTransformedShape(trans));
        }
        paintPoint(ox, oy, g, Color.blue);
        paintPoint(ox+ax, oy+ay, g, Color.red);
        paintPoint(ox+bx, oy+by, g, Color.green);
        if (gridMode && !group.fixedGrid()) {
            debug("Voronoi control point: " + voronoiPoints[0]);
            if (movingPoint==0) {
                paintPoint(voronoiPoints[0].x, voronoiPoints[0].y,
                           g, Color.yellow);
            }
            if (movingPoint==4) {
                for (int i=0; i<group.countTransforms(); ++i)
                    paintPoint(voronoiPoints[i].x, voronoiPoints[i].y,
                               g, Color.yellow);
            }
        }
    }

    private void debug(String msg) {
        // System.err.println(msg);
    }

    public void setGroup(Group grp) {
        group=grp;
        setType(grp.getTileType());
        recalcVoronoi();
        repaint();
    }

    public void setGridMode(boolean gm) {
        if (gridMode==gm) return;
        gridMode=gm;
        repaint();
    }

    protected boolean setType(int type) {
        if (this.type==type) return false;
        double ra=Math.sqrt(ax*ax+ay*ay);
        double rb=Math.sqrt(bx*bx+by*by);
        double rm=(ra+rb)/2;
        double ta=Math.atan2(ay, ax);
        double tb=Math.atan2(by, bx);
        double tm=Math.atan2(ay+by, ax+bx);
        recalc: do {
            switch (type) {
            case TILETYPE_TRIANGLE:
                ta=tm+Math.PI/6;
                tb=tm-Math.PI/6;
                ra=rb=rm;
                break;
            case TILETYPE_SQUARE:
                ra=rb=rm;
                // fall-through
            case TILETYPE_RECTANGLE:
                ta=tm+Math.PI/4;
                tb=tm-Math.PI/4;
                break;
            case TILETYPE_RHOMB:
                ra=rb=rm;
                break;
            }
            ax=(int)Math.round(Math.cos(ta)*ra);
            ay=(int)Math.round(Math.sin(ta)*ra);
            bx=(int)Math.round(Math.cos(tb)*rb);
            by=(int)Math.round(Math.sin(tb)*rb);
            if (Math.abs(ax*by-bx*ay) < 10) {
                ax=10;
                ay=bx=0;
                by=-10;
            }
        } while(false);
        this.type=type;
        setVectors();
        return true;
    }

    public void setVectors(int ax, int ay, int bx, int by) {
        if (this.ax == ax && this.ay == ay && this.bx == bx && this.by == by)
            return;
        this.ax = ax;
        this.bx = bx;
        this.ay = ay;
        this.by = by;
        centerTile();
        setVectors();
    }

    void setVectors() {
        if (movingPoint!=1) main.setVectors(ax, ay, bx, by);
        trans.setTransform(ax, ay, bx, by, ox, oy);
        recalcVoronoi();
        recalcClipping();
        recalcTiling();
    }

    void centerTile() {
        Dimension s=getSize();
        ox=(s.width-ax-bx)/2;
        if (ox>=s.width) ox=s.width-1;
        if (ox<0) ox=0;
        oy=(s.height-ay-by)/2;
        if (oy>=s.height) oy=s.height-1;
        if (oy<0) oy=0;
    }

    private static final double VORONOI_EPS = 1e-3;

    void recalcVoronoi() {
        Point2D control = main.getVoronoiPoint();
        Point2D.Double tmp = new Point2D.Double();
        for (int i = 0; i < group.countTransforms(); ++i) {
            group.getTransform(i).transform(control, tmp);
            if (tmp.x < -VORONOI_EPS || tmp.x > 1. + VORONOI_EPS)
                tmp.x -= Math.floor(tmp.x);
            if (tmp.y < -VORONOI_EPS || tmp.y > 1. + VORONOI_EPS)
                tmp.y -= Math.floor(tmp.y);
            trans.transform(tmp, voronoiPoints[i]);
        }
    }

    void recalcClipping() {
        clip=new Polygon(new int[] { ox, ox+ax, ox+ax+bx, ox+bx },
                         new int[] { oy+1, oy+ay+1, oy+ay+by+1, oy+by+1 },
                         4);
        Area bg=new Area(new Rectangle(getSize()));
        bg.subtract(new Area(clip));
        background=bg;
    }

    void recalcTiling() {
        mina=maxa=minb=maxb=0;
        int minx, maxx, miny, maxy;
        if (shadeMode) {
            Dimension size=getSize();
            minx=-ox;
            miny=-oy;
            maxx=size.width-ox;
            maxy=size.height-oy;
        } else {
            minx=Math.min(Math.min(ax+bx, 0), Math.min(ax, bx));
            maxx=Math.max(Math.max(ax+bx, 0), Math.max(ax, bx));
            miny=Math.min(Math.min(ay+by, 0), Math.min(ay, by));
            maxy=Math.max(Math.max(ay+by, 0), Math.max(ay, by));
        }
        expandTilingToPoint(minx, miny);
        expandTilingToPoint(minx, maxy);
        expandTilingToPoint(maxx, miny);
        expandTilingToPoint(maxx, maxy);
    }

    void expandTilingToPoint(int x, int y) {
        double a, b;
        a=main.aFromPoint(x, y);
        b=main.bFromPoint(x, y);
        if (a<mina) mina=(int)Math.floor(a);
        if (a>maxa) maxa=(int)Math.ceil(a);
        if (b<minb) minb=(int)Math.floor(b);
        if (b>maxb) maxb=(int)Math.ceil(b);
    }

    public void mouseClicked(MouseEvent evnt) { }
    public void mousePressed(MouseEvent evnt) {
        if (movingPoint!=0) return;
        int max=100, x=evnt.getX(), y=evnt.getY(), dx, dy;
        dx=ox-x;
        dy=oy-y;
        if (dx*dx+dy*dy<=max) {
            movingPoint=1;
            max=dx*dx+dy*dy;
        }
        dx=ox+ax-x;
        dy=oy+ay-y;
        if (dx*dx+dy*dy<=max) {
            movingPoint=2;
            max=dx*dx+dy*dy;
            otherX=bx;
            otherY=by;
            otherLength=Math.sqrt(bx*bx+by*by);
        }
        dx=ox+bx-x;
        dy=oy+by-y;
        if (dx*dx+dy*dy<=max) {
            movingPoint=3;
            max=dx*dx+dy*dy;
            otherX=ax;
            otherY=ay;
            otherLength=Math.sqrt(ax*ax+ay*ay);
        }
        dx=voronoiPoints[0].x-x;
        dy=voronoiPoints[0].y-y;
        if (gridMode && !group.fixedGrid() && dx*dx+dy*dy<=max) {
            movingPoint=4;
            max=dx*dx+dy*dy;
        }
    }

    public void mouseReleased(MouseEvent evnt) {
        if (movingPoint==0) return;
        if(movingPoint>3) {
            main.repaintBuffer();
        } else {
            Dimension s=getSize();
            if (ax*by-bx*ay == 0 ||
                ax>=s.width || bx>=s.width ||
                ay>=s.height || by>=s.height) {
                ax=main.ax;
                ay=main.ay;
                bx=main.bx;
                by=main.by;
            } else {
                if ((movingPoint==1 && ( ox<0 || ox>=s.width)) ||
                    (movingPoint==2 && ( ox+ax<0 || ox+ax>=s.width)) ||
                    (movingPoint==3 && ( ox+bx<0 || ox+bx>=s.width))) {
                    ox=(s.width-ax-bx)/2;
                    if (ox>=s.width) ox=s.width-1;
                    if (ox<0) ox=0;
                }
                if ((movingPoint==1 && ( oy<0 || oy>=s.height)) ||
                    (movingPoint==2 && ( oy+ay<0 || oy+ay>=s.height)) ||
                    (movingPoint==3 && ( oy+by<0 || oy+by>=s.height))) {
                    oy=(s.height-ay-by)/2;
                    if (oy>=s.height) oy=s.height-1;
                    if (oy<0) oy=0;
                }
                setVectors();
            }
        }
        movingPoint=0;
        repaint();
    }
    public void mouseEntered(MouseEvent evnt) { }
    public void mouseExited(MouseEvent evnt) { }
    public void mouseDragged(MouseEvent evnt) {
        int x=evnt.getX(), y=evnt.getY();
        if (x<0 || x>getWidth() || y<0 || y>getHeight()) return;
        switch(movingPoint) {
        case 0:
            return;
        case 1:
            ox=x;
            oy=y;
            break;
        case 2:
            ax=x-ox;
            ay=y-oy;
            if (Math.abs(ay)<=Math.abs(ax)/25) ay=0;
            else if (Math.abs(ax)<=Math.abs(ay)/25) ax=0;
            else if (Math.abs(ax-ay)<=Math.abs(ax)/25) ax=ay=(ax+ay)/2;
            else if (Math.abs(ax+ay)<=Math.abs(ay)/25) ay=-(ax=(ax-ay)/2);
            switch (type) {
            case TILETYPE_RECTANGLE:
                double factor=otherLength/Math.sqrt(ax*ax+ay*ay);
                if (factor>=0.97 && factor<=1.03) {
                    ax=-by;
                    ay=bx;
                } else {
                    bx=(int)Math.round(ay*factor);
                    by=(int)Math.round(-ax*factor);
                }
                break;
            case TILETYPE_SQUARE:
                bx=ay;
                by=-ax;
                break;
            case TILETYPE_TRIANGLE:
                bx=(int)Math.round(0.5*ax+SQRT3HALF*ay);
                by=(int)Math.round(-SQRT3HALF*ax+0.5*ay);
                break;
            case TILETYPE_RHOMB:
                factor=Math.sqrt(ax*ax+ay*ay)/otherLength;
                bx=(int)Math.round(otherX*factor);
                by=(int)Math.round(otherY*factor);
                break;
            }
            break;
        case 3:
            bx=x-ox;
            by=y-oy;
            if (Math.abs(by)<=Math.abs(bx)/25) by=0;
            else if (Math.abs(bx)<=Math.abs(by)/25) bx=0;
            else if (Math.abs(bx-by)<=Math.abs(bx)/25) bx=by=(bx+by)/2;
            else if (Math.abs(bx+by)<=Math.abs(by)/25) by=-(bx=(bx-by)/2);
            switch (type) {
            case TILETYPE_RECTANGLE:
                double factor=otherLength/Math.sqrt(bx*bx+by*by);
                if (factor>=0.97 && factor<=1.03) {
                    bx=ay;
                    by=-ax;
                } else {
                    ax=(int)Math.round(-by*factor);
                    ay=(int)Math.round(bx*factor);
                }
                break;
            case TILETYPE_SQUARE:
                ax=-by;
                ay=bx;
                break;
            case TILETYPE_TRIANGLE:
                ax=(int)Math.round(0.5*bx-SQRT3HALF*by);
                ay=(int)Math.round(SQRT3HALF*bx+0.5*by);
                break;
            case TILETYPE_RHOMB:
                factor=Math.sqrt(bx*bx+by*by)/otherLength;
                ax=(int)Math.round(otherX*factor);
                ay=(int)Math.round(otherY*factor);
                break;
            }
            break;
        default:
            try {
                voronoiPoints[0].setLocation(x, y);
                Point2D snappedPoint;
                snappedPoint = group.snapVoronoi(voronoiPoints[0], trans);
                trans.inverseTransform(snappedPoint, main.getVoronoiPoint());
                main.recalcVoronoi();
            }
            catch (NoninvertibleTransformException e) {
                e.printStackTrace();
            }
            recalcVoronoi();
        }
        repaint();
    }
    public void mouseMoved(MouseEvent evnt) { }

}
