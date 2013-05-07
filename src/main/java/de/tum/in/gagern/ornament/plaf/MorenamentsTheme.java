/*    */ package de.tum.in.gagern.ornament.plaf;
/*    */ 
/*    */ import javax.swing.BorderFactory;
/*    */ import javax.swing.UIDefaults;
/*    */ import javax.swing.UIManager;
/*    */ import javax.swing.UnsupportedLookAndFeelException;
/*    */ import javax.swing.border.Border;
/*    */ import javax.swing.plaf.ColorUIResource;
/*    */ import javax.swing.plaf.metal.DefaultMetalTheme;
/*    */ import javax.swing.plaf.metal.MetalLookAndFeel;
/*    */ 
/*    */ public class MorenamentsTheme extends DefaultMetalTheme
/*    */ {
/* 14 */   private static final int[] COLORS = { 0 };
/*    */   private final ColorUIResource[] colors;
/*    */ 
/*    */   public MorenamentsTheme()
/*    */   {
/* 22 */     this.colors = new ColorUIResource[COLORS.length];
/* 23 */     for (int i = 0; i < COLORS.length; ++i)
/* 24 */       this.colors[i] = new ColorUIResource(COLORS[i]);
/*    */   }
/*    */ 
/*    */   public static void activate() {
/*    */     try {
/* 29 */       MetalLookAndFeel.setCurrentTheme(new MorenamentsTheme());
/* 30 */       UIManager.setLookAndFeel(new MetalLookAndFeel());
/*    */     }
/*    */     catch (UnsupportedLookAndFeelException e)
/*    */     {
/* 34 */       e.printStackTrace();
/*    */     }
/*    */   }
/*    */ 
/*    */   public String getName()
/*    */   {
/* 40 */     return "morenaments";
/*    */   }
/*    */ 
/*    */   public void addCustomEntriesToTable(UIDefaults table) {
/* 44 */     super.addCustomEntriesToTable(table);
/* 45 */     Border buttonBorder = BorderFactory.createEmptyBorder(4, 4, 4, 4);
/* 46 */     table.put("ButtonUI", MorenamentsButtonUI.class.getName());
/* 47 */     table.put("Button.border", buttonBorder);
/* 48 */     table.put("ToggleButtonUI", MorenamentsToggleButtonUI.class.getName());
/* 49 */     table.put("ToggleButton.border", buttonBorder);
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.plaf.MorenamentsTheme
 * JD-Core Version:    0.5.3
 */