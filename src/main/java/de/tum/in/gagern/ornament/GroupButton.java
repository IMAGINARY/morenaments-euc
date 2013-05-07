/*     */ package de.tum.in.gagern.ornament;
/*     */ 
/*     */ import java.awt.Color;
/*     */ import java.awt.Component;
/*     */ import java.awt.Graphics;
/*     */ import java.awt.Graphics2D;
/*     */ import java.awt.RenderingHints;
/*     */ import java.awt.event.ActionEvent;
/*     */ import java.awt.event.ActionListener;
/*     */ import java.awt.geom.AffineTransform;
/*     */ import java.awt.geom.GeneralPath;
/*     */ import javax.swing.Icon;
/*     */ import javax.swing.JToggleButton;
/*     */ 
/*     */ class GroupButton extends JToggleButton
/*     */   implements ActionListener, Icon
/*     */ {
/*  25 */   private static final GeneralPath iconShape = new GeneralPath(1, 5);
/*     */   Ornament main;
/*     */   Group group;
/*     */ 
/*     */   GroupButton(Ornament ornament, Group g)
/*     */   {
/*  36 */     super("");//g.getName());
			super.setToolTipText(g.getName());
/*  37 */     this.main = ornament;
/*  38 */     this.group = g;
/*  39 */     setIcon(this);
/*  40 */     addActionListener(this);
/*     */   }
/*     */ 
/*     */   public void actionPerformed(ActionEvent e) {
/*  44 */     this.main.setGroup(this.group);
/*     */   }
/*     */ 
/*     */   public Group getSymmetryGroup() {
/*  48 */     return this.group;
/*     */   }
/*     */ 
/*     */   public void setName(int namingSystem) {
/*  52 */     //setText(this.group.getName(namingSystem));
/*     */   }
/*     */ 
/*     */   public int getIconWidth() {
/*  56 */     return 28;
/*     */   }
/*     */ 
/*     */   public int getIconHeight() {
/*  60 */     return 28;
/*     */   }
/*     */ 
/*     */   public void paintIcon(Component c, Graphics g, int x, int y) {
/*  64 */     g = g.create(x, y, getIconWidth(), getIconHeight());
/*     */     Graphics2D g2d;
/*  66 */     if (g instanceof Graphics2D) {
/*  67 */       g2d = (Graphics2D)g;
/*     */     }
/*     */     else
/*     */     {
/*  71 */       g.dispose();
/*  72 */       return;
/*     */     }
/*  74 */     g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
/*     */ 
/*  76 */     g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
/*     */ 
/*  78 */     g2d.setColor(Color.RED);
/*     */     AffineTransform tc;
/*  80 */     if (this.group.getTileType() == 4)
/*  81 */       tc = new AffineTransform(16.0F, 0.0F, 8.0F, 14.0F, 4.0F, 4.0F);
/*     */     else
/*  83 */       tc = new AffineTransform(16.0F, 0.0F, 0.0F, 16.0F, 4.0F, 4.0F);
/*  84 */     AffineTransform tr = new AffineTransform();
/*  85 */     for (int t = 0; t < this.group.countTransforms(); ++t) {
/*  86 */       AffineTransform tg = this.group.getTransform(t);
/*  87 */       for (int dx = -3; dx <= 3; ++dx) {
/*  88 */         for (int dy = -3; dy <= 3; ++dy) {
/*  89 */           tr.setToTranslation(dx, dy);
/*  90 */           tr.concatenate(tg);
/*  91 */           tr.preConcatenate(tc);
/*  92 */           double sx = tr.getScaleX(); double sy = tr.getShearY();
/*  93 */           double tx = tr.getTranslateX(); double ty = tr.getTranslateY();
/*  94 */           double sg = (tr.getDeterminant() > 0.0D) ? 1.0D : -1.0D;
/*  95 */           tr.setTransform(sx, sy, -sg * sy, sg * sx, tx, ty);
/*  96 */           g2d.draw(iconShape.createTransformedShape(tr));
/*     */         }
/*     */       }
/*     */     }
/* 100 */     g.dispose();
/*     */   }
/*     */ 
/*     */   static
/*     */   {
/*  26 */     iconShape.moveTo(0.05F, 0.05F);
/*  27 */     iconShape.lineTo(0.18F, 0.27F);
/*  28 */     iconShape.curveTo(0.22F, 0.35F, 0.21F, 0.4F, 0.16F, 0.42F);
/*  29 */     iconShape.curveTo(0.12F, 0.44F, 0.06F, 0.4F, 0.02F, 0.37F);
/*     */   }
/*     */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.GroupButton
 * JD-Core Version:    0.5.3
 */