/*    */ package de.tum.in.gagern.ornament.export;
/*    */ 
/*    */ import de.tum.in.gagern.ornament.I18n;
/*    */ import de.tum.in.gagern.ornament.Ornament;
/*    */ 
/*    */ public abstract class ExportMechanism
/*    */ {
/*    */   protected Ornament main;
/*    */   protected Export[] exports;
/*    */ 
/*    */   protected ExportMechanism(Ornament main)
/*    */   {
/* 32 */     this.main = main;
/*    */   }
/*    */ 
/*    */   public boolean init() {
/* 36 */     this.exports = new Export[] { /*new PdfExport(this.main), */new PostScriptExport(this.main, 1), new PostScriptExport(this.main, 2), new PostScriptExport(this.main, 3), new SvgExport(this.main, 0), new SvgExport(this.main, 1), new BitmapExport(this.main, "png", true, I18n._("export.descr.png")), new BitmapExport(this.main, "jpeg", new String[] { "jpg", "jpeg" }, false, I18n._("export.descr.jpeg")) };
/*    */ 
/* 47 */     return true;
/*    */   }
/*    */ 
/*    */   public abstract void export();
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.export.ExportMechanism
 * JD-Core Version:    0.5.3
 */