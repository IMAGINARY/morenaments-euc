/*    */ package de.tum.in.gagern.ornament.plaf;
/*    */ 
/*    */ import java.awt.Color;
/*    */ import java.awt.Component;
/*    */ import java.awt.Graphics;
/*    */ import java.awt.Image;
/*    */ import java.awt.MediaTracker;
/*    */ import java.awt.Toolkit;
/*    */ import java.net.URL;
/*    */ import java.util.HashMap;
/*    */ import java.util.Map;
/*    */ 
/*    */ class ImagePainter
/*    */ {
/*    */   private static Toolkit currentToolkit;
/*    */   private static Map cache;
/*    */   private final Image img;
/*    */ 
/*    */   public static ImagePainter getInstance(Component component, String name)
/*    */   {
/* 20 */     Toolkit toolkit = component.getToolkit();
/* 21 */     if (toolkit == null) throw new NullPointerException();
/* 22 */     if (toolkit != currentToolkit) cache = new HashMap(5);
/* 23 */     Object cached = cache.get(name);
/* 24 */     if (cached != null) return ((ImagePainter)cached);
/*    */ 
/* 26 */     URL url = ImagePainter.class.getResource(name);
/* 27 */     if (url == null) return new ImagePainter(null);
/* 28 */     Image img = toolkit.createImage(url);
/* 29 */     ImagePainter newPainter = new ImagePainter(img);
/* 30 */     MediaTracker mt = new MediaTracker(component);
/* 31 */     mt.addImage(img, 0);
/*    */     try {
/* 33 */       mt.waitForAll();
/*    */     }
/*    */     catch (InterruptedException e) {
/* 36 */       e.printStackTrace();
/*    */     }
/* 38 */     cache.put(name, newPainter);
/* 39 */     return newPainter;
/*    */   }
/*    */ 
/*    */   private ImagePainter(Image img)
/*    */   {
/* 45 */     this.img = img;
/*    */   }
/*    */ 
/*    */   public void foreground(Graphics g, Component c, int x, int y, int w, int h)
/*    */   {
/* 50 */     if (this.img == null) return;
/* 51 */     int w1 = this.img.getWidth(c); int h1 = this.img.getHeight(c);
/* 52 */     if ((w1 == -1) || (h1 == -1))
/*    */     {
/* 54 */       return;
/*    */     }
/* 56 */     int w2 = (w1 - 1) / 2; int h2 = (h1 - 1) / 2;
/* 57 */     g.drawImage(this.img, x, y, x + w2, y + h2, 0, 0, w2, h2, c);
/*    */ 
/* 59 */     g.drawImage(this.img, x + w2, y, x + w - w2, y + h2, w2, 0, w1 - w2, h2, c);
/*    */ 
/* 61 */     g.drawImage(this.img, x + w - w2, y, x + w, y + h2, w1 - w2, 0, w1, h2, c);
/*    */ 
/* 63 */     g.drawImage(this.img, x, y + h2, x + w2, y + h - h2, 0, h2, w2, h1 - h2, c);
/*    */ 
/* 65 */     g.drawImage(this.img, x + w2, y + h2, x + w - w2, y + h - h2, w2, h2, w1 - w2, h1 - h2, c);
/*    */ 
/* 67 */     g.drawImage(this.img, x + w - w2, y + h2, x + w, y + h - h2, w1 - w2, h2, w1, h1 - h2, c);
/*    */ 
/* 69 */     g.drawImage(this.img, x, y + h - h2, x + w2, y + h, 0, h1 - h2, w2, h1, c);
/*    */ 
/* 71 */     g.drawImage(this.img, x + w2, y + h - h2, x + w - w2, y + h, w2, h2, w1 - w2, h1, c);
/*    */ 
/* 73 */     g.drawImage(this.img, x + w - w2, y + h - h2, x + w, y + h, w1 - w2, h1 - h2, w1, h1, c);
/*    */   }
/*    */ 
/*    */   public void foreground(Graphics g, Component c)
/*    */   {
/* 78 */     foreground(g, c, 0, 0, c.getWidth(), c.getHeight());
/*    */   }
/*    */ 
/*    */   public void background(Graphics g, Component c, Color bg, int x, int y, int w, int h)
/*    */   {
/* 83 */     if ((bg == null) || (this.img == null)) return;
/* 84 */     int w1 = this.img.getWidth(c); int h1 = this.img.getHeight(c);
/* 85 */     if ((w1 == -1) || (h1 == -1))
/*    */     {
/* 87 */       return;
/*    */     }
/* 89 */     int w2 = (w1 - 1) / 2; int h2 = (h1 - 1) / 2;
/* 90 */     Color old = g.getColor();
/* 91 */     g.setColor(bg);
/* 92 */     g.fillRoundRect(x, y, w, h, w2 + 1, h2 + 1);
/* 93 */     g.setColor(old);
/*    */   }
/*    */ 
/*    */   public void background(Graphics g, Component c) {
/* 97 */     background(g, c, c.getBackground(), 0, 0, c.getWidth(), c.getHeight());
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.plaf.ImagePainter
 * JD-Core Version:    0.5.3
 */