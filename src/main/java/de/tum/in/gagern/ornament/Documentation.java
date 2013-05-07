/*    */ package de.tum.in.gagern.ornament;
/*    */ 
/*    */ import java.awt.Component;
/*    */ import java.awt.Dialog;
/*    */ import java.awt.Frame;
/*    */ import java.awt.GraphicsConfiguration;
/*    */ import java.awt.Point;
/*    */ import java.awt.Rectangle;
/*    */ import java.io.IOException;
/*    */ import java.io.PrintStream;
/*    */ import java.net.URL;
/*    */ import javax.swing.JDialog;
/*    */ import javax.swing.JEditorPane;
/*    */ import javax.swing.JScrollPane;
/*    */ import javax.swing.JViewport;
/*    */ import javax.swing.event.HyperlinkEvent;
/*    */ import javax.swing.event.HyperlinkEvent.EventType;
/*    */ import javax.swing.event.HyperlinkListener;
/*    */ 
/*    */ class Documentation
/*    */ {
/*    */   JDialog dlg;
/*    */   JEditorPane hp;
/*    */   JScrollPane sp;
/*    */   URL url;
/*    */ 
/*    */   public void show()
/*    */   {
/* 23 */     if (this.hp == null)
/* 24 */       System.err.println("Previous loading of documentation failed");
/*    */     else
/*    */       try {
/* 27 */         this.hp.setPage(this.url);
/* 28 */         this.sp.getViewport().setViewPosition(new Point());
/* 29 */         this.dlg.setVisible(true);
/*    */       } catch (IOException e) {
/* 31 */         System.err.println("Could not load documentation: " + e);
/*    */       }
/*    */   }
/*    */ 
/*    */   Documentation(Component owner, String path)
/*    */   {
/* 37 */     this.url = super.getClass().getClassLoader().getResource(path);
/* 38 */     if (this.url == null) {
/* 39 */       System.err.println("Could not find documentation");
/*    */     } else {
/* 41 */       for (Component c = owner; (this.dlg == null) && (c != null); c = c.getParent()) {
/* 42 */         if (c instanceof Frame) this.dlg = new JDialog((Frame)c);
/* 43 */         if (!(c instanceof Dialog)) continue; this.dlg = new JDialog((Dialog)c);
/*    */       }
/* 45 */       if (this.dlg == null) this.dlg = new JDialog();
/* 46 */       this.dlg.setTitle("Ornament Help");
/*    */       try {
/* 48 */         this.hp = new JEditorPane(this.url);
/* 49 */         this.hp.setEditable(false);
/* 50 */         this.hp.addHyperlinkListener(new Hyperlinker());
/* 51 */         this.sp = new JScrollPane(this.hp);
/* 52 */         this.dlg.setContentPane(this.sp);
/* 53 */         int h = owner.getGraphicsConfiguration().getBounds().height;
/* 54 */         this.dlg.setSize(500, Math.max(400, h * 2 / 3));
/* 55 */         this.dlg.setVisible(true);
/*    */       } catch (IOException e) {
/* 57 */         System.err.println("Could not load documentation: " + e);
/*    */       }
/*    */     }
/*    */   }
/*    */ 
/*    */   static class Hyperlinker implements HyperlinkListener {
/*    */     public void hyperlinkUpdate(HyperlinkEvent evnt) {
/* 64 */       if (evnt.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
/* 65 */         return;
/*    */       try
/*    */       {
/* 68 */         Object o = evnt.getSource();
/* 69 */         if (o instanceof JEditorPane) {
/* 70 */           ((JEditorPane)o).setPage(evnt.getURL());
/*    */         }
/*    */         else
/* 73 */           System.err.println("Not an JEditorPane: " + o);
/*    */       }
/*    */       catch (IOException e) {
/* 76 */         System.err.println("Could not load " + evnt.getURL() + ": " + e);
/*    */       }
/*    */     }
/*    */   }
/*    */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.Documentation
 * JD-Core Version:    0.5.3
 */