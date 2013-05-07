package de.tum.in.gagern.ornament.recog;

import java.awt.Component;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import javax.swing.JOptionPane;

import de.tum.in.gagern.ornament.Group;
import de.tum.in.gagern.ornament.I18n;
import de.tum.in.gagern.ornament.groups.*;

class FeatureInspector {

    private final AffineTransform tr = new AffineTransform();

    private final AffineTransform trv, scale, triInv;

    private final BufferedImage median, img1, img2;

    private final ProgressPhase[] progress;

    private final Correlator corr;

    private final float minimumValue, valueScale;

    private final RenderingHints rh;

    private final boolean debug;

    public FeatureInspector(BufferedImage median,
			    AffineTransform tri,
			    AffineTransform trv,
			    ProgressPhase progressPhase,
			    boolean debug)
	    throws CanceledOperationException,
	           NoninvertibleTransformException,
	           IOException {

	this.median = median;
	this.trv = trv;
	this.debug = debug;

	progress = progressPhase.createPhases(9);
	progress[0].setNoticePrefix("Symmetry reference tile: ");
	ProgressPhase progressTransform = progress[0].createPhase(1);
	ProgressPhase progressCorrelate = progress[0].createPhase(5);
	ProgressPhase progressRemainder = progress[0].createPhase(1);

	progressTransform.begin("preparing tile");
	double ax = tri.getScaleX(), ay = tri.getShearY();
	double bx = tri.getShearX(), by = tri.getScaleY();
	double sizeSq = Math.max(ax*ax + ay*ay, bx*bx + by*by);
	int size = (int)Math.round(Math.sqrt(sizeSq));
	corr = new Correlator(size, size, false);
	size = corr.getWidth();
	ColorModel cm = median.getColorModel();
	img1 = new BufferedImage(cm,
				 cm.createCompatibleWritableRaster(size, size),
				 true, null);
	img2 = new BufferedImage(cm,
				 cm.createCompatibleWritableRaster(size, size),
				 true, null);
	rh = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	scale = AffineTransform.getScaleInstance(size, size);
	triInv = tri.createInverse();
	tr.setTransform(scale);
	tr.concatenate(triInv);
	AffineTransformOp op = new AffineTransformOp(tr, rh);
	op.filter(median, img1);
	progressTransform.done();

	corr.autocorrelate(img1, progressCorrelate);
	progressRemainder.begin();
	float maximumValue = corr.getMaximum().getValue();
	minimumValue = corr.getMinimum().getValue();
	valueScale = 1/(maximumValue - minimumValue);
	debugImg(img1, "debugSymIdentityTile");
	progressRemainder.done();

    }

    private void debugImg(BufferedImage img, String name) {
	if (!debug) return;
	Recognizer.debugImgImpl(img, name);
    }
    
    private boolean hasMirror(String name, double[] matrix,
			      Point2D.Double pos, ProgressPhase progress)
	    throws CanceledOperationException,
	           NoninvertibleTransformException,
	           IOException {
	progress.setNoticePrefix("Symmetry " + name + ": ");
	ProgressPhase progressTransform = progress.createPhase(1);
	ProgressPhase progressCorrelate = progress.createPhase(5);
	ProgressPhase progressCreateImg = progress.createPhase(2);
	ProgressPhase progressRemainder = progress.createPhase(1);
	
	// Autocorrelate with transformed tile
	progressTransform.begin("preparing tile");
	AffineTransform tr = new AffineTransform(scale);
	tr.concatenate(new AffineTransform(matrix));
	tr.concatenate(triInv);
	AffineTransformOp op = new AffineTransformOp(tr, rh);
	op.filter(median, img2);
	debugImg(img2, "debugSym_" + name + "_Tile");
	progressTransform.done();
	
	corr.correlate(img1, img2, progressCorrelate);
	BufferedImage acImg =
	    corr.getDebugImage(true, true, 0, 0, progressCreateImg);
	debugImg(acImg, "debugSym_" + name + "_Ac");

	progressRemainder.begin();
	Peak peak = corr.getMaximum();
	float probability = (peak.getValue() - minimumValue)*valueScale;
	pos.setLocation(peak.getX(), peak.getY());
	scale.inverseTransform(pos, pos);
	return decide(name, probability, pos, progress);
    }

    private boolean decide(String name, float probability, Point2D.Double pos,
			   ProgressPhase progress)
	throws CanceledOperationException
    {
	boolean decision = (probability > .75);
	System.err.println("Symmetry " + name +
			   " with probability " + probability*100 +
			   "% at location " +
			   pos.getX() + "/" + pos.getY() +
			   " defaults to " + decision);
	Component parent = progress.getComponent();
	if (parent != null) {
	    Object[] args = { name, new Float(probability),
			      new Integer(decision ? 1 : 0) };
	    String msg = I18n._("recog.decide", args);
	    int result =
		JOptionPane.showConfirmDialog(parent, msg, "Recognition",
					      JOptionPane.YES_NO_CANCEL_OPTION,
					      JOptionPane.QUESTION_MESSAGE);
	    switch (result) {
	    case JOptionPane.YES_OPTION:
		decision = true;
		break;
	    case JOptionPane.NO_OPTION:
		decision = false;
		break;
	    case JOptionPane.CANCEL_OPTION:
		throw new CanceledOperationException();
	    }
	}
	return decision;
    }


    private boolean hasRotation(String name, double[] matrix,
				Point2D.Double pos, ProgressPhase progress)
	    throws CanceledOperationException,
	           NoninvertibleTransformException,
	           IOException {
	if (!hasMirror(name, matrix, pos, progress)) return false;
	tr.setTransform(matrix[0]-1, matrix[1],
			matrix[2], matrix[3]-1,
			0, 0);
	// System.err.println(tr.createInverse());
	tr.inverseTransform(pos, pos);
	pos.x -= Math.floor(pos.x);
	pos.y -= Math.floor(pos.y);
	trv.translate(pos.x, pos.y);
	return true;
    }

    private static final double[]
	R6 = {0, 1, -1, 1, 1, 0},   // 6-fold rotation
	R3 = {-1, 1, -1, 0, 1, 0},  // 3-fold rotation
	R4 = {0, 1, -1, 0, 1, 0},   // 4-fold rotation
	R2 = {-1, 0, 0, -1, 1, 1},  // 2-fold rotation
	Mab = {0, 1, 1, 0, 0, 0},   // reflection at a+b
	Mba = {0, -1, -1, 0, 1, 1}, // reflection at a-b
	Ma = {1, 0, 0, -1, 0, 1},   // reflection at a with 90 degree base
	Mb = {-1, 0, 0, 1, 1, 0},   // reflection at b with 90 degree base
	M3 = {1, 0, 1, -1, 0, 1};   // reflection at a with 60 degree base

    public Group decideInnerSymmetries()
	    throws CanceledOperationException,
	           NoninvertibleTransformException,
	           IOException {
	Point2D.Double rPos = new Point2D.Double();
	Point2D.Double mPos = new Point2D.Double();
	Group group;

	if (hasRotation(I18n._("recog.R6"), R6, rPos, progress[1])) {
	    if (hasMirror(I18n._("recog.Mab"), Mab, mPos, progress[4])) {
		group = new P6mm();
	    }
	    else {
		group = new P6();
	    }
	}
	else if (hasRotation(I18n._("recog.R3"), R3, rPos, progress[2])) {
	    if (hasMirror(I18n._("recog.Mab"), Mab, mPos, progress[4])) {
		group = new P3m1();
	    }
	    else if (hasMirror(I18n._("recog.M3"), M3, mPos, progress[6])) {
		double x = -mPos.getX() - 2*rPos.getX() - rPos.getY();
		double y = -mPos.getY() - rPos.getY() + rPos.getX();
		x -= Math.floor(x);
		y -= Math.floor(y);
		if (x + y > 1/3. && x + y < 1/2. && x < 2/3. && y < 2/3.)
		    trv.translate(2/3., 2/3.);
		else if ( x + y < 2/3. && x + y >= 1/2. && x > 1/3. && y > 1/3.)
		    trv.translate(1/3., 1/3.);
		group = new P31m();
	    }
	    else {
		group = new P3();
	    }
	}
	else if (hasRotation(I18n._("recog.R4"), R4, rPos, progress[3])) {
	    if (hasMirror(I18n._("recog.Mab"), Mab, mPos, progress[6])) {
		// L4
		double rd = rPos.getX() - rPos.getY();
		double x = -mPos.getX() + rd;
		double y = -mPos.getY() - rd;
		x -= Math.floor(x);
		y -= Math.floor(y);
		if (x + y < 0.5 || x + y > 1.5 || x - y > 0.5 || y - x > 0.5) {
		    group = new P4mm();
		}
		else {
		    group = new P4gm();
		    trv.translate(0.5, 0);
		}
	    }
	    else {
		group = new P4();
	    }
	}
	else if (hasRotation(I18n._("recog.R2"), R2, rPos, progress[4])) {
	    if (hasMirror(I18n._("recog.Mab"), Mab, mPos, progress[6])) {
		double x = -mPos.getX() - 2*rPos.getX();
		double y = -mPos.getY() - 2*rPos.getY();
		x -= Math.floor(x);
		y -= Math.floor(y);
		if (x + y > 0.5 && x + y < 1.5 && x - y < 0.5 && y - x < 0.5)
		    trv.translate(0, 0);
		else
		    trv.translate(0.5, 0);
		group = new Cmm();
	    }
	    else if (hasMirror(I18n._("recog.Ma"), Ma, mPos, progress[7])) {
		// L2
		double x = -mPos.getX() + .25;
		double y = -mPos.getY() - 2*rPos.getY() + .25;
		x -= Math.floor(x);
		y -= Math.floor(y);
		if (x < 0.5) {
		    if (y < 0.5) {
			group = new Pmm();
		    }
		    else {
			group = new Pmg();
			// mirror axis parallel to a vector
			// glide reflection parallel to b vector
			trv.translate(0, 0.25);
		    }
		}
		else {
		    if (y < 0.5) {
			group = new Pmg();
			// mirror axis parallel to b vector
			// glide reflection parallel to a vector
			trv.concatenate(new AffineTransform(0, 1, 1, 0,
							    0.25, 0));
		    }
		    else {
			group = new Pgg();
		    }
		}
		
	    }
	    else {
		group = new P2();
	    }
	}
	else {
	    if (hasMirror(I18n._("recog.Mab"), Mab, mPos, progress[5])) {
		group = new Cm();
	    }
	    else if (hasMirror(I18n._("recog.Mba"), Mba, mPos, progress[6])) {
		group = new Cm();
		trv.concatenate(new AffineTransform(0, 1, -1, 0, 0, 0));
	    }
	    else if (hasMirror(I18n._("recog.Ma"), Ma, mPos, progress[7])) {
		// La
		double x = -mPos.getX() + .25;
		double y = -mPos.getY()/2;
		x -= Math.floor(x);
		y -= Math.floor(y);
		trv.translate(0, y);
		if (x < .5)
		    group = new Pm();
		else
		    group = new Pg();
	    }
	    else if (hasMirror(I18n._("recog.Mb"), Mb, mPos, progress[8])) {
		// Lb
		double x = -mPos.getX()/2;
		double y = -mPos.getY() + .25;
		x -= Math.floor(x);
		y -= Math.floor(y);
		trv.concatenate(new AffineTransform(0, 1, 1, 0, x, 0));
		if (y < .5)
		    group = new Pm();
		else
		    group = new Pg();
	    }
	    else {
		group = new P1();
	    }
	}
	progress[progress.length-1].done();
	return group;
    }
    

}
