package de.tum.in.gagern.ornament.recog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tum.in.gagern.ornament.I18n;
import de.tum.in.gagern.ornament.LoopBounds;
import de.tum.in.gagern.ornament.MinGrid;
import de.tum.in.gagern.ornament.Ornament;
import de.tum.in.gagern.ornament.Group;
import de.tum.in.gagern.ornament.groups.P1;

public class Recognizer implements Runnable {

    private static final boolean doNormalize = false;

    private static final ColorSpace cs =
	ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
    private static final ColorModel cmOpaque =
	new ComponentColorModel(cs, false, true, Transparency.OPAQUE,
				DataBuffer.TYPE_BYTE);
    private static final ColorModel cmAlpha =
	new ComponentColorModel(cs, true, true, Transparency.TRANSLUCENT,
				DataBuffer.TYPE_BYTE);

    private final Component owner;
    private final RecognitionListener listener;
    private final BufferedImage original;
    private final BufferedImage img;
    private final int wImg, hImg;
    private final boolean hasAlpha;
    private ProgressView progressView;
    private ProgressPhase progressTransform, progressDebugAc, progressPeaks;
    private ProgressPhase progressAod, progressGrid;
    private ProgressPhase progressMedian1, progressSymmetry;
    private ProgressPhase progressMedianTile, progressMedian2;
    private List peaks;
    private AffineTransform trv;
    private AffineTransform tri;
    private BufferedImage median;
    private Group group;
    private boolean debug;
    GridDebugger gdbg;

    public Recognizer(Component owner,
		      RecognitionListener listener, BufferedImage img,
		      boolean debug)
		throws IOException {
	this.owner = owner;
	this.listener = listener;
	this.debug = debug;

	original = img;
	wImg = img.getWidth();
	hImg = img.getHeight();

	if (false && !cmOpaque.equals(img.getColorModel()) &&
	    !cmAlpha.equals(img.getColorModel())) {
	    ColorModel cm;
	    if (img.getColorModel().hasAlpha()) {
		cm = cmAlpha;
		hasAlpha = true;
	    }
	    else {
		cm = cmOpaque;
		hasAlpha = false;
	    }
	    WritableRaster raster;
	    raster = cm.createCompatibleWritableRaster(wImg, hImg);
	    this.img = new BufferedImage(cm, raster, true, null);
	}
	else {
	    this.img = img;
	    hasAlpha = img.getColorModel().hasAlpha();
	}

	group = new P1();
    }

    public void run() {
	try {
	    initProgress();
	    colorConvert();
	    transform();
	    aodPeaks(peaks, progressAod);
	    thinPeaks();
	    findGrid();
	    median = getMedian(3, progressMedian1);
	    innerSymmetries();
	    BufferedImage medianTile = getMedian(1, progressMedianTile);
	    debugImg(medianTile, "debugMedianTile");
	    median = getMedianSquare(progressMedian2);
	    debugImg(median, "debugMedianTile2");
	    listener.recognitionSuccessful(group, median, trv);
	}
	catch (CanceledOperationException e) {
	    listener.recognitionCanceled();
	}
	catch (Throwable e) {
	    listener.recognitionFailed(e);
	}
	finally {
	    progressView.close();
	}
    }

    private void initProgress() {
	String title = I18n._("Recognizing pattern");
	String cancelCaption = I18n._("Cancel");
	progressView = new ProgressView(owner, title, cancelCaption);
	progressTransform = progressView.createPhase(3);
	progressDebugAc = progressView.createPhase(1);
	ProgressPhase peaksAndGrid = progressView.createPhase(2);
	progressPeaks = peaksAndGrid.createPhase(1);
	progressAod = peaksAndGrid.createPhase(1);
	progressGrid = peaksAndGrid.createPhase(1);
	ProgressPhase symmetry = progressView.createPhase(5);
	progressMedian1 = symmetry.createPhase(9);
	progressSymmetry = symmetry.createPhase(FEATURE_MATRIX.length+1);
	progressMedianTile = symmetry.createPhase(1);
	progressMedian2 = symmetry.createPhase(12);
	progressView.setNotice(I18n._("initializing"));
	progressView.show();
    }

    private void colorConvert() throws CanceledOperationException {
	if (original == img) return;
	RenderingHints rh = null;
	ColorConvertOp cco = new ColorConvertOp(rh);
	cco.filter(original, img);
    }

    private void transform() throws CanceledOperationException, 
	    IOException, RecognitionException {

	Correlator ac = new Correlator(2*wImg, 2*hImg, true);
	ac.autocorrelate(img, progressTransform);
	float max = ac.getAcMaximum();
	if (max == 0.f)
	    throw new RecognitionException("Black image");
	debugImg(ac.getDebugImage(true, false, -max, max, progressDebugAc),
		 "debugAcGray");
	if (doNormalize) {
	    float minValue = ac.normalize();
	    float maxValue = ac.getAcMaximum();
	    debugImg(ac.getDebugImage(true, false, -maxValue, maxValue,
				      progressDebugAc), "debugAcNormGray");
	}
	peaks = ac.extractPeaks(0, progressPeaks);
    }

    private void debugImg(BufferedImage img, String name) {
	if (!debug) return;
	debugImgImpl(img, name);
    }

    static void debugImgImpl(BufferedImage img, String name) {
	try {
	    javax.imageio.ImageIO.write(img, "png",
					new java.io.File(name + ".png"));
	}
	catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Rank of the peak to use for dominance threshold calculation.
     *
     * Once the areas of dominance have been calculated for all peaks,
     * the list of peaks should be reduced to those most likely to be
     * part of the actual grid of the pattern. For statistical
     * reasons, the area of dominance of a single peak is more
     * appropriate than any average of all the peaks. This constant
     * determines the index into the list (sorted by area of
     * dominance) of the peaks whose area should be used as threshold
     * value.
     *
     * A value of about 8 seems a good choice. In a clear image with
     * enough repetitions of the pattern, this should give the least
     * of the eight peaks surrounding the central peak.
     */
    private static final int peakThresholdRank = 8;

    /**
     * Factor to calculate threshold area of dominance.
     *
     * The threshold area of dominance is calculated by taking the
     * area of dominance of a peak of given rank and multilying it
     * with this factor, which must be between zero and one. A smaller
     * value will lead to more incorrect peaks being considered,
     * whereas too large a value might cut off too many correct peaks,
     * leading to an imperfect grid detection.
     */
    private static final double peakThresholdFactor = 0.2;

    private void aodPeaks(List peaks, ProgressPhase progress)
	    throws RecognitionException, CanceledOperationException {
	progress.begin("calculating area of dominance", peaks.size()*5);
	if (peaks.isEmpty()) throw new AssertionError("no peaks");
	Collections.sort(peaks, new Peak.ValueComparator());
	progress.step(peaks.size());
	if (peaks == this.peaks)
	    debugPeaks(peaks, "debugPeaksValue", false);
	progress.step(peaks.size());
	Peak root = (Peak)peaks.get(0);
	for (int i = 1; i < peaks.size(); ++i) {
	    ((Peak)peaks.get(i)).addToTree(root);
	    progress.step();
	}
	Collections.sort(peaks, new Peak.AodComparator());
	progress.step(peaks.size());
	if (peaks == this.peaks)
	    debugPeaks(peaks, "debugPeaksAod", true);
	progress.done();
    }
    

    private void thinPeaks() throws RecognitionException {
	if (peaks.size() < 2)
	    throw new RecognitionException("Only " + peaks.size() + " peaks");
	if (peaks.size() > peakThresholdRank) {
	    int threshold = ((Peak)peaks.get(peakThresholdRank)).getAod();
	    threshold = (int)(threshold*peakThresholdFactor);
	    System.err.println("threshold = " + Math.sqrt(threshold) + "²");
	    int numPeaks;
	    for (numPeaks = peakThresholdRank + 1;
		 numPeaks < peaks.size(); ++numPeaks)
		if (((Peak)peaks.get(numPeaks)).getAod() < threshold)
		    break;
	    System.err.println("peak list reduced from " + peaks.size() +
			       " to " + numPeaks);
	    peaks.subList(numPeaks, peaks.size()).clear();
	}
	debugPeaks(peaks, "debugPeaksThin", true);
	debugPeaks(peaks, "debugPeaksThinValue", false);
	assert ((Peak)peaks.get(0)).getX() == 0 &&
	       ((Peak)peaks.get(0)).getY() == 0;
	if (peaks.size() < 3)
	    throw new RecognitionException("Image with insufficient structure");
    }

    private void debugPeaks(List peaks, String file, boolean aod) {
	if (!debug) return;
	PrintStream stream = null;
	try {
	    stream = new PrintStream(new FileOutputStream(file + ".peaks"));
	    stream.println("dimensions(" + wImg + ", " + hImg + ");");
	}
	catch (IOException e) {
	    e.printStackTrace();
	    stream = System.err;
	}
	BufferedImage res = new BufferedImage(2*wImg + 9, 2*hImg + 9,
					      BufferedImage.TYPE_INT_RGB);
	Graphics2D g = res.createGraphics();
	g.translate(wImg + 4, hImg + 4);
	Peak peak;
	int x, y;
	float level;
	double scale = Double.NEGATIVE_INFINITY;
	peak = (Peak)peaks.get(0);
	if (aod) level = peak.getAod();
	else level = peak.getValue();
	if (peak.getX() == 0 && peak.getY() == 0 && peaks == this.peaks)
	    scale = Double.NEGATIVE_INFINITY;
	else
	    scale = level;
	for (int i = 1; i < peaks.size(); ++i) {
	    peak = (Peak)peaks.get(i);
	    if (aod) level = peak.getAod();
	    else level = peak.getValue();
	    if (scale < level) scale = level;
	}
	if (aod) scale = Math.sqrt(scale);
	scale = 1./scale;
	for (int i = peaks.size() - 1; i >= 0; --i) {
	    peak = (Peak)peaks.get(i);
	    x = peak.getX();
	    y = peak.getY();
	    if (aod) level = (float)(scale*Math.sqrt(peak.getAod()));
	    else level = (float)(scale*peak.getValue());
	    if (level < 0) level = 0;
	    if (level > 1) level = 1;
	    stream.println("peak(" + x + ", " + y + ", " + level + ");");
	    g.setColor(Color.getHSBColor(2/3.f*(1.f-level),
					 1.f, .3f+.7f*level));
	    int size = (int)(4*level + 1.5f);
	    g.drawLine(x, y - size, x, y + size);
	    g.drawLine(x - size, y, x + size, y);
	}
	peak = (Peak)peaks.get(0);
	x = peak.getX();
	y = peak.getY();
	g.setColor(Color.WHITE);
	if (trv != null && peaks == this.peaks) {
	    double[] matrix = new double[4];
	    trv.getMatrix(matrix);
	    g.drawLine(0, 0,
		       (int)Math.round(matrix[0]),
		       (int)Math.round(matrix[1]));
	    g.drawLine(0, 0,
		       (int)Math.round(matrix[2]),
		       (int)Math.round(matrix[3]));
	}
	else {
	    g.drawLine(x - 5, y - 5, x + 5, y + 5);
	    g.drawLine(x - 5, y + 5, x + 5, y - 5);
	}
	g.dispose();
	debugImg(res, file);
	if (stream != System.err) stream.close();
    }

    /**
     * Limit used when deciding whether two vectors are collinear or not.
     *
     * Two vectors are considered collinear if the square of the sine
     * of the angle between them is at least this value. This constant
     * has to be positive and less than one. A value of 1/128
     * corresponds to an angle of slightly more than 5 degrees.
     */
    private static final double gridCollinear = 1/128.;

    /**
     * Factor to weight vector shortness against grid matching.
     *
     * For pairs of vectors that could be used as the basis of the
     * grid, the squares of the error distances for other peaks is
     * calculated, measured in pixels. The sum of the squared lengths
     * of the grid vectors is added as an additional error term. The
     * intention is that, everything else being equal, the algorithm
     * should prefer shorter vectors over longer ones for the same
     * grid. Too small a number might result in too large vectors
     * being chosen, whereas too large a number might lead to the
     * choice of a grid with inacceptably bad fit in order to reduce
     * vector lengths.
     */
    private static final double gridShortness = 1/8.;

    /**
     * Limiting value to consider whether points are on grid or not.
     *
     * When two integral grid vectors corresponding two to suitable
     * peaks have been found, these vectors are adjusted in a way that
     * best matches all the peaks of the grid. If its squared distance
     * in terms of grid vector units is less than this constant, a
     * given peak is considered part of the grid. Too high a value
     * will lead to the acceptance of spurious peaks during grid
     * fitting, whereas too low a value might cause suboptimal fits
     * for distorted images, as not all grid peaks are considered in
     * the fitting.
     */
    private static final double gridFitness = 1/64.;

    private void findGrid()
	    throws RecognitionException, CanceledOperationException {
	HashSet vectorCandidateSet = new HashSet();
	Point[] vc;
	Comparator[] comparators;
	int nVectorCandidates;
	if (doNormalize) {
	    comparators = new Comparator[] {
		new Peak.LengthComparator(),
		new Peak.DenormalizedComparator(wImg, hImg),
		//new Peak.DenormalizedProductComparator(wImg, hImg),
		new Peak.AodComparator(),
	    };
	    nVectorCandidates = Math.min(peaks.size(), 6);
	}
	else {
	    comparators = new Comparator[] {
		new Peak.AodComparator(),
	    };
	    nVectorCandidates = Math.min(peaks.size(), 9);
	}
	for (int cmp = 0; cmp < comparators.length; ++cmp) {
	    Collections.sort(peaks, comparators[cmp]);
	    for (int i = 0; i < nVectorCandidates; ++i) {
		Peak peak = (Peak)peaks.get(i);
		Point vec = new Point(peak.getX(), peak.getY());
		if (vec.x == 0 && vec.y == 0) continue;
		if (vec.x < 0 || (vec.x == 0 && vec.y > 0)) {
		    vec.x = -vec.x;
		    vec.y = -vec.y;
		}
		vectorCandidateSet.add(vec);
	    }
	}
	vc = new Point[vectorCandidateSet.size()];
	vc = (Point[])vectorCandidateSet.toArray(vc);
	nVectorCandidates = vc.length;
	if (doNormalize) {
	    Collections.sort(peaks,
		new Peak.DenormalizedProductComparator(wImg, hImg));
	}

	if (gdbg == null) gdbg = new GridDebugger.Nop();
	gdbg.imageSizes(wImg, hImg);
	gdbg.vectorCandidates(vc);
	gdbg.peaks(peaks);

	Point2D pt = new Point2D.Double();
	Point bestA = null, bestB = null;
	int bestGridsize = 0;
	int iterations = (vc.length - 1)*vc.length/2;
	progressGrid.begin("finding grid vectors", iterations+20);
	double bestScore = 0;
	Map gridPoints = new HashMap(peaks.size());
	loopA: for (int ai = 0; ai < vc.length; ++ai) {
	    Point av = vc[ai];
	    loopB: for (int bi = ai + 1; bi < vc.length; ++bi) {
		progressGrid.step();
		Point bv = vc[bi];
		int ax = av.x, ay = av.y, bx = bv.x, by = bv.y;
		double alSq = ax*ax + ay*ay;
		double blSq = bx*bx + by*by;
		double det = ax*by - bx*ay;
		if (det*det < alSq*blSq*gridCollinear) // almost collinear
		    continue;

		// minimize grid to not distort the fit value distribution
		MinGrid minGrid = new MinGrid(ax, ay, bx, by);
		ax = minGrid.getA1()*av.x + minGrid.getB1()*bv.x;
		ay = minGrid.getA1()*av.y + minGrid.getB1()*bv.y;
		bx = minGrid.getA2()*av.x + minGrid.getB2()*bv.x;
		by = minGrid.getA2()*av.y + minGrid.getB2()*bv.y;

		gdbg.currentPair(ax, ay, bx, by);
		LoopBounds bounds;
		try {
		    bounds = new LoopBounds(ax, ay, bx, by);
		}
		catch (NoninvertibleTransformException e) {
		    continue; // still too collinear
		}
		bounds.boundsFor(-wImg, -hImg, wImg, hImg);
		bounds.stepLowerBounds();
		double allOverlap = 0, allAod = 0, gridFit = 0, peakFit = 0;
		for (int a = bounds.getMinA(); a <= bounds.getMaxA(); ++a) {
		    for (int b = bounds.getMinB(); b <= bounds.getMaxB(); ++b) {
			int dx = Math.abs(a*ax + b*bx);
			int dy = Math.abs(a*ay + b*by);
			dx = Math.max(0, wImg - dx);
			dy = Math.max(0, hImg - dy);
			double overlap = dx*(double)dy;
			allOverlap += overlap;
		    }
		}
		gridPoints.clear();
		loopC: for (int ci = 0; ci < peaks.size(); ++ci) {
		    Peak cp = (Peak)peaks.get(ci);
		    int cx = cp.getX(), cy = cp.getY();
		    double ca = (cx*by - bx*cy)/det;
		    double cb = (ax*cy - cx*ay)/det;
		    int ia = (int)Math.round(ca);
		    int ib = (int)Math.round(cb);
		    double fit = calculateFit(ca - ia, cb - ib);
		    cp.setFit(fit);
		    double overlap = (wImg - Math.abs(cx))*
		                     (hImg - Math.abs(cy));
		    final double denormalize = doNormalize ? overlap : 1;
		    Point ip = new Point(ia, ib);
		    gdbg.beginPoint(ci, cp, ip, fit, overlap);
		    allAod += denormalize*cp.getAod();
		    Peak op = null;
		    if (ia != 0 || ib != 0) op = (Peak)gridPoints.put(ip, cp);
		    if (op != null) {
			if (op.getFit() > fit) {
			    gridPoints.put(ip, op);
			    gdbg.skipPoint();
			    continue;
			}
			gdbg.replacePoint(op);
			gridFit -= op.getFit()*overlap;
			peakFit -= op.getFit()*denormalize*op.getAod();
		    }
		    double gridFitSingle = fit*overlap;
		    double peakFitSingle = fit*denormalize*cp.getAod();
		    gridFit += gridFitSingle;
		    peakFit += peakFitSingle;
		    double gridCoverage = gridFit / allOverlap;
		    double peakCoverage = peakFit / allAod;
		    double score;
		    if (gridCoverage > 0 && peakCoverage > 0)
			score = gridCoverage * peakCoverage;
		    else
			score = Math.min(gridCoverage, peakCoverage);
		    gdbg.judgePoint(score, gridCoverage, peakCoverage);
		    if (score > bestScore && ci >= 8) {
			bestScore = score;
			bestA = new Point(ax, ay);
			bestB = new Point(bx, by);
			bestGridsize = ci + 1;
		    }
		} // loopC
		gdbg.donePair();
	    } // loopB
	} // loopA
	progressGrid.step();
	if (bestA == null)
	    throw new RecognitionException("No two independent translations");
	double ax = bestA.x;
	double ay = bestA.y;
	double bx = bestB.x;
	double by = bestB.y;
	if (ax*bx + ay*by < 0.) {
	    // ensure an angle of no more than 90° between vectors
	    bx *= -1.;
	    by *= -1.;
	}
	if (ax*by - ay*bx > 0.) {
	    // ensure correct order of vectors: counter clockwise
	    double t;
	    t = ax; ax = bx; bx = t;
	    t = ay; ay = by; by = t;
	}
	peaks.subList(bestGridsize, peaks.size()).clear();
	progressGrid.step();

	// Find more precise vectors by least squares fitting
	double det = ax*by - bx*ay;
	double a2Sum = 0., b2Sum = 0., abSum = 0.;
	double axSum = 0., aySum = 0., bxSum = 0., bySum = 0.;
	for (int ci = 1; ci < peaks.size(); ++ci) {
	    Peak cp = (Peak)peaks.get(ci);
	    double cx = cp.getX(), cy = cp.getY();
	    double da = (cx*by - bx*cy)/det, db = (ax*cy - cx*ay)/det;
	    double ca = Math.rint(da), cb = Math.rint(db);
	    double ea = da - ca, eb = db - cb, err = ea*ea + eb*eb;
	    if (err > gridFitness)
		continue; // Too far off to be considered for fitting
	    a2Sum += ca*ca;
	    b2Sum += cb*cb;
	    abSum += ca*cb;
	    axSum += ca*cx;
	    aySum += ca*cy;
	    bxSum += cb*cx;
	    bySum += cb*cy;
	}
	det = (a2Sum*b2Sum - abSum*abSum);
	MinGrid minGrid = new MinGrid((axSum*b2Sum - abSum*bxSum)/det,
				      (aySum*b2Sum - abSum*bySum)/det,
				      (a2Sum*bxSum - axSum*abSum)/det,
				      (a2Sum*bySum - aySum*abSum)/det);
	trv = minGrid.getTransform(false);
	System.err.println("fit: " + trv);
	debugPeaks(peaks, "debugGrid", true);
	progressGrid.done();
    }

    static double calculateFit(double errorA, double errorB) {
	errorA = Math.abs(errorA);
	errorB = Math.abs(errorB);

	// formula from r70
	errorA -= .5;
	errorB -= .5;
	double error = (errorA*errorA + errorB*errorB)*2;
	return 10*error - 9;

	// quadratic
	//return .05 - errorA*errorA - errorB*errorB;

	// cubic
	//double errorMin = Math.max(errorA, errorB);
	//double error = errorA*errorA + errorB*errorB + errorMin*errorMin;
	//return 1/64. - error;
    }

    private BufferedImage getMedian(int repeat, ProgressPhase progress)
	throws CanceledOperationException, NoninvertibleTransformException
    {
	Point2D.Double p = new Point2D.Double();
	p.setLocation(0, 0);
	trv.transform(p, p);
	Rectangle2D bounds = new Rectangle2D.Double(p.getX(), p.getY(), 0, 0);
	p.setLocation(repeat, 0); trv.transform(p, p); bounds.add(p);
	p.setLocation(0, repeat); trv.transform(p, p); bounds.add(p);
	p.setLocation(repeat, repeat); trv.transform(p, p); bounds.add(p);
	int w = (int)Math.round(bounds.getWidth());
	int h = (int)Math.round(bounds.getHeight());
	ColorModel cm = repeat == 1 ? cmAlpha : img.getColorModel();

	// tri is a slight modification of trv,
	// adjusted to integral dimensions w x h
	// and non-negative x/y coordinates within the standard tile
	tri = AffineTransform.getScaleInstance(w/bounds.getWidth(),
					       h/bounds.getHeight());
	tri.translate(-bounds.getX(), -bounds.getY());
	tri.concatenate(this.trv);
	tri.translate(repeat/2, repeat/2);

	AffineTransform invTri = tri.createInverse();
	boolean singleTile = (repeat == 1);
	return getMedian(progress, w, h, cm, singleTile, invTri);
    }

    private BufferedImage getMedianSquare(ProgressPhase progress)
	throws CanceledOperationException, NoninvertibleTransformException
    {
	double ax = trv.getScaleX(), ay = trv.getShearY();
	double bx = trv.getShearX(), by = trv.getScaleY();
	double sizeSq = Math.max(ax*ax + ay*ay, bx*bx + by*by);
	int size = (int)Math.round(Math.sqrt(sizeSq));
	tri = AffineTransform.getScaleInstance(size, size);

	double invSize = 1./size;
	AffineTransform invTri;
	invTri = AffineTransform.getScaleInstance(invSize, invSize);
	ColorModel cm = img.getColorModel();
	boolean singleTile = false;
	return getMedian(progress, size, size, cm, singleTile, invTri);
    }

    private BufferedImage getMedian(ProgressPhase progress, int w, int h,
				    ColorModel cm, boolean singleTile,
				    AffineTransform tr)
	throws CanceledOperationException, NoninvertibleTransformException
    {
	progress.begin("getting median tile", w*h);

	Point2D.Double p1 = new Point2D.Double();
	Point2D.Double p2 = new Point2D.Double();
	Point2D.Double p3 = new Point2D.Double();
	boolean fillInAlpha = cm.hasAlpha() && !hasAlpha;
	Raster in = img.getRaster();
	WritableRaster out = cm.createCompatibleWritableRaster(w, h);
	BufferedImage median = new BufferedImage(cm, out, true, null);

	LoopBounds ib = new LoopBounds(trv);
	ib.boundsFor(0, 0, wImg - 1, hImg - 1);
	ib.stepLowerBounds();

	int[][] samples = new int[4][ib.getCount()*group.countTransforms()];
	samples[3][0] = 0xff;
	for (int y1 = 0; y1 < h; ++y1) {
	    for (int x1 = 0; x1 < w; ++x1) {
		p1.setLocation(x1 + .5, y1 + .5);
		tr.transform(p1, p1);
		if (!singleTile) {
		    p1.x -= Math.floor(p1.x);
		    p1.y -= Math.floor(p1.y);
		}
		else if (p1.x < 0 || p1.y < 0 || p1.x >= 1 || p1.y >= 1) {
		    progress.step();
		    continue;
		}
		int orbitLen = 0;
		for (int i = 0; i < group.countTransforms(); ++i) {
		    group.getTransform(i).transform(p1, p2);
		    p2.x -= Math.floor(p2.x);
		    p2.y -= Math.floor(p2.y);
		    for (int a = ib.getMinA(); a <= ib.getMaxA(); ++a) {
			for (int b = ib.getMinB(); b <= ib.getMaxB(); ++b) {
			    p3.setLocation(p2.x + a, p2.y + b);
			    trv.transform(p3, p3);
			    int x2 = (int)Math.floor(p3.x);
			    int y2 = (int)Math.floor(p3.y);
			    if (x2 < 0 || x2 >= wImg || y2 < 0 || y2 >= hImg)
				continue;
			    for (int band = 0; band < in.getNumBands(); ++band)
				samples[band][orbitLen] =
				in.getSample(x2, y2, band);
			    ++orbitLen;
			}
		    }
		}
		// We no longer do real medians, but instead take the average
		// of the central third of the sample values. This gets rid of
		// outliers but yields smoother results on noisy or gridded
		// input.
		int start = orbitLen/3;
		int end = orbitLen - start;
		int count = end - start;
		int half = count / 2;
		for (int band = 0; band < in.getNumBands(); ++band) {
		    int[] sb = samples[band];
		    Arrays.sort(sb, 0, orbitLen);
		    int sum = half;
		    for (int i = start; i < end; ++i)
			sum += sb[i];
		    out.setSample(x1, y1, band, sum/count);
		}
		if (fillInAlpha)
		    out.setSample(x1, y1, 3, 0xff);
		progress.step();
	    }
	}
	progress.done();
	return median;
    }

    private static final int FEATURE_ROT6 = 0;
    private static final int FEATURE_ROT3 = 1;
    private static final int FEATURE_ROT4 = 2;
    private static final int FEATURE_ROT2 = 3;

    private static final int BIT_ROT6 = 1 << FEATURE_ROT6;
    private static final int BIT_ROT3 = 1 << FEATURE_ROT3;
    private static final int BIT_ROT4 = 1 << FEATURE_ROT4;
    private static final int BIT_ROT2 = 1 << FEATURE_ROT2;

    private static final String[] FEATURE_NAME = {
	"rot6",	"rot3",	"rot4",	"rot2",
    };

    private static final double[][] FEATURE_MATRIX = {
	{0, 1, -1, 1, 1, 0},  // 6-fold rotation
	{-1, 1, -1, 0, 1, 0}, // 3-fold rotation
	{0, 1, -1, 0, 1, 0},  // 4-fold rotation
	{-1, 0, 0, -1, 1, 1}, // 2-fold rotation
    };

    private void innerSymmetries()
	    throws CanceledOperationException,
	           NoninvertibleTransformException,
	           IOException {
	FeatureInspector fi =
	    new FeatureInspector(median, tri, trv, progressSymmetry, debug);
	group = fi.decideInnerSymmetries();
    }

}
