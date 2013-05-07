/*     */ package de.tum.in.gagern.ornament;
/*     */ 
/*     */ import de.tum.in.gagern.geom.Vec3R;
/*     */ import java.awt.geom.AffineTransform;
/*     */ import java.awt.geom.GeneralPath;
/*     */ import java.awt.geom.NoninvertibleTransformException;
/*     */ import java.awt.geom.PathIterator;
/*     */ import java.awt.geom.Point2D;
/*     */ import java.awt.geom.Point2D.Double;
/*     */ import java.awt.geom.Point2D.Float;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collection;
/*     */ import java.util.Collections;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ 
/*     */ public class Voronoi
/*     */ {
/*     */   private static final double ALMOST_ZERO = 0.001D;
/*     */   private Group group;
/*     */   private Point2D.Double controlPoint;
/*     */   private int ax;
/*     */   private int ay;
/*     */   private int bx;
/*     */   private int by;
/*     */   private AffineTransform vectorTransform;
/*     */   private List cell;
/*     */   protected GeneralPath grid;
/*     */   private GeneralPath compactGrid;
/*     */ 
/*     */   public Voronoi()
/*     */   {
/*  48 */     this.group = null;
/*  49 */     this.controlPoint = new Point2D.Double(1.0D, 1.0D);
/*  50 */     this.vectorTransform = new AffineTransform();
/*  51 */     this.cell = new ArrayList();
/*  52 */     this.grid = new GeneralPath();
/*  53 */     this.compactGrid = new GeneralPath();
/*     */   }
/*     */ 
/*     */   public Voronoi set(Group group, int ax, int ay, int bx, int by) {
/*  57 */     if ((group != this.group) || (ax != this.ay) || (ay != this.ay) || (bx != this.bx) || (by != this.by))
/*     */     {
/*  59 */       this.group = group;
/*  60 */       this.controlPoint.setLocation(this.controlPoint);
/*  61 */       this.ax = ax;
/*  62 */       this.ay = ay;
/*  63 */       this.bx = bx;
/*  64 */       this.by = by;
/*  65 */       this.vectorTransform.setTransform(ax, ay, bx, by, 0.0D, 0.0D);
/*  66 */       recreate();
/*     */     }
/*  68 */     return this;
/*     */   }
/*     */ 
/*     */   public Point2D getControlPoint() {
/*  72 */     return this.controlPoint;
/*     */   }
/*     */ 
/*     */   public void recreate() {
/*  76 */     if (this.group == null) {
/*  77 */       this.cell.clear();
/*  78 */       this.grid.reset();
/*  79 */       this.compactGrid.reset();
/*  80 */       return;
/*     */     }
/*     */     try {
/*  83 */       if (this.controlPoint.x > 1.0D) this.controlPoint.x = 1.0D;
/*  84 */       if (this.controlPoint.x < 0.0D) this.controlPoint.x = 0.0D;
/*  85 */       if (this.controlPoint.y > 1.0D) this.controlPoint.y = 1.0D;
/*  86 */       if (this.controlPoint.y < 0.0D) this.controlPoint.y = 0.0D;
/*  87 */       createCell();
/*  88 */       createGrid();
/*  89 */       createCompactGrid();
/*     */     }
/*     */     catch (NoninvertibleTransformException e) {
/*  92 */       e.printStackTrace();
/*     */     }
/*     */   }
/*     */ 
/*     */   private void debug(String msg)
/*     */   {
/*     */   }
/*     */ 
/*     */   private void createCell()
/*     */     throws NoninvertibleTransformException
/*     */   {
/* 107 */     MinGrid minGrid = new MinGrid(this.ax, this.ay, this.bx, this.by);
/* 108 */     debug("Minimum grid: " + minGrid);
/*     */ 
/* 111 */     Point2D controlAB = new Point2D.Double();
/* 112 */     Point2D controlXY = new Point2D.Double();
/* 113 */     Point2D image = new Point2D.Double();
/* 114 */     controlAB.setLocation(this.group.sensibleVoronoiPoint(this.controlPoint, this.vectorTransform));
/*     */ 
/* 116 */     double minDist = 1.0E-10D;
/*     */     while (true) {
/* 118 */       floor(controlAB);
/* 119 */       Point2D nearestImage = null;
/* 120 */       for (int i = 1; i < this.group.countTransforms(); ++i) {
/* 121 */         AffineTransform trafo = this.group.getTransform(i);
/* 122 */         trafo.transform(controlAB, image);
/* 123 */         double da = controlAB.getX() - image.getX();
/* 124 */         double db = controlAB.getY() - image.getY();
/* 125 */         da -= Math.round(da);
/* 126 */         db -= Math.round(db);
/* 127 */         double dist = da * da + db * db;
/* 128 */         if (minDist >= dist) {
/* 129 */           minDist = dist;
/* 130 */           nearestImage = new Point2D.Double(image.getX(), image.getY()); }
/*     */       }
/* 132 */       if (nearestImage == null) break;
/* 133 */       debug("control = " + controlAB + ", nearest = " + nearestImage);
/*     */ 
/* 135 */       double da = controlAB.getX() - nearestImage.getX();
/* 136 */       double db = controlAB.getY() - nearestImage.getY();
/* 137 */       double dl = da * da + db * db;
/* 138 */       if (dl == 0.0D) {
/* 139 */         controlAB.setLocation(controlAB.getX() + 0.00123D, controlAB.getY() + 0.00234D);
/*     */       }
/*     */       else {
/* 142 */         controlAB.setLocation(nearestImage.getX() + da / dl * 0.001D, nearestImage.getY() + db / dl * 0.001D);
/*     */       }
/*     */     }
/* 145 */     this.vectorTransform.transform(controlAB, controlXY);
/* 146 */     AffineTransform translate = AffineTransform.getTranslateInstance(controlXY.getX(), controlXY.getY());
/*     */ 
/* 151 */     int da = Math.max(Math.max(Math.abs(minGrid.getA1()), Math.abs(minGrid.getA2())), Math.abs(minGrid.getA1() - minGrid.getA2()));
/*     */ 
/* 153 */     int db = Math.max(Math.max(Math.abs(minGrid.getB1()), Math.abs(minGrid.getB2())), Math.abs(minGrid.getB1() - minGrid.getB2()));
/*     */ 
/* 155 */     debug("Loop bounds: da = " + da + ", db = " + db);
/*     */ 
/* 158 */     List sites = new ArrayList(this.group.countTransforms() * (2 * da + 1) * (2 * db + 1));
/* 159 */     for (int i = 0; i < this.group.countTransforms(); ++i) {
/* 160 */       AffineTransform trafo = this.group.getTransform(i);
/* 161 */       trafo.transform(controlAB, image);
/* 162 */       floor(image);
/* 163 */       for (int ai = -da; ai <= da; ++ai) {
/* 164 */         for (int bi = -db; bi <= db; ++bi) {
/* 165 */           if ((i == 0) && (ai == 0) && (bi == 0))
/*     */             continue;
/* 167 */           Point2D site = new Point2D.Double(image.getX() + ai, image.getY() + bi);
/*     */ 
/* 169 */           this.vectorTransform.transform(site, site);
/* 170 */           translate.inverseTransform(site, site);
/* 171 */           debug("Site: " + site.getX() + ", " + site.getY());
/* 172 */           sites.add(site);
/*     */         }
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 178 */     Collections.sort(sites, new AngleCmp());
/* 179 */     minDist = (1.0D / 0.0D);
/* 180 */     int n = sites.size(); int i0 = -1;
/* 181 */     List v = new ArrayList(6);
/* 182 */     for (int i = 0; i < sites.size(); ++i) {
/* 183 */       double dist = ((Point2D)sites.get(i)).distanceSq(0.0D, 0.0D);
/* 184 */       if (minDist > dist) {
/* 185 */         minDist = dist;
/* 186 */         i0 = i; }
/*     */     }
/* 188 */     int i1 = i0; for (int i2 = -1; i2 != i0; i1 = i2) {
/* 189 */       Point2D p1 = (Point2D)sites.get(i1);
/* 190 */       double p1x = p1.getX(); double p1y = p1.getY();
/* 191 */       v.add(new Vec3R(-2.0D * p1x, -2.0D * p1y, p1x * p1x + p1y * p1y));
/* 192 */       debug("Vertex cell neighbour: " + p1x + ", " + p1y);
/* 193 */       for (i2 = (i1 + 1) % n; i2 != i0; i2 = (i2 + 1) % n) {
/* 194 */         Point2D p2 = (Point2D)sites.get(i2);
/* 195 */         if (!(inCircle(p1, p2, sites))) {
/*     */           break;
/*     */         }
/*     */       }
/*     */     }
/* 200 */     Vec3R e1 = (Vec3R)v.get(v.size() - 1);
/* 201 */     for (int i = 0; i < v.size(); ++i) {
/* 202 */       Vec3R e2 = (Vec3R)v.get(i);
/* 203 */       v.set(i, e1.cross(e2));
/* 204 */       e1 = e2;
/*     */     }
/*     */ 
/* 207 */     this.cell.clear();
/* 208 */     for (int i = 0; i < v.size(); ++i) {
/* 209 */       Point2D vertex = ((Vec3R)v.get(i)).dehomog();
/* 210 */       translate.transform(vertex, vertex);
/* 211 */       this.vectorTransform.inverseTransform(vertex, vertex);
/* 212 */       this.cell.add(vertex);
/*     */     }
/*     */   }
/*     */ 
/*     */   private boolean inCircle(Point2D p1, Point2D p2, Collection sites) {
/* 217 */     double x1 = p1.getX(); double y1 = p1.getY(); double z1 = x1 * x1 + y1 * y1;
/* 218 */     double x2 = p2.getX(); double y2 = p2.getY(); double z2 = x2 * x2 + y2 * y2;
/* 219 */     for (Iterator i = sites.iterator(); i.hasNext(); ) {
/* 220 */       Point2D p3 = (Point2D)i.next();
/* 221 */       if (p3 == p1) continue; if (p3 != p2);
/* 222 */       double x3 = p3.getX(); double y3 = p3.getY(); double z3 = x3 * x3 + y3 * y3;
/* 223 */       double det = x1 * y2 * z3 + x2 * y3 * z1 + x3 * y1 * z2 - (x3 * y2 * z1) - (x2 * y1 * z3) - (x1 * y3 * z2);
/*     */ 
/* 225 */       if (det < -1.E-05D) {
/* 226 */         return true;
/*     */       }
/*     */     }
/* 229 */     return false;
/*     */   }
/*     */ 
/*     */   private Point2D floor(Point2D p) {
/* 233 */     p.setLocation(p.getX() - Math.floor(p.getX()), p.getY() - Math.floor(p.getY()));
/*     */ 
/* 235 */     return p;
/*     */   }
/*     */ 
/*     */   public GeneralPath getGrid()
/*     */   {
/* 245 */     return this.grid;
/*     */   }
/*     */ 
/*     */   public void createGrid() {
/* 249 */     this.grid.reset();
/* 250 */     Point2D.Float tmp = new Point2D.Float();
/* 251 */     for (int t = 0; t < this.group.countTransforms(); ++t) {
/* 252 */       AffineTransform tr = this.group.getTransform(t);
/* 253 */       tr.transform((Point2D)this.cell.get(0), tmp);
/* 254 */       this.grid.moveTo(tmp.x, tmp.y);
/* 255 */       for (int i = 1; i < this.cell.size(); ++i) {
/* 256 */         tr.transform((Point2D)this.cell.get(i), tmp);
/* 257 */         this.grid.lineTo(tmp.x, tmp.y);
/*     */       }
/* 259 */       this.grid.closePath();
/*     */     }
/*     */   }
/*     */ 
/*     */   public GeneralPath getCompactGrid()
/*     */   {
/* 269 */     return this.compactGrid;
/*     */   }
/*     */ 
/*     */   private void createCompactGrid() {
/* 273 */     this.compactGrid.reset();
/* 274 */     PathIterator pi = getGrid().getPathIterator(null);
/* 275 */     float a = 0.0F; float b = 0.0F; float a0 = 0.0F; float b0 = 0.0F;
/* 276 */     float[] coords = new float[6];
/* 277 */     Point2D.Float pos = new Point2D.Float(-1.0F, -1.0F);
/* 278 */     while (!(pi.isDone())) {
/* 279 */       switch (pi.currentSegment(coords))
/*     */       {
/*     */       case 0:
/* 281 */         a0 = a = coords[0];
/* 282 */         b0 = b = coords[1];
/* 283 */         break;
/*     */       case 1:
/* 285 */         addCompactLine(pos, a, b, coords[0], coords[1]);
/* 286 */         a = coords[0];
/* 287 */         b = coords[1];
/* 288 */         break;
/*     */       case 4:
/* 290 */         addCompactLine(pos, a, b, a0, b0);
/* 291 */         a = a0;
/* 292 */         b = b0;
/* 293 */         break;
/*     */       case 2:
/*     */       case 3:
/*     */       default:
/* 295 */         throw new IllegalStateException("non-linear grid path");
/*     */       }
/* 297 */       pi.next();
/*     */     }
/*     */   }
/*     */ 
/*     */   private void addCompactLine(Point2D.Float pos, float a1, float b1, float a2, float b2)
/*     */   {
/* 305 */     float a1i = (float)Math.floor(a1);
/* 306 */     float b1i = (float)Math.floor(b1);
/* 307 */     float a2i = (float)Math.floor(a2);
/* 308 */     float b2i = (float)Math.floor(b2);
/*     */ 
/* 314 */     if ((a1 == a2) && (b1 == b2)) return;
/*     */     float c;
/* 315 */     if ((a1i < a2i) && (((a2i < a2) || (a2i - a1i > 1.5F))))
/*     */     {
/* 317 */       a2i = a1i + 1.0F;
/* 318 */       c = (a2i - a1) * (b2 - b1) / (a2 - a1) + b1;
/* 319 */       addCompactLine(pos, a1, b1, a2i, c);
/* 320 */       addCompactLine(pos, a2i, c, a2, b2);
/* 321 */     } else if ((a2i < a1i) && (((a1i < a1) || (a1i - a2i > 1.5F))))
/*     */     {
/* 323 */       a1i = a2i + 1.0F;
/* 324 */       c = (a1i - a1) * (b2 - b1) / (a2 - a1) + b1;
/* 325 */       addCompactLine(pos, a1, b1, a1i, c);
/* 326 */       addCompactLine(pos, a1i, c, a2, b2);
/* 327 */     } else if ((b1i < b2i) && (((b2i < b2) || (b2i - b1i > 1.5F))))
/*     */     {
/* 329 */       b2i = b1i + 1.0F;
/* 330 */       c = (b2i - b1) * (a2 - a1) / (b2 - b1) + a1;
/* 331 */       addCompactLine(pos, a1, b1, c, b2i);
/* 332 */       addCompactLine(pos, c, b2i, a2, b2);
/* 333 */     } else if ((b2i < b1i) && (((b1i < b1) || (b1i - b2i > 1.5F))))
/*     */     {
/* 335 */       b1i = b2i + 1.0F;
/* 336 */       c = (b1i - b1) * (a2 - a1) / (b2 - b1) + a1;
/* 337 */       addCompactLine(pos, a1, b1, c, b1i);
/* 338 */       addCompactLine(pos, c, b1i, a2, b2);
/*     */     }
/*     */     else {
/* 341 */       a1i = Math.min(a1i, a2i);
/* 342 */       b1i = Math.min(b1i, b2i);
/* 343 */       a1 -= a1i;
/* 344 */       b1 -= b1i;
/* 345 */       a2 -= a1i;
/* 346 */       b2 -= b1i;
/* 347 */       if ((pos.x != a1) || (pos.y != b1)) this.compactGrid.moveTo(a1, b1);
/* 348 */       pos.x = a2; this.compactGrid.lineTo(pos.x, pos.y = b2);
/* 349 */       if ((a1 < 0.001D) && (a2 < 0.001D)) {
/* 350 */         this.compactGrid.moveTo(a1 + 1.0F, b1);
/* 351 */         this.compactGrid.lineTo(++a2, pos.y = b2);
/* 352 */       } else if ((1.0F - a1 < 0.001D) && (1.0F - a2 < 0.001D)) {
/* 353 */         this.compactGrid.moveTo(a1 - 1.0F, b1);
/* 354 */         this.compactGrid.lineTo(--a2, pos.y = b2);
/* 355 */       } else if ((b1 < 0.001D) && (b2 < 0.001D)) {
/* 356 */         this.compactGrid.moveTo(a1, b1 + 1.0F);
/* 357 */         this.compactGrid.lineTo(pos.x = a2, ++b2);
/* 358 */       } else if ((1.0F - b1 < 0.001D) && (1.0F - b2 < 0.001D)) {
/* 359 */         this.compactGrid.moveTo(a1, b1 - 1.0F);
/* 360 */         this.compactGrid.lineTo(pos.x = a2, --b2);
/*     */       }
/*     */     }
/*     */   }
/*     */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.Voronoi
 * JD-Core Version:    0.5.3
 */