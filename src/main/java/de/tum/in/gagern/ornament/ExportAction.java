/*    */ package de.tum.in.gagern.ornament;
/*    */ 
/*    */ import de.tum.in.gagern.ornament.export.ExportMechanism;
/*    */ import de.tum.in.gagern.ornament.export.FileExportMechanism;
/*    */ //import de.tum.in.gagern.ornament.export.MailExportMechanism;
/*    */ import de.tum.in.gagern.ornament.export.ServiceExportMechanism;
/*    */ import java.awt.event.ActionEvent;
/*    */ import java.security.AccessControlException;
/*    */ import javax.swing.AbstractAction;
/*    */ import javax.swing.JOptionPane;
/*    */ 
/*    */ class ExportAction extends AbstractAction
/*    */ {
/*    */   private Ornament main;
/*    */   private ExportMechanism fileExportMechanism;
/*    */   private ExportMechanism serviceExportMechanism;
/*    */   private ExportMechanism mailExportMechanism;
/*    */ 
/*    */   public ExportAction(Ornament main)
/*    */   {
/* 18 */     super(I18n.dots(I18n._("Export")));
/* 19 */     this.main = main;
/*    */   }
/*    */ 
/*    */   public void actionPerformed(ActionEvent evnt) {
/*    */     try {
/* 24 */       export();
/*    */     }
/*    */     catch (Exception e) {
/* 27 */       e.printStackTrace();
/* 28 */       JOptionPane.showMessageDialog(this.main, e.toString(), I18n._("Export"), 0);
/*    */     }
/*    */   }
/*    */ 
/*    */   private void export()
/*    */   {
/*    */     String param;
/*    */     try
/*    */     {
/* 39 */       param = this.main.getParameter("ornament.export.mechanism");
/*    */     }
/*    */     catch (Exception e) {
/* 42 */       param = null;
/*    */     }
/* 44 *///     if ("mail".equals(param)) {
/* 45 *///      if (this.mailExportMechanism == null) {
/* 46 *///         MailExportMechanism exp = new MailExportMechanism(this.main);
/* 47 *///         if (exp.init()) this.mailExportMechanism = exp;
/*    *///       }
/* 49 *///       if (this.mailExportMechanism != null) {
/* 50 *///         this.mailExportMechanism.export();
/*    *///       }
/* 52 *///       return;
/*    *///     }
/*    */     try {
/* 55 */       if (this.fileExportMechanism == null) {
/* 56 */         ExportMechanism exp = new FileExportMechanism(this.main);
/* 57 */         if (exp.init()) this.fileExportMechanism = exp;
/*    */       }
/* 59 */       if (this.fileExportMechanism != null)
/* 60 */         this.fileExportMechanism.export();
/*    */     }
/*    */     catch (AccessControlException e1)
/*    */     {
/*    */       try {
/* 65 */         if (this.serviceExportMechanism == null) {
/* 66 */           ExportMechanism exp = new ServiceExportMechanism(this.main, e1);
/*    */ 
/* 68 */           if (exp.init()) this.serviceExportMechanism = exp;
/*    */         }
/* 70 */         if (this.serviceExportMechanism != null)
/* 71 */           this.serviceExportMechanism.export();
/*    */       }
/*    */       catch (LinkageError e2)
/*    */       {
/* 75 */         Object[] args = { e1.getLocalizedMessage(), e2.getLocalizedMessage(), e1.toString(), e2.toString() };
/*    */ 
/* 81 */         JOptionPane.showMessageDialog(this.main, I18n._("Access denied", args), I18n._("Export"), 0);
/*    */       }
/*    */     }
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.ExportAction
 * JD-Core Version:    0.5.3
 */