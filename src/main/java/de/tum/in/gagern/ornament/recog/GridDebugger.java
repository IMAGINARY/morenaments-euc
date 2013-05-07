package de.tum.in.gagern.ornament.recog;

import java.awt.Point;
import java.util.List;

interface GridDebugger {

    public void imageSizes(int wImg, int hImg);
    public void vectorCandidates(Point[] vectorCandidates);
    public void peaks(List list);
    public void currentPair(int ax, int ay, int bx, int by);
    public void beginPoint(int ci, Peak cp, Point ip,
			   double fit, double overlap);
    public void skipPoint();
    public void replacePoint(Peak op);
    public void judgePoint(double score,
			   double gridCoverage, double peakCoverage);
    public void donePair();

    public static class Nop implements GridDebugger {
	public void imageSizes(int wImg, int hImg) {}
	public void vectorCandidates(Point[] vectorCandidates) {}
	public void peaks(List list) {}
	public void currentPair(int ax, int ay, int bx, int by) {}
	public void beginPoint(int ci, Peak cp, Point ip,
			       double fit, double overlap) {}
	public void skipPoint() {}
	public void replacePoint(Peak op) {}
	public void judgePoint(double score,
			       double gridCoverage, double peakCoverage) {}
	public void donePair() {}
    }

}
