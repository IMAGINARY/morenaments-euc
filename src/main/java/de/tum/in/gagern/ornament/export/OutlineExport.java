/*     */ package de.tum.in.gagern.ornament.export;
/*     */ 
/*     */ import de.tum.in.gagern.ornament.Group;
/*     */ import de.tum.in.gagern.ornament.LinPath;
/*     */ import de.tum.in.gagern.ornament.Ornament;
/*     */ import java.awt.BasicStroke;
/*     */ import java.awt.Color;
/*     */ import java.awt.Shape;
/*     */ import java.awt.geom.AffineTransform;
/*     */ import java.awt.geom.Area;
/*     */ import java.awt.geom.GeneralPath;
/*     */ import java.awt.geom.NoninvertibleTransformException;
/*     */ import java.awt.geom.Rectangle2D;
/*     */ import java.awt.geom.Rectangle2D.Double;
/*     */ import java.io.IOException;
/*     */ 
/*     */ public abstract class OutlineExport extends Export
/*     */ {
/*     */   private Area area;
/*     */   private AffineTransform[] transforms;
/*     */   private AffineTransform inverseTransform;
/*     */   private static final double clipSlack = 0.125D;
/*  65 */   private static final Area clipRect = new Area(new Rectangle2D.Double(-0.125D, -0.125D, 1.25D, 1.25D));
/*     */ 
/*     */   protected OutlineExport(Ornament ornament, String id)
/*     */   {
/*  42 */     super(ornament, id);
/*     */   }
/*     */ 
/*     */   protected void init() throws NoninvertibleTransformException {
/*  46 */     super.init();
/*     */ 
/*  48 */     this.area = new Area();
/*  49 */     int[] iVec = new int[4];
/*  50 */     this.ornament.getVectors(iVec);
/*  51 */     double[] dVec = new double[6];
/*  52 */     for (int i = 0; i != 4; ++i) dVec[i] = iVec[i];
/*  53 */     AffineTransform aVec = new AffineTransform(dVec);
/*  54 */     this.inverseTransform = aVec.createInverse();
/*     */ 
/*  56 */     Group g = this.ornament.getGroup();
/*  57 */     this.transforms = new AffineTransform[g.countTransforms()];
/*  58 */     for (int i = 0; i < this.transforms.length; ++i) {
/*  59 */       this.transforms[i] = new AffineTransform(aVec);
/*  60 */       this.transforms[i].concatenate(g.getTransform(i));
/*     */     }
/*     */   }
/*     */ 
/*     */   private void prepareArea()
/*     */     throws IOException
/*     */   {
/*  70 */     if (this.area.isEmpty()) return;
/*  71 */     this.area.intersect(clipRect);
/*  72 */     area(this.area, this.color);
/*  73 */     this.area.reset();
/*     */   }
/*     */ 
/*     */   protected void postBody() throws IOException {
/*  77 */     prepareArea();
/*  78 */     super.postBody();
/*     */   }
/*     */ 
/*     */   protected void color(Color c) throws IOException {
/*  82 */     prepareArea();
/*  83 */     super.color(c);
/*     */   }
/*     */ 
/*     */   protected abstract void area(Area paramArea, Color paramColor) throws IOException;
/*     */ 
/*     */   protected void path(LinPath l) throws IOException, NoninvertibleTransformException
/*     */   {
/*  90 */     super.path(l);
/*  91 */     GeneralPath gp1 = asGeneralPath(l.getShape());
/*  92 */     for (int i = 0; i != this.transforms.length; ++i) {
/*  93 */       Shape s = gp1.createTransformedShape(this.transforms[i]);
/*  94 */       GeneralPath gp2 = asGeneralPath(this.stroke.createStrokedShape(s));
/*  95 */       gp2.transform(this.inverseTransform);
/*  96 */       Area a = new Area(gp2);
/*  97 */       Rectangle2D bounds = a.getBounds2D();
/*  98 */       int x1 = (int)Math.floor(bounds.getMinX());
/*  99 */       int y1 = (int)Math.floor(bounds.getMinY());
/* 100 */       int x2 = (int)Math.ceil(bounds.getMaxX());
/* 101 */       int y2 = (int)Math.ceil(bounds.getMaxY());
/* 102 */       AffineTransform t = new AffineTransform();
/* 103 */       for (int x = x1; x < x2; ++x) for (int y = y1; y < y2; ++y) {
/* 104 */           t.setToTranslation(-x, -y);
/* 105 */           this.area.add(a.createTransformedArea(t));
/*     */         }
/*     */     }
/*     */   }
/*     */ 
/*     */   private GeneralPath asGeneralPath(Shape s) {
/* 111 */     if (s instanceof GeneralPath) return ((GeneralPath)s);
/* 112 */     return new GeneralPath(s);
/*     */   }
/*     */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.export.OutlineExport
 * JD-Core Version:    0.5.3
 */