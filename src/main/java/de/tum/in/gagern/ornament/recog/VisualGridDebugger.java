package de.tum.in.gagern.ornament.recog;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.List;
import javax.imageio.ImageIO;
import de.tum.in.gagern.ornament.*;

class VisualGridDebugger extends JPanel
    implements GridDebugger, RecognitionListener {

    private int wImg, hImg;

    private Point[] vectorCandidates;

    private List peaks;

    private Point av, bv;

    private BufferedImage background;

    private double best;

    private JudgedPeak current;

    private List judged = new ArrayList();

    private JSlider gridSizeSlider;

    private int gridSize;

    private JLabel msgLabel;

    private double bestPairScore;

    private int bestPairSize;

    private double bestScore;

    public static void main(String[] args) throws Exception {
	JFrame frm = new JFrame("grid debugger");
	VisualGridDebugger vgdbg = new VisualGridDebugger();
	frm.getContentPane().add(vgdbg, BorderLayout.CENTER);
	frm.getContentPane().add(vgdbg.getControls(), BorderLayout.SOUTH);
	frm.setSize(400, 400);
	frm.setLocationRelativeTo(null);
	frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frm.setVisible(true);
	BufferedImage img = ImageIO.read(new File(args[0]));
	Recognizer recog = new Recognizer(vgdbg, vgdbg, img, true);
	recog.gdbg = vgdbg;
	recog.run();
	System.exit(3);
    }

    private Component getControls() {
    	JPanel ctrls = new JPanel();
	ctrls.setLayout(new GridLayout(0, 1, 5, 5));
	ctrls.add(new JButton(new AbstractAction("Next pair") {
		public void actionPerformed(ActionEvent evnt) {
		    unblock(5);
		}
	    }));
	ctrls.add(new JButton(new AbstractAction("Next best") {
		public void actionPerformed(ActionEvent evnt) {
		    unblock(10);
		}
	    }));
	ctrls.add(new JButton(new AbstractAction("Finish") {
		public void actionPerformed(ActionEvent evnt) {
		    unblock(20);
		}
	    }));
	ctrls.add(gridSizeSlider = new JSlider());
	gridSizeSlider.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent evnt) {
		    updateGridSize(gridSizeSlider.getValue());
		}
	    });
	ctrls.add(msgLabel = new JLabel(""));
	return ctrls;
    }

    public synchronized void paintComponent(Graphics g) {
	if (background == null) {
	    super.paintComponent(g);
	    return;
	}
	g.drawImage(background, 0, 0, this);
	g.translate(wImg - 1, hImg - 1);
	g.setColor(Color.BLUE);
	if (av != null) g.drawLine(0, 0, av.x, av.y);
	if (bv != null) g.drawLine(0, 0, bv.x, bv.y);
	if (peaks != null) {
	    g.setColor(Color.BLACK);
	    for (int i = 0; i < gridSize; ++i) {
		Peak peak = (Peak)peaks.get(i);
		drawCross(g, peak.getX(), peak.getY(), 3);
	    }
	}
    }

    private void drawX(Graphics g, int x, int y, int size) {
	g.drawLine(x - size, y - size, x + size, y + size);
	g.drawLine(x - size, y + size, x + size, y - size);
    }

    private void drawCross(Graphics g, int x, int y, int size) {
	g.drawLine(x - size, y, x + size, y);
	g.drawLine(x, y - size, x, y + size);
    }

    public void imageSizes(int wImg, int hImg) {
	this.wImg = wImg;
	this.hImg = hImg;
	int w = 2*wImg - 1, h = 2*hImg - 1;
	background = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
	setPreferredSize(new Dimension(2*wImg-1, 2*hImg-1));
	Container c, p;
	for (c = this; (p = c.getParent()) != null; c = p);
	if (c instanceof Window) ((Window)c).pack();
    }

    public void vectorCandidates(Point[] vectorCandidates) {
	this.vectorCandidates = vectorCandidates;
    }

    public void peaks(List peaks) {
	this.peaks = peaks;
	gridSizeSlider.setMaximum(peaks.size());
    }

    public synchronized void currentPair(int ax, int ay, int bx, int by) {
	this.av = new Point(ax, ay);
	this.bv = new Point(bx, by);
	Point2D.Double p = new Point2D.Double();
	double maxFit = Recognizer.calculateFit(0, 0);
	double minFit = Recognizer.calculateFit(.5, .5);
	AffineTransform trv = new AffineTransform(ax, ay, bx, by, 0, 0);
	try {
	    AffineTransform trvi = trv.createInverse();
	    for (int y = 0; y < background.getHeight(); ++y) {
		int dy = y - hImg + 1;
		for (int x = 0; x < background.getWidth(); ++x) {
		    int dx = x - wImg + 1;
		    p.setLocation(dx, dy);
		    trvi.transform(p, p);
		    double ca = p.x, cb = p.y;
		    int ia = (int)Math.round(ca);
		    int ib = (int)Math.round(cb);
		    double fit = Recognizer.calculateFit(ca - ia, cb - ib);
		    double brightness = (fit - minFit)/(maxFit - minFit);
		    brightness = .6*brightness + .2;
		    double hue;
		    if (fit >= 0) hue = 1/3.;
		    else hue = 0;
		    int color = Color.HSBtoRGB((float)hue, 1.f,
					       (float)brightness);
		    background.setRGB(x, y, color);
		}
	    }
	}
	catch (NoninvertibleTransformException e) {
	    Graphics g = background.createGraphics();
	    g.setColor(Color.YELLOW);
	    g.fillRect(0, 0, background.getWidth(), background.getHeight());
	    g.dispose();
	}
	judged.clear();
	bestPairScore = Double.NEGATIVE_INFINITY;
	bestPairSize = 0;
	block(2);
    }

    public void beginPoint(int ci, Peak cp, Point ip,
			   double fit, double overlap) {
	current = new JudgedPeak(ci, cp, ip, fit, overlap);
	judged.add(current);
    }

    public void skipPoint() {
    }

    public void replacePoint(Peak op) {
    }

    public void judgePoint(double score,
			   double gridCoverage, double peakCoverage) {
	current.judge(score, gridCoverage, peakCoverage);
	if (score > bestPairScore) {
	    bestPairScore = score;
	    bestPairSize = current.ci + 1;
	    if (score > bestScore && bestPairSize > 8)
		bestScore = score;
	}
    }

    public void donePair() {
	updateGridSize(bestPairSize);
	if (bestPairScore == bestScore) block(10);
	else block(5);
    }

    private int blockLevel = 5;

    private boolean blocked;

    private synchronized void block(int level) {
	repaint();
	if (level < blockLevel) return;
	blocked = true;
	do {
	    try {
		wait();
	    }
	    catch (InterruptedException e) {
	    }
	} while (blocked);
    }

    public synchronized void unblock(int level) {
	blockLevel = level;
	blocked = false;
	notify();
    }

    private void updateGridSize(int size) {
	gridSize = size;
	gridSizeSlider.setValue(gridSize);
	if (gridSize == 0) {
	    msgLabel.setText("beginning");
	}
	else if (gridSize > judged.size()) {
	    msgLabel.setText("past end: " + gridSize + " > " + judged.size());
	}
	else {
	    JudgedPeak jp = (JudgedPeak)judged.get(gridSize-1);
	    msgLabel.setText(jp.toString());
	}
	repaint();
    }

    public void recognitionSuccessful(Group group,
				      BufferedImage median,
				      AffineTransform transform) {
	System.exit(0);
    }

    public void recognitionCanceled() {
	System.exit(2);
    }

    public void recognitionFailed(Throwable e) {
	e.printStackTrace();
	System.exit(1);
    }

    private static class JudgedPeak {

	int ci;
	Peak cp;
	Point ip;
	double fit;
	double overlap;

	public JudgedPeak(int ci, Peak cp, Point ip,
			  double fit, double overlap) {
	    this.ci = ci;
	    this.cp = cp;
	    this.ip = ip;
	    this.fit = fit;
	    this.overlap = overlap;
	}

	double score;
	double gridCoverage;
	double peakCoverage;

	public void judge(double score,
			  double gridCoverage, double peakCoverage) {
	    this.score = score;
	    this.gridCoverage = gridCoverage;
	    this.peakCoverage = peakCoverage;
	}

	public String toString() {
	    String format = "score={0,number,0.###} gc={1,number,0.###} pk={2,number,0.###} fit={3,number,0.###}";
	    Object[] args = {
		new Double(score*1e4),
		new Double(gridCoverage*1e2),
		new Double(peakCoverage*1e2),
		new Double(fit),
	    };
	    return MessageFormat.format(format, args);
	}

    }

}
