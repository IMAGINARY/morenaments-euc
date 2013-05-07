/*    */ package de.tum.in.gagern.ornament;
/*    */ 
/*    */ import java.awt.geom.Point2D;
/*    */ import java.util.Comparator;
/*    */ 
/*    */ class AngleCmp
/*    */   implements Comparator
/*    */ {
/*    */   public int compare(Object o1, Object o2)
/*    */   {
/*  9 */     Point2D p1 = (Point2D)o1; Point2D p2 = (Point2D)o2;
/* 10 */     double x1 = p1.getX(); double y1 = p1.getY(); double x2 = p2.getX(); double y2 = p2.getY();
/* 11 */     return Double.compare(Math.atan2(y1, x1), Math.atan2(y2, x2));
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.AngleCmp
 * JD-Core Version:    0.5.3
 */