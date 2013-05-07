/*    */ package de.tum.in.gagern.ornament;
/*    */ 
/*    */ import java.awt.geom.AffineTransform;
/*    */ import java.awt.geom.PathIterator;
/*    */ 
/*    */ class LinIterator
/*    */   implements PathIterator
/*    */ {
/*    */   private float[] data;
/*    */   private int pos;
/*    */   private AffineTransform at;
/* 14 */   private static final AffineTransform IDENTITY = new AffineTransform();
/*    */ 
/*    */   public LinIterator(float[] coords, AffineTransform transform) {
/* 17 */     this.data = coords;
/* 18 */     this.at = transform;
/* 19 */     if (this.at != null) return; this.at = IDENTITY;
/*    */   }
/*    */ 
/*    */   public int getWindingRule() {
/* 23 */     return 1;
/*    */   }
/*    */ 
/*    */   public boolean isDone() {
/* 27 */     return (this.pos >= this.data.length);
/*    */   }
/*    */ 
/*    */   public void next() {
/* 31 */     this.pos += 2;
/*    */   }
/*    */ 
/*    */   public int currentSegment(float[] coords) {
/* 35 */     this.at.transform(this.data, this.pos, coords, 0, 1);
/* 36 */     return ((this.pos == 0) ? 0 : 1);
/*    */   }
/*    */ 
/*    */   public int currentSegment(double[] coords) {
/* 40 */     this.at.transform(this.data, this.pos, coords, 0, 1);
/* 41 */     return ((this.pos == 0) ? 0 : 1);
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.LinIterator
 * JD-Core Version:    0.5.3
 */