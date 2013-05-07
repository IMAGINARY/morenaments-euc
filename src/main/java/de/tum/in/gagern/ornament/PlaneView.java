/*    */ package de.tum.in.gagern.ornament;
/*    */ 
/*    */ import java.awt.Dimension;
/*    */ import java.awt.Graphics;
/*    */ import java.awt.Image;
/*    */ import java.awt.event.ComponentAdapter;
/*    */ import java.awt.event.ComponentEvent;
/*    */ import java.awt.event.MouseEvent;
/*    */ import javax.swing.JPanel;
/*    */ import javax.swing.event.MouseInputListener;
/*    */ 
/*    */ class PlaneView extends JPanel
/*    */   implements MouseInputListener
/*    */ {
/*    */   private Ornament main;
/*    */   private int mina;
/*    */   private int maxa;
/*    */   private int minb;
/*    */   private int maxb;
/*    */   private boolean painting;
/*    */   private float[] buf;
/*    */   private int bufpos;
/*    */ 
/*    */   public PlaneView(Ornament ornament)
/*    */   {
/* 26 */     this.main = ornament;
/* 27 */     this.buf = new float[512];
/* 28 */     addMouseListener(this);
/* 29 */     addMouseMotionListener(this);
/* 30 */     setPreferredSize(new Dimension(600, 600));
/* 31 */     setDoubleBuffered(false);
/* 32 */     addComponentListener(new ComponentAdapter() {
/*    */       public void componentResized(ComponentEvent e) {
/* 34 */         PlaneView.this.recalcTiling();
/*    */       }
/*    */     });
/*    */   }
/*    */ 
/*    */   void recalcTiling() {
/* 40 */     Dimension size = getSize();
/* 41 */     if (size == null) return;
/* 42 */     this.mina = (this.maxa = this.minb = this.maxb = 0);
/* 43 */     expandTilingToPoint(size.width, 0);
/* 44 */     expandTilingToPoint(size.width, size.height);
/* 45 */     expandTilingToPoint(0, size.height);
/* 46 */     repaint();
/*    */   }
/*    */ 
/*    */   private void expandTilingToPoint(int x, int y)
/*    */   {
/* 51 */     double a = this.main.aFromPoint(x, y);
/* 52 */     double b = this.main.bFromPoint(x, y);
/* 53 */     if (a < this.mina) this.mina = (int)Math.floor(a);
/* 54 */     if (a > this.maxa) this.maxa = (int)Math.ceil(a);
/* 55 */     if (b < this.minb) this.minb = (int)Math.floor(b);
/* 56 */     if (b <= this.maxb) return; this.maxb = (int)Math.ceil(b);
/*    */   }
/*    */ 
/*    */   public void paint(Graphics g) {
/* 60 */     Image buf = this.main.getBuffer();
/* 61 */     for (int a = this.mina; a <= this.maxa; ++a) for (int b = this.minb; b <= this.maxb; ++b) {
/* 62 */         int x = this.main.ax * a + this.main.bx * b; int y = this.main.ay * a + this.main.by * b;
/* 63 */         if (g.hitClip(x, y, this.main.width, this.main.height))
/* 64 */           g.drawImage(buf, x, y, this);  }
/*    */   }
/*    */ 
/*    */   public void mouseClicked(MouseEvent evnt) {
/*    */   }
/*    */ 
/*    */   public void mousePressed(MouseEvent evnt) {
/* 70 */     this.painting = true;
/* 71 */     this.buf[0] = (float)this.main.aFromPoint(evnt.getX(), evnt.getY());
/* 72 */     this.buf[1] = (float)this.main.bFromPoint(evnt.getX(), evnt.getY());
/* 73 */     this.bufpos = 2;
/* 74 */     this.main.drawLine(this.buf[0], this.buf[1], this.buf[0], this.buf[1]); }
/*    */ 
/*    */   public void mouseReleased(MouseEvent evnt) {
/* 77 */     if (this.bufpos == 2) {
/* 78 */       this.buf[2] = this.buf[0];
/* 79 */       this.buf[3] = this.buf[1];
/* 80 */       this.bufpos += 2;
/*    */     }
/* 82 */     float[] coords = new float[this.bufpos];
/* 83 */     System.arraycopy(this.buf, 0, coords, 0, this.bufpos);
/* 84 */     this.main.addLine(coords);
/* 85 */     this.painting = false; }
/*    */ 
/*    */   public void mouseEntered(MouseEvent evnt) { }
/*    */ 
/*    */   public void mouseExited(MouseEvent evnt) {  }
/*    */ 
/*    */   public void mouseDragged(MouseEvent evnt) { if (!(this.painting)) return;
/* 91 */     if (this.bufpos == this.buf.length) {
/* 92 */       float[] coords = new float[this.bufpos * 2];
/* 93 */       System.arraycopy(this.buf, 0, coords, 0, this.bufpos);
/* 94 */       this.buf = coords;
/*    */     }
/* 96 */     this.buf[this.bufpos] = (float)this.main.aFromPoint(evnt.getX(), evnt.getY());
/* 97 */     this.buf[(this.bufpos + 1)] = (float)this.main.bFromPoint(evnt.getX(), evnt.getY());
/* 98 */     this.main.drawLine(this.buf[(this.bufpos - 2)], this.buf[(this.bufpos - 1)], this.buf[this.bufpos], this.buf[(this.bufpos + 1)]);
/* 99 */     this.bufpos += 2;
/*    */   }
/*    */ 
/*    */   public void mouseMoved(MouseEvent evnt)
/*    */   {
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.PlaneView
 * JD-Core Version:    0.5.3
 */