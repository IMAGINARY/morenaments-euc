package de.tum.in.gagern.ornament.recog;

import java.util.Comparator;

class Peak {

    private int x;

    private int y;

    private float value;

    private int aod;

    private double fit;

    private Peak parent;

    private int quadrant;

    private Peak[] children;

    public Peak(int x, int y , float value, int wImg, int hImg) {
	this.x = x;
	this.y = y;
	this.value = value;
	int dist = Math.min(wImg - Math.abs(x), hImg - Math.abs(y));
	this.aod = dist * dist;
	this.parent = null;
	this.quadrant = -1;
	this.children = new Peak[4];
	// System.err.println("Peak at " + x + " / " + y + " with initial aod " + aod);
    }

    public float getValue() { return value; }

    public float getValueDenormalized(int wImg, int hImg) {
	return value*
	    Math.max(1, wImg - Math.abs(x))*
	    Math.max(1, hImg - Math.abs(y));
    }

    public int getAod() { return aod; }

    public int getX() { return x; }

    public int getY() { return y; }

    private int getLengthSq() { return x*x + y*y; }

    public double getFit() { return fit; }

    public void setFit(double fit) { this.fit = fit; }

    public void addToTree(Peak root) {
	int q;
	for(Peak parent = root; ; parent = parent.children[q]) {
	    int d = distSq(parent);
	    if (aod > d)
		aod = d;
	    q = quadrant(parent);
	    if (parent.children[q] == null) {
		this.parent = parent;
		this.quadrant = q;
		break;
	    }
	}
	int neighborInterest = 0xf;
	for (Peak ancestor = parent; ancestor != null;
	     ancestor = ancestor.parent) {
	    int dx = distSq(x, ancestor.x), dy = distSq(y, ancestor.y);
	    int neighborExam = 0;
	    switch (q) {
	    case 0:
		assert x >= ancestor.x && y >= ancestor.y;
		if (dx >= aod) neighborInterest &= ~1;
		else aodNeighbor(ancestor.children[1]);
		if (dy >= aod) neighborInterest &= ~4;
		else aodNeighbor(ancestor.children[2]);
		break;
	    case 1:
		assert x <  ancestor.x && y >= ancestor.y;
		if (dx >= aod) neighborInterest &= ~2;
		else aodNeighbor(ancestor.children[0]);
		if (dy >= aod) neighborInterest &= ~4;
		else aodNeighbor(ancestor.children[3]);
		break;
	    case 2:
		assert x >= ancestor.x && y <  ancestor.y;
		if (dx >= aod) neighborInterest &= ~1;
		else aodNeighbor(ancestor.children[3]);
		if (dy >= aod) neighborInterest &= ~8;
		else aodNeighbor(ancestor.children[0]);
		break;
	    case 3:
		assert x <  ancestor.x && y <  ancestor.y;
		if (dx >= aod) neighborInterest &= ~2;
		else aodNeighbor(ancestor.children[2]);
		if (dy >= aod) neighborInterest &= ~8;
		else aodNeighbor(ancestor.children[1]);
		break;
	    }
	    if (neighborInterest == 0) break;
	    q = ancestor.quadrant;
	}
	parent.children[quadrant] = this;
    }

    private void aodNeighbor(Peak peak) {
	if (peak == null || peak.value < value) return;
	int dx = distSq(x, peak.x), dy = distSq(y, peak.y), d = dx + dy;
	if (aod > d) aod = d;
	int q = quadrant(peak);

	// Descend into children only if quadrant is near enough.
	aodNeighbor(peak.children[q]);
	if (dx < aod) aodNeighbor(peak.children[q ^ 1]);
	if (dy < aod) aodNeighbor(peak.children[q ^ 2]);
    }

    private int distSq(Peak that) {
	int dx = this.x - that.x, dy = this.y - that.y;
	return dx*dx + dy*dy;
    }

    private int distSq(int a, int b) {
	int d = a - b;
	return d*d;
    }

    private int quadrant(Peak parent) {
	return (x >= parent.x ? 0 : 1) | (y >= parent.y ? 0 : 2);
    }

    private static abstract class StaticComparator implements Comparator {
	public boolean equals(Object obj) {
	    return obj != null && this.getClass().equals(obj.getClass());
	}
	public int hashCode() {
	    return getClass().hashCode();
	}
    }

    public static class ValueComparator extends StaticComparator {
	public int compare(Object o1, Object o2) {
	    return -Float.compare(((Peak)o1).getValue(), ((Peak)o2).getValue());
	}
    }

    public static class DenormalizedComparator implements Comparator {
	private int wImg;
	private int hImg;
	public DenormalizedComparator (int wImg, int hImg) {
	    this.wImg = wImg;
	    this.hImg = hImg;
	}
	public int compare(Object o1, Object o2) {
	    return -Float.compare(((Peak)o1).getValueDenormalized(wImg, hImg),
				  ((Peak)o2).getValueDenormalized(wImg, hImg));
	}
	public boolean equals(Object obj) {
	    if (obj == null || !this.getClass().equals(obj.getClass()))
		return false;
	    DenormalizedComparator that = (DenormalizedComparator)obj;
	    return this.wImg == that.wImg && this.hImg == that.hImg;
	}
	public int hashCode() {
	    return wImg ^ (hImg << 16) ^ (hImg >> 16) ^ getClass().hashCode();
	}
    }

    public static class DenormalizedProductComparator implements Comparator {
	private int wImg;
	private int hImg;
	public DenormalizedProductComparator (int wImg, int hImg) {
	    this.wImg = wImg;
	    this.hImg = hImg;
	}
	public int compare(Object o1, Object o2) {
	    Peak p1 = (Peak)o1, p2 = (Peak)o2;
	    int aod1 = p1.getAod(), aod2 = p2.getAod();
	    float v1 = p1.getValueDenormalized(wImg, hImg);
	    float v2 = p2.getValueDenormalized(wImg, hImg);
	    return -Float.compare(aod1*v1, aod2*v2);
	}
	public boolean equals(Object obj) {
	    if (obj == null || !this.getClass().equals(obj.getClass()))
		return false;
	    DenormalizedComparator that = (DenormalizedComparator)obj;
	    return this.wImg == that.wImg && this.hImg == that.hImg;
	}
	public int hashCode() {
	    return wImg ^ (hImg << 16) ^ (hImg >> 16) ^ getClass().hashCode();
	}
    }

    public static class ProductComparator extends StaticComparator {
	public int compare(Object o1, Object o2) {
	    Peak p1 = (Peak)o1, p2 = (Peak)o2;
	    int aod1 = p1.getAod(), aod2 = p2.getAod();
	    float v1 = p1.getValue(), v2 = p2.getValue();
	    return -Float.compare(aod1*v1, aod2*v2);
	}
    }

    public static class AodComparator extends StaticComparator {
	public int compare(Object o1, Object o2) {
	    int aod1 = ((Peak)o1).getAod(), aod2 = ((Peak)o2).getAod();
	    return aod2 - aod1;
	}
    }

    public static class LengthComparator extends StaticComparator {
	public int compare(Object o1, Object o2) {
	    int l1 = ((Peak)o1).getLengthSq(), l2 = ((Peak)o2).getLengthSq();
	    return l1 - l2;
	}
    }

}
