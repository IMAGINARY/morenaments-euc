/*    */ package de.tum.in.gagern.ornament.plaf;
/*    */ 
/*    */ import java.awt.Graphics;
/*    */ import javax.swing.AbstractButton;
/*    */ import javax.swing.ButtonModel;
/*    */ import javax.swing.JComponent;
/*    */ import javax.swing.plaf.ComponentUI;
/*    */ import javax.swing.plaf.metal.MetalToggleButtonUI;
/*    */ 
/*    */ public class MorenamentsToggleButtonUI extends MetalToggleButtonUI
/*    */ {
/* 12 */   private static final MorenamentsToggleButtonUI instance = new MorenamentsToggleButtonUI();
/*    */ 
/*    */   public static ComponentUI createUI(JComponent b)
/*    */   {
/* 16 */     return instance;
/*    */   }
/*    */ 
/*    */   public void update(Graphics g, JComponent c) {
/* 20 */     ButtonModel model = ((AbstractButton)c).getModel();
/* 21 */     String img = "fgButton.png";
/* 22 */     if (((model.isArmed()) && (model.isPressed())) || (model.isSelected()))
/* 23 */       img = "fgToggled.png";
/* 24 */     ImagePainter ip = ImagePainter.getInstance(c, img);
/* 25 */     ip.background(g, c);
/* 26 */     super.paint(g, c);
/* 27 */     ip.foreground(g, c);
/*    */   }
/*    */ 
/*    */   protected void paintButtonPressed(Graphics g, AbstractButton b)
/*    */   {
/*    */   }
/*    */ 
/*    */   public void installDefaults(AbstractButton b) {
/* 35 */     super.installDefaults(b);
/* 36 */     b.setOpaque(true);
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.plaf.MorenamentsToggleButtonUI
 * JD-Core Version:    0.5.3
 */