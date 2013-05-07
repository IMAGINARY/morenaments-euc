/*    */ package de.tum.in.gagern.ornament;
/*    */ 
/*    */ import java.text.MessageFormat;
/*    */ import java.util.MissingResourceException;
/*    */ import java.util.ResourceBundle;
/*    */ 
/*    */ public class I18n
/*    */ {
/* 19 */   private static ResourceBundle res = null;
/*    */ 
/*    */   private I18n()
/*    */   {
/* 16 */     throw new Error("Don't instantiate!");
/*    */   }
/*    */ 
/*    */   public static String _(String key)
/*    */   {
/*    */     try
/*    */     {
/* 23 */       if (res == null) load();
/* 24 */       return res.getString(key.replace(' ', '_')); } catch (MissingResourceException e) {
/*    */     }
/* 26 */     return key.substring(key.lastIndexOf(46) + 1);
/*    */   }
/*    */ 
/*    */   public static String _(String key, Object[] args)
/*    */   {
/* 31 */     return MessageFormat.format(_(key), args);
/*    */   }
/*    */ 
/*    */   public static String dots(String name) {
/* 35 */     return _("menu.dots", new Object[] { name });
/*    */   }
/*    */ 
/*    */   private static synchronized void load() throws MissingResourceException {
/* 39 */     if (res != null) return;
/* 40 */     res = ResourceBundle.getBundle(Ornament.class.getName());
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.I18n
 * JD-Core Version:    0.5.3
 */