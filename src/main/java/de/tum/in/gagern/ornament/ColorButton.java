/*    */ package de.tum.in.gagern.ornament;
/*    */ 
/*    */ import java.awt.Color;
/*    */ import java.awt.Component;
/*    */ import java.awt.Graphics;
/*    */ import java.awt.event.ActionEvent;
/*    */ import java.awt.event.ActionListener;
/*    */ import javax.swing.Icon;
/*    */ import javax.swing.JToggleButton;
/*    */ 
/*    */ public class ColorButton extends JToggleButton
/*    */   implements Icon, ActionListener
/*    */ {
/*    */   Ornament main;
/*    */   int w;
/*    */   int h;
/*    */   Color c;
/*    */ 
/*    */   public ColorButton(Ornament ornament, int width, int height, Color color)
/*    */   {
/* 25 */     if (color == null) throw new NullPointerException();
/* 26 */     this.main = ornament;
/* 27 */     this.w = width;
/* 28 */     this.h = height;
/* 29 */     this.c = color;
/* 30 */     setIcon(this);
/* 31 */     setBackground(color);
/* 32 */     addActionListener(this);
/*    */   }
/*    */ 
/*    */   public void paintIcon(Component comp, Graphics g, int x, int y) {
/* 36 */     Color oc = g.getColor();
/* 37 */     int d = this.h / 16;
/* 38 */     g.setColor(this.c);
/* 39 */     g.fillRect(x + d, y + d, this.w - (2 * d), this.h - (2 * d));
/* 40 */     g.setColor(oc);
/*    */   }
/*    */ 
/*    */   public int getIconWidth() {
/* 44 */     return this.w;
/*    */   }
/*    */ 
/*    */   public int getIconHeight() {
/* 48 */     return this.h;
/*    */   }
/*    */ 
/*    */   public void actionPerformed(ActionEvent e) {
/* 52 */     this.main.setColor(this.c);
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.ColorButton
 * JD-Core Version:    0.5.3
 */