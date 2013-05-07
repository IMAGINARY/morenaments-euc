/*     */ package de.tum.in.gagern.ornament;
/*     */ 
/*     */ import java.awt.BorderLayout;
/*     */ import java.awt.GridBagConstraints;
/*     */ import java.awt.GridBagLayout;
/*     */ import java.awt.Insets;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.InputStreamReader;
/*     */ import java.io.Reader;
/*     */ import java.net.URL;
/*     */ import java.nio.charset.Charset;
/*     */ import java.util.Properties;
/*     */ import javax.swing.ImageIcon;
/*     */ import javax.swing.JLabel;
/*     */ import javax.swing.JPanel;
/*     */ import javax.swing.JScrollPane;
/*     */ import javax.swing.JTabbedPane;
/*     */ import javax.swing.JTextArea;
/*     */ 
/*     */ class AboutDlg extends JTabbedPane
/*     */ {
/*     */   GridBagConstraints gbcLabel;
/*     */   GridBagConstraints gbcValue;
/*     */   JPanel infoPanel;
/*     */ 
/*     */   AboutDlg()
/*     */   {
/*  28 */     Properties prop = Ornament.getVersionInfo();
/*  29 */     JPanel infoTab = new JPanel();
/*  30 */     infoTab.setLayout(new BorderLayout());
/*  31 */     this.infoPanel = new JPanel();
/*  32 */     this.infoPanel.setLayout(new GridBagLayout());
/*  33 */     infoTab.add(this.infoPanel, "Center");
/*  34 */     this.gbcLabel = new GridBagConstraints();
/*  35 */     this.gbcLabel.insets = new Insets(4, 4, 4, 4);
/*  36 */     this.gbcLabel.fill = 1;
/*  37 */     this.gbcValue = ((GridBagConstraints)this.gbcLabel.clone());
/*  38 */     this.gbcValue.gridwidth = 0;
/*  39 */     String unknown = I18n._("about.info.unknown");
/*  40 */     JLabel title = null;
/*  41 */     URL url = AboutDlg.class.getResource("morenaments-euc.png");
/*  42 */     if (url != null) {
/*  43 */       title = new JLabel(new ImageIcon(url));
/*     */     }
/*     */     else {
/*  46 */       title = new JLabel(I18n._("about.info.Ornament"));
/*     */     }
/*  48 */     title.setHorizontalAlignment(0);
/*  49 */     infoTab.add(title, "North");
/*  50 */     String cleanStr = prop.getProperty("clean", null);
/*  51 */     int clean = 2;
/*  52 */     if ("True".equals(cleanStr)) clean = 1;
/*  53 */     if ("False".equals(cleanStr)) clean = 0;
/*  54 */     addPair(I18n._("about.info.Version"), prop.getProperty("version", unknown));
/*     */ 
/*  56 */     addPair(I18n._("about.info.Revision"), I18n._("about.info.revfmt", new Object[] { prop.getProperty("branch-nick", unknown), prop.getProperty("revno", unknown), new Integer(clean) }));
/*     */ 
/*  61 */     addPair(I18n._("about.info.RevID"), prop.getProperty("revision-id", unknown));
/*     */ 
/*  63 */     addPair(I18n._("about.info.Date"), prop.getProperty("date", unknown));
/*     */ 
/*  65 */     addPair(I18n._("about.info.Homepage"), I18n._("homepage"));
/*     */ 
/*  67 */     addPair(I18n._("about.info.Author"), "Martin von Gagern");
/*     */ 
/*  69 */     addPair(I18n._("about.info.License"), I18n._("about.info.GPL2"));
/*     */ 
/*  72 */     addTab(I18n._("about.tab.Info"), infoTab);
/*  73 */     addResourceTab(I18n._("about.tab.Copyleft"), "COPYING");
/*  74 */     addResourceTab(I18n._("about.tab.License"), "LICENSE.txt");
/*     */   }
/*     */ 
/*     */   void addPair(String label, String value) {
/*  78 */     JLabel jLabel = new JLabel(label);
/*  79 */     JLabel jValue = new JLabel(value);
/*  80 */     this.infoPanel.add(jLabel, this.gbcLabel);
/*  81 */     this.infoPanel.add(jValue, this.gbcValue);
/*     */   }
/*     */ 
/*     */   void addResourceTab(String tabName, String resourcePath) {
/*     */     String resourceText;
/*     */     try {
/*  87 */       InputStream in = super.getClass().getResourceAsStream(resourcePath);
/*     */ 
/*  89 */       if (in == null) throw new IOException(resourcePath + " not found");
/*  90 */       Reader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
/*     */ 
/*  92 */       StringBuffer sbuf = new StringBuffer();
/*  93 */       char[] cbuf = new char[1024];
/*     */       while (true) {
/*  95 */         int nread = reader.read(cbuf);
/*  96 */         if (nread == -1) break;
/*  97 */         sbuf.append(cbuf, 0, nread);
/*     */       }
/*  99 */       resourceText = sbuf.toString();
/*     */     }
/*     */     catch (IOException e) {
/* 102 */       resourceText = e.toString();
/*     */     }
/* 104 */     JTextArea area = new JTextArea(resourceText, 20, 60);
/* 105 */     area.setEditable(false);
/* 106 */     addTab(tabName, new JScrollPane(area));
/*     */   }
/*     */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.AboutDlg
 * JD-Core Version:    0.5.3
 */