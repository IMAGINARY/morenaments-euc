package de.tum.in.gagern.ornament;

import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import de.tum.in.gagern.ornament.plaf.MorenamentsTheme;

/**
 * Main class of this program.
 * This class also contains the current document (collection of all lines
 * drawn) as well as a offscreen buffer used to display the pattern quickly.
 *
 * @author Martin von Gagern
 */
public class Ornament extends JApplet implements Constants {

    static final int GRID_NONE=0;
    static final int GRID_TILE=1;
    static final int GRID_CELL=2;
    static final int GRID_PROPS=4;
    static final int GRID_BUFFER=8;

    private Group group;
    int ax, ay, bx, by, width, height;
    private int mina, maxa, minb, maxb;
    private double dax, day, dbx, dby, denom;
    Window window;
    private JMenuBar menuBar;
    TileView tv;
    private PlaneView pv;
    private Controls cntrl;
    private JSplitPane splitPane;
    private Image buf;
    private boolean showBackground;
    private BufferedImage backgroundTile;
    private BufferedImage backgroundTiles;
    List lines;
    Voronoi voronoi;
    private AffineTransform vectorTransform, tempTrans;
    private GeneralPath tempPath;
    private Line2D tempLine;
    Color[] colors={
        /* Pen: */ Color.white, /* Background: */ Color.black,
        /* Grids: */
        Color.gray, Color.darkGray, Color.darkGray,
        new Color(0x99, 0x99, 0x99),
        new Color(0xcc, 0x33, 0x33),
        new Color(0x66, 0x66, 0xcc),
        new Color(0x00, 0xcc, 0x66),
        new Color(0xcc, 0xcc, 0x00)};
    private BasicStroke lineStroke, gridStroke=new BasicStroke(),
        glideStroke=new BasicStroke(1.f, BasicStroke.CAP_BUTT,
                                    BasicStroke.JOIN_BEVEL,
                                    0.f, new float[]{3, 3}, 0.f);
    private int grid;
    RenderingHints renderingHints;
    boolean useBufferedImage=false, useStrokes=true,
        useAntiAlias=true, gridOnTop=false;
    private boolean debug=false;
    KioskMode kioskMode;
    private List hooks;
	private JLabel statusBar = new JLabel("");

    public static void main(String[] args) {
        System.setProperty("java.awt.Window.locationByPlatform", "true");
        MorenamentsTheme.activate();
        boolean debug = false, kiosk = true, showurl = true;
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if ("-debug".equals(arg))
                debug = true;
            if ("-kiosk".equals(arg))
                kiosk = true;
            if ("-nourl".equals(arg))
                showurl = false;
        }
        if (kiosk) {
            KioskMode km = new KioskMode(null, null);
            km.start();
            return;
        }
        JFrame frm=new JFrame(I18n._("morenaments euc"));
        Ornament ornament = new Ornament(frm, debug);
        frm.setContentPane(ornament);
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frm.setSize(1024, 768);
        frm.setVisible(true);
    }

    public Ornament() {
        this(null, false);
    }

    public Ornament(Window wnd, boolean debug) {
        this(wnd, debug, null);
    }

    public Ornament(Window wnd, boolean debug, KioskMode kiosk) {
        kioskMode = kiosk;
        hooks = new ArrayList();
        renderingHints=new RenderingHints(null);
        renderingHints.put(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        renderingHints.put(RenderingHints.KEY_DITHERING,
                           RenderingHints.VALUE_DITHER_DISABLE);
        renderingHints.put(RenderingHints.KEY_STROKE_CONTROL,
                           RenderingHints.VALUE_STROKE_NORMALIZE);
        renderingHints.put(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        lines=new ArrayList();
        voronoi=new Voronoi();
        vectorTransform=new AffineTransform();
        tempTrans=new AffineTransform();
        tempPath=new GeneralPath();
        tempLine=new Line2D.Float();
        setVectors(150, 0, 0, -150);

        if (wnd != null) {
            setStub(new AppStub());
            window=wnd;
        }
        this.debug=debug;
        tv=new TileView(this);
        pv=new PlaneView(this);
        cntrl=new Controls(this);
        
    	statusBar.setFont(new Font("SansSerif", 0, 14));
    	statusBar.setBorder(BorderFactory.createEmptyBorder(0,6,7,0));
    	
        //menuBar = cntrl.createMenu();
        GridLayout groupLayout = new GridLayout(0, 2);
        JPanel groupButtons = cntrl.createGroupButtons();
        JPanel colorControls = cntrl.createColorControls();
        JPanel miscControls = cntrl.createMiscControls();
        JPanel sidePane = new JPanel();
        JPanel colorsAndMisc = new JPanel();
        JPanel colorsMiscAndTile = new JPanel();
        sidePane.setLayout(new BorderLayout());
        colorsAndMisc.setLayout(new BorderLayout());
        colorsMiscAndTile.setLayout(new BorderLayout());
        //sidePane.add(menuBar, BorderLayout.NORTH);
        //sidePane.add(groupButtons, BorderLayout.WEST);
        sidePane.add(colorsMiscAndTile, BorderLayout.CENTER);
        colorsMiscAndTile.add(colorsAndMisc, BorderLayout.SOUTH);
        colorsMiscAndTile.add(tv, BorderLayout.CENTER);
        colorControls.setBorder(BorderFactory.createEmptyBorder(0,3,3,3));
        colorsAndMisc.add(colorControls, BorderLayout.CENTER);
        colorsAndMisc.add(miscControls, BorderLayout.SOUTH);
        colorsAndMisc.add(statusBar, BorderLayout.NORTH);

        JPanel planeAndGroupPanel = new JPanel(new BorderLayout());
        JPanel buttonPan = new JPanel(new GridBagLayout());
        buttonPan.setBackground(Color.black);
        buttonPan.add(groupButtons);
        planeAndGroupPanel.add(buttonPan, BorderLayout.WEST);
        
        
        planeAndGroupPanel.add(pv, BorderLayout.CENTER);
        
        //sidePane.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        planeAndGroupPanel.add(sidePane, BorderLayout.EAST);
        
        //splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false,
        //                         planeAndGroupPanel, sidePane);
        //splitPane.setOneTouchExpandable(true);
        //splitPane.setResizeWeight(1.); // resize affects only drawing area
        
        
        setContentPane(planeAndGroupPanel);
    }

    private static class AppStub implements AppletStub {
        public boolean isActive() { return true; }
        public URL getDocumentBase() { return null; }
        public URL getCodeBase() { return null; }
        public AppletContext getAppletContext() { return null; }
        public void appletResize(int width, int height) { }
        public String getParameter(String name) {
            return System.getProperty(name);
        }
    }

    public void addHooks(Hooks hooks) {
        this.hooks.add(hooks);
    }

    public void removeHooks(Hooks hooks) {
        this.hooks.remove(hooks);
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    public void init() {
        super.init();
        JSplitPane splitPane=(JSplitPane)getContentPane();
        splitPane.setBorder(BorderFactory.createLoweredBevelBorder());
        adjustSplitter();
    }

    void adjustSplitter() {
        JSplitPane splitPane=(JSplitPane)getContentPane();
        int all=getWidth();
        int right=splitPane.getRightComponent().getPreferredSize().width;
        right+=splitPane.getDividerSize();
        right+=7-(right%5);
        splitPane.setDividerLocation(all-right);
        splitPane.getRightComponent().invalidate();
        validate();
    }

    void setAntiAlias(boolean aa) {
        renderingHints.put(RenderingHints.KEY_ANTIALIASING,
                           aa ?
                           RenderingHints.VALUE_ANTIALIAS_ON :
                           RenderingHints.VALUE_ANTIALIAS_OFF);
        useAntiAlias=aa;
        repaintBuffer();
    }

    public void setColor(Color clr) {
        if (clr == null) throw new NullPointerException();
        if (colors[0].equals(clr)) return;
        colors[0] = clr;
        cntrl.setColor(clr);
    }

    public void setBackgroundColor(Color clr) {
        if (clr == null) throw new NullPointerException();
        if (colors[1].equals(clr)) return;
        colors[1] = clr;
        cntrl.setBackgroundColor(clr);
    }

    void setColors(Color[] clrs) {
        if (clrs.length != colors.length)
            throw new ArrayIndexOutOfBoundsException();
        setColor(clrs[0]);
        setBackgroundColor(clrs[1]);
        for (int i = 2; i < colors.length; ++i) {
            if (clrs[i] == null) throw new NullPointerException();
            colors[i] = clrs[i];
        }
        repaintBuffer();
    }

    void setStroke(BasicStroke s) {
        lineStroke=s;
    }

    void setGroup(Group grp) {
        group=grp;
        backgroundTile=null;
        hasBackground(false);
        tv.setGroup(grp);
        voronoi.set(group, ax, ay, bx, by);
        repaintBuffer();
        statusBar.setText(I18n._("Group")+": "+grp.getName());
    }

    public void setVectors(int ax, int ay, int bx, int by) {
        if (this.ax == ax && this.ay == ay && this.bx == bx && this.by == by)
            return;
        for (Iterator i = hooks.iterator(); i.hasNext(); )
            ((Hooks)i.next()).setVectors(ax, ay, bx, by);
        dax=this.ax=ax;
        day=this.ay=ay;
        dbx=this.bx=bx;
        dby=this.by=by;
        if (tv != null) tv.setVectors(ax, ay, bx, by);
        voronoi.set(group, ax, ay, bx, by);
        vectorTransform.setTransform(ax, ay, bx, by, 0, 0);
        denom=dax*dby-dbx*day;
        int minx= bx<ax&&bx<0 ? bx : ax<0 ? ax : 0;
        int maxx= bx>ax&&bx>0 ? bx : ax>0 ? ax : 0;
        int miny= by<ay&&by<0 ? by : ay<0 ? ay : 0;
        int maxy= by>ay&&by>0 ? by : ay>0 ? ay : 0;
        width=maxx-minx;
        height=maxy-miny;
        mina=maxa=minb=maxb=0;
        expandTilingToPoint(width, 0);
        expandTilingToPoint(width, height);
        expandTilingToPoint(0, height);
        repaintBuffer();
        if (pv!=null) pv.recalcTiling();
    }

    public Image getBuffer() {
        if (buf!=null) return buf;
        Graphics2D g2d=null;
        if (!useBufferedImage) {
            buf=createImage(width, height);
            Graphics g=buf.getGraphics();
            if (g instanceof Graphics2D) g2d=(Graphics2D)g;
            else {
                System.out.println("fallback to BufferedImage");
                useBufferedImage=true;
            }
        }
        if (useBufferedImage) {
            buf=new BufferedImage(width, height,
                                  BufferedImage.TYPE_4BYTE_ABGR);
            g2d=((BufferedImage)buf).createGraphics();
        }
        paintBuffer(g2d);
        return buf;
    }

    void addLine(float[] coords) {
        lines.add(new LinPath(coords, colors[0], lineStroke));
        if (gridOnTop && grid!=GRID_NONE && buf!=null) {
            Graphics2D g2d=getBufferGraphics();
            paintGrid(g2d);
            g2d.dispose();
        }
        if (tv!=null) tv.repaint();
        if (pv!=null) pv.repaint();
    }

    public List getLines() {
        return lines;
    }

    public Group getGroup() {
        return group;
    }

    public void getVectors(int[] vecs) {
        if (vecs.length < 4) throw new IllegalArgumentException();
        vecs[0] = ax;
        vecs[1] = ay;
        vecs[2] = bx;
        vecs[3] = by;
    }

    public AffineTransform getVectorTransform() {
        return vectorTransform;
    }

    public Color getBackgroundColor() {
        return colors[1];
    }

    void clearLines() {
        lines.clear();
        repaintBuffer();
    }

    void setGrid(int mode, boolean state) {
        int g = (state ? grid|mode : grid&~mode);
        if (g==grid) return;
        grid=g;
        if (mode==GRID_CELL) tv.setGridMode(state);
        repaintBuffer();
    }

    Point2D getVoronoiPoint() {
        return voronoi.getControlPoint();
    }

    void recalcVoronoi() {
        voronoi.recreate();
    }

    Graphics2D getBufferGraphics() {
        if (buf==null)
            throw new IllegalStateException("Buffer not ready");
        if (!useBufferedImage)
            return (Graphics2D) buf.getGraphics();
        else
            return ((BufferedImage)buf).createGraphics();
    }

    void repaintBuffer() {
        buf=null;
        if (tv!=null) tv.repaint();
        if (pv!=null) pv.repaint();
    }

    void paintBuffer(Graphics2D g2d) {
        g2d.setRenderingHints(renderingHints);
        g2d.setColor(colors[1]);
        g2d.fillRect(0, 0, width, height);
        if (showBackground) {
            int nw = maxa - mina + 1, nh = maxb - minb + 1;
            int bgw = backgroundTile.getWidth();
            int bgh = backgroundTile.getHeight();
            if (backgroundTiles == null ||
                backgroundTiles.getWidth() < nw*bgw ||
                backgroundTiles.getHeight() < nh*bgh) {
                backgroundTiles = tileImage(backgroundTile, nw, nh);
            }
            tempTrans.setTransform(vectorTransform);
            tempTrans.translate(mina, minb);
            tempTrans.scale(1./bgw, 1./bgh);
            g2d.drawImage(backgroundTiles, tempTrans, this);
        }
        if (!gridOnTop) paintGrid(g2d);
        Iterator i=lines.iterator();
        while (i.hasNext())
            paintLinPath(g2d, (LinPath)i.next());
        if (gridOnTop) paintGrid(g2d);
    }

    void paintGrid(Graphics2D g2d) {
        g2d.setStroke(gridStroke);
        if ((grid & GRID_BUFFER)>0) {
            g2d.setColor(colors[COLOR_BUFFER]);
            g2d.drawRect(0, 0, width-1, height-1);
        }
        if ((grid & GRID_CELL)>0) {
            g2d.setColor(colors[COLOR_CELL]);
            drawAll(g2d, voronoi.getGrid());
        }
        if ((grid & GRID_TILE)>0) {
            g2d.setColor(colors[COLOR_TILE]);
            drawGridLine(g2d, 0f, 0f, 1f, 0f);
            drawGridLine(g2d, 0f, 0f, 0f, 1f);
        }
        if ((grid & GRID_PROPS)>0) {
            paintProps(g2d);
        }
    }

    void paintProps(Graphics2D g) {
        Property[] props=group.getProperties();
        AffineTransform t=new AffineTransform();
        for (int i=props.length-1; i>=0; --i) {
            g.setColor(colors[COLOR_PROPERTIES+props[i].getTC()]);
            for (int da=mina; da<=maxa+1; ++da) {
                for (int db=minb; db<=maxb+1; ++db) {
                    t.setTransform(dax, day, dbx, dby,
                                   da*dax+db*dbx, da*day+db*dby);
                    props[i].paint(g, t);
                }
            }
        }
    }

    void paintLinPath(Graphics2D g2d, LinPath l) {
        g2d.setColor(l.getColor());
        if (useStrokes) g2d.setStroke(l.getStroke());
        for (int i=0; i<group.countTransforms(); ++i)
            drawOnce(g2d, l.getPathIterator(group.getTransform(i)));
    }

    void drawAll(Graphics2D g2d, Shape s) {
        if(group==null) {
            System.err.println("NULL!");
            System.exit(1);
        }
        for (int i=0; i<group.countTransforms(); ++i)
            drawOnce(g2d, s.getPathIterator(group.getTransform(i)));
    }

    void drawOnce(Graphics2D g2d, Shape s) {
        tempPath.reset();
        tempPath.append(s, false);
        drawPathOnce(g2d);
    }

    void drawOnce(Graphics2D g2d, PathIterator s) {
        tempPath.reset();
        tempPath.append(s, false);
        drawPathOnce(g2d);
    }

    private void drawPathOnce(Graphics2D g2d) {
        Rectangle2D bb=tempPath.getBounds2D();
        int min_a=mina-(int)Math.ceil(bb.getMaxX());
        int max_a=maxa-(int)Math.floor(bb.getMinX());
        int min_b=minb-(int)Math.ceil(bb.getMaxY());
        int max_b=maxb-(int)Math.floor(bb.getMinY());
        for (int da=min_a; da<=max_a; ++da) {
            for (int db=min_b; db<=max_b; ++db) {
                tempTrans.setTransform(vectorTransform);
                tempTrans.translate(da, db);
                g2d.draw(tempPath.createTransformedShape(tempTrans));
            }
        }
    }

    void drawLine(float a1, float b1, float a2, float b2) {
        Graphics2D g2d=getBufferGraphics();
        g2d.setRenderingHints(renderingHints);
        g2d.setColor(colors[0]);
        if (useStrokes) g2d.setStroke(lineStroke);
        tempLine.setLine(a1, b1, a2, b2);
        drawAll(g2d, tempLine);
        // paintLine(g2d, a1, b1, a2, b2, false);
        g2d.dispose();
        if (pv!=null) pv.repaint();
        if (tv!=null) tv.repaint();
    }

    void drawGridLine(Graphics2D g2d, float a1, float b1, float a2, float b2) {
        for (int da=mina; da<=maxa; ++da) {
            float a1d=a1+da, a2d=a2+da;
            for (int db=minb; db<=maxb; ++db) {
                float b1d=b1+db, b2d=b2+db;
                tempLine.setLine(a1d*ax+b1d*bx, a1d*ay+b1d*by,
                                 a2d*ax+b2d*bx, a2d*ay+b2d*by);
                g2d.draw(tempLine);
            }
        }
    }

    void expandTilingToPoint(int x, int y) {
        double a, b;
        a=aFromPoint(x, y);
        b=bFromPoint(x, y);
        if (a<mina) mina=(int)Math.floor(a);
        if (a>maxa) maxa=(int)Math.ceil(a);
        if (b<minb) minb=(int)Math.floor(b);
        if (b>maxb) maxb=(int)Math.ceil(b);
    }

    double aFromPoint(double x, double y) {
        return (dby*x-dbx*y)/denom;
    }

    double bFromPoint(double x, double y) {
        return (-day*x+dax*y)/denom;
    }

    public void setRecognizedImage(Group group,
                                   int ax, int ay, int bx, int by,
                                   BufferedImage median) {
        for (Iterator i = hooks.iterator(); i.hasNext(); )
            ((Hooks)i.next()).recognizedImage(group, ax, ay, bx, by, median);
        cntrl.setGroup(group);
        backgroundTile = median;
        hasBackground(true);
        setVectors(ax, ay, bx, by);
    }

    private void hasBackground(boolean hasBackground) {
        backgroundTiles = null;
        showBackground = hasBackground;
        if (cntrl != null)
            cntrl.hasBackground(hasBackground);
    }

    public BufferedImage getBackgroundTile() {
        if (!showBackground) return null;
        return backgroundTile;
    }

    public void showBackground(boolean show) {
        if (backgroundTile == null) return;
        showBackground = show;
        repaintBuffer();
    }

    public BufferedImage renderTileAnisotropic(int width, int height,
                                               boolean transparent) {
        int imageType;
        if (transparent)
            imageType = BufferedImage.TYPE_INT_ARGB;
        else
            imageType = BufferedImage.TYPE_INT_RGB;
        BufferedImage img = new BufferedImage(width, height, imageType);
        Graphics2D g = img.createGraphics();
        if (!transparent) { // fill with background color
            g.setColor(colors[1]);
            g.fillRect(-1, -1, width + 2, height + 2);
        }
        g.setRenderingHints(renderingHints);
        if (showBackground) {
            g.drawImage(backgroundTile, 0, 0, width, height, null);
        }
        try {
            g.scale(width, height);
            g.transform(vectorTransform.createInverse());
        }
        catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
        Iterator i=lines.iterator();
        while (i.hasNext())
            paintLinPath(g, (LinPath)i.next());
        g.dispose();
        return img;
    }

    public static BufferedImage tileImage(BufferedImage in,
                                          int xTiles, int yTiles) {
        if (xTiles == 1 && yTiles == 1) return in;
        int inW = in.getWidth(), inH = in.getHeight();
        int outW = inW*xTiles, outH = inH*yTiles;
        Raster inR = in.getRaster();
        ColorModel cm = in.getColorModel();
        WritableRaster outR = cm.createCompatibleWritableRaster(outW, outH);
        for (int x = 0; x < xTiles; ++x) {
            for (int y = 0; y < yTiles; ++y) {
                outR.setRect(x*inW, y*inH, inR);
            }
        }
        boolean premultiplied = in.isAlphaPremultiplied();
        BufferedImage out = new BufferedImage(cm, outR, premultiplied, null);
        return out;
    }

    public boolean isDebug() {
        return debug;
    }

    private static Properties versionInfo;

    public static Properties getVersionInfo() {
        if (versionInfo != null) return versionInfo;
        versionInfo = new Properties();
        Class clazz = Ornament.class;
        String file = clazz.getName().replace('.', '/') + ".class";
        URL url = clazz.getClassLoader().getResource(file);
        if (url == null) {
            System.err.println("Couldn't find " + file);
            return versionInfo;
        }
        String str = url.toString();
        if (!str.endsWith("/" + file)) {
            System.err.println("Unexpected url " + str);
            return versionInfo;
        }
        str = str.substring(0, str.length() - file.length());
        try {
            url = new URL(str + "META-INF/version.properties");
            InputStream in = url.openStream();
            versionInfo.load(in);
        }
        catch (FileNotFoundException e) {
            System.err.println("No such file: version.properties");
        }
        catch (IOException e) {
            System.err.println(e);
        }
        try {
            url = new URL(str + "META-INF/bzr.properties");
            InputStream in = url.openStream();
            Properties bzr = new Properties(versionInfo);
            bzr.load(in);
            versionInfo = bzr;
        }
        catch (FileNotFoundException e) {
            System.err.println("No such file: bzr.properties");
        }
        catch (IOException e) {
            System.err.println(e);
        }
        return versionInfo;
    }

    public static String getDescription() {
        Properties versionInfo = getVersionInfo();
        String[] args = new String[2];
        args[0] = versionInfo.getProperty("version.number",
                                          "[unknown version]");
        args[1] = "Martin von Gagern";
        return MessageFormat.format("Ornament {0} by {1}", args);
    }

}
