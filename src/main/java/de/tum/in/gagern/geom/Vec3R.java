/*    */ package de.tum.in.gagern.geom;
/*    */ 
/*    */ import java.awt.geom.Point2D;
/*    */ 
/*    */ public class Vec3R
/*    */ {
/*    */   public final double x;
/*    */   public final double y;
/*    */   public final double z;
/*    */ 
/*    */   public Vec3R(double x, double y, double z)
/*    */   {
/* 14 */     this.x = x;
/* 15 */     this.y = y;
/* 16 */     this.z = z;
/*    */   }
/*    */ 
/*    */   public Vec3R cross(Vec3R that) {
/* 20 */     return new Vec3R(this.y * that.z - (this.z * that.y), this.z * that.x - (this.x * that.z), this.x * that.y - (this.y * that.x));
/*    */   }
/*    */ 
/*    */   public Vec3R add(Vec3R that)
/*    */   {
/* 26 */     return new Vec3R(this.x + that.x, this.y + that.y, this.z + that.z);
/*    */   }
/*    */ 
/*    */   public double abs() {
/* 30 */     return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
/*    */   }
/*    */ 
/*    */   public Vec3R normalize() {
/* 34 */     double r = abs();
/* 35 */     if (this.z < 0.0D) r = -r;
/* 36 */     return new Vec3R(this.x / r, this.y / r, this.z / r);
/*    */   }
/*    */ 
/*    */   public Point2D dehomog() {
/* 40 */     return new Point2D.Double(this.x / this.z, this.y / this.z);
/*    */   }
/*    */ 
/*    */   public boolean isNaN() {
/* 44 */     return ((Double.isNaN(this.x)) || (Double.isNaN(this.y)) || (Double.isNaN(this.z)));
/*    */   }
/*    */ 
/*    */   public String toString() {
/* 48 */     return "Vec3R[" + this.x + ", " + this.y + ", " + this.z + "]";
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.geom.Vec3R
 * JD-Core Version:    0.5.3
 */