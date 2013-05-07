/*    */ package de.tum.in.gagern.ornament;
/*    */ 
/*    */ import java.awt.BasicStroke;
/*    */ import java.awt.Color;
/*    */ import java.awt.Shape;
/*    */ import java.awt.geom.AffineTransform;
/*    */ import java.awt.geom.GeneralPath;
/*    */ import java.awt.geom.PathIterator;
/*    */ 
/*    */ public class LinPath
/*    */ {
/*    */   private float[] coords;
/*    */   private Color color;
/*    */   private BasicStroke stroke;
/*    */   private float width;
/*    */ 
/*    */   LinPath(float[] coords, Color color, BasicStroke stroke)
/*    */   {
/* 18 */     this.coords = coords;
/* 19 */     this.color = color;
/* 20 */     this.stroke = stroke;
/*    */   }
/*    */ 
/*    */   public float[] getCoordinates() {
/* 24 */     return this.coords;
/*    */   }
/*    */ 
/*    */   public Color getColor() {
/* 28 */     return this.color;
/*    */   }
/*    */ 
/*    */   public BasicStroke getStroke() {
/* 32 */     return this.stroke;
/*    */   }
/*    */ 
/*    */   public Shape getShape() {
/* 36 */     GeneralPath path = new GeneralPath(1, this.coords.length / 2);
/* 37 */     path.moveTo(this.coords[0], this.coords[1]);
/* 38 */     for (int i = 2; i < this.coords.length; i += 2)
/* 39 */       path.lineTo(this.coords[i], this.coords[(i + 1)]);
/* 40 */     return path;
/*    */   }
/*    */ 
/*    */   public PathIterator getPathIterator(AffineTransform at) {
/* 44 */     return new LinIterator(this.coords, at);
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.LinPath
 * JD-Core Version:    0.5.3
 */