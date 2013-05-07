/*    */ package de.tum.in.gagern.ornament.export;
/*    */ 
/*    */ import de.tum.in.gagern.ornament.I18n;
/*    */ import de.tum.in.gagern.ornament.Ornament;
/*    */ import java.awt.geom.NoninvertibleTransformException;
/*    */ import java.io.File;
/*    */ import java.io.FileOutputStream;
/*    */ import java.io.IOException;
/*    */ import javax.swing.JFileChooser;
/*    */ import javax.swing.JOptionPane;
/*    */ import javax.swing.filechooser.FileFilter;
/*    */ 
/*    */ public class FileExportMechanism extends ExportMechanism
/*    */ {
/*    */   private JFileChooser fdlg;
/*    */ 
/*    */   public FileExportMechanism(Ornament main)
/*    */   {
/* 38 */     super(main);
/*    */   }
/*    */ 
/*    */   public boolean init() {
/* 42 */     if (!(super.init())) return false;
/* 43 */     this.fdlg = new JFileChooser();
/* 44 */     this.fdlg.setAcceptAllFileFilterUsed(false);
/* 45 */     for (int i = 0; i < this.exports.length; ++i)
/* 46 */       this.fdlg.addChoosableFileFilter(this.exports[i]);
/* 47 */     this.fdlg.setFileFilter(this.exports[0]);
/* 48 */     return true;
/*    */   }
/*    */ 
/*    */   public void export() {
/* 52 */     if (this.fdlg.showDialog(this.main, I18n._("Export")) != 0)
/* 53 */       return;
/* 54 */     File file = this.fdlg.getSelectedFile();
/* 55 */     if (file == null) {
/* 56 */       JOptionPane.showMessageDialog(this.main, I18n._("No file name given"), I18n._("Export"), 2);
/*    */ 
/* 59 */       return;
/*    */     }
/* 61 */     FileFilter filter = this.fdlg.getFileFilter();
/* 62 */     if (filter instanceof Export) {
/*    */       try {
/* 64 */         exportToFile((Export)filter, file);
/*    */       }
/*    */       catch (IOException ex) {
/* 67 */         JOptionPane.showMessageDialog(this.main, ex.getLocalizedMessage(), I18n._("Export"), 0);
/*    */       }
/*    */       catch (NoninvertibleTransformException ex)
/*    */       {
/* 72 */         JOptionPane.showMessageDialog(this.main, ex.getLocalizedMessage(), I18n._("Export"), 0);
/*    */       }
/*    */ 
/*    */     }
/*    */     else
/*    */     {
/* 78 */       JOptionPane.showMessageDialog(this.main, I18n._("Invalid file type"), I18n._("Export"), 2);
/*    */     }
/*    */   }
/*    */ 
/*    */   private void exportToFile(Export export, File file)
/*    */     throws IOException, NoninvertibleTransformException
/*    */   {
/* 86 */     file = export.fixFile(file);
/* 87 */     this.fdlg.setSelectedFile(file);
/* 88 */     if (file.exists()) {
/* 89 */       Object[] args = { file.getAbsolutePath(), file.getName() };
/* 90 */       if (JOptionPane.showConfirmDialog(this.main, I18n._("export.overwrite", args), I18n._("Export"), 0) != 0)
/*    */       {
/* 93 */         return; }
/*    */     }
/* 95 */     if (!(export.configure())) return;
/* 96 */     FileOutputStream os = new FileOutputStream(file);
/* 97 */     export.export(os);
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.export.FileExportMechanism
 * JD-Core Version:    0.5.3
 */