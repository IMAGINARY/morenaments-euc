/*     */ package de.tum.in.gagern.ornament;
/*     */ 
/*     */ import java.awt.Component;
/*     */ import java.awt.Dialog;
/*     */ import java.awt.Dimension;
/*     */ import java.awt.FlowLayout;
/*     */ import java.awt.Font;
/*     */ import java.awt.Frame;
/*     */ import java.awt.event.ActionEvent;
/*     */ import java.awt.event.ActionListener;
/*     */ import javax.swing.BoxLayout;
/*     */ import javax.swing.JButton;
/*     */ import javax.swing.JDialog;
/*     */ import javax.swing.JLabel;
/*     */ import javax.swing.JOptionPane;
/*     */ import javax.swing.JPanel;
/*     */ import javax.swing.JTextField;
/*     */ 
/*     */ public class Keyboard extends JPanel
/*     */   implements ActionListener
/*     */ {
/*     */   static final int SPEC_BUTTON_X = 130;
/*     */   static final int BUTTON_X = 53;
/*     */   static final int BUTTON_Y = 45;
/*     */   static final int NO_ENTER = 0;
/*     */   static final int INSERT_ENTER = 1;
/*     */   static final int INSERT_DELETE = 2;
/*     */   JDialog dlg;
/*     */   JTextField textField;
/*  30 */   JButton[] numberButton = new JButton[10];
/*  31 */   String[] numbers = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
/*  32 */   JButton[] firstLetterRowButton = new JButton[12];
/*  33 */   String[] firstLetterRow = { "Q", "W", "E", "R", "T", "Z", "U", "I", "O", "P", "+", "@" };
/*  34 */   JButton[] secondLetterRowButton = new JButton[9];
/*  35 */   String[] secondLetterRow = { "A", "S", "D", "F", "G", "H", "J", "K", "L" };
/*  36 */   JButton[] thirdLetterRowButton = new JButton[10];
/*  37 */   String[] thirdLetterRow = { "Y", "X", "C", "V", "B", "N", "M", ".", "-", "_" };
/*     */   JButton enter;
/*     */   JButton delete;
/*     */   String text;
/*     */ 
/*     */   public Keyboard(JDialog dialog)
/*     */   {
/*  44 */     this.dlg = dialog;
/*     */ 
/*  47 */     setPreferredSize(new Dimension(667, 290));
/*  48 */     this.dlg.setLocation(100, 100);
/*  49 */     this.dlg.setResizable(false);
/*     */ 
/*  51 */     setLayout(new BoxLayout(this, 1));
/*     */ 
/*  54 */     JPanel textPanel = new JPanel();
/*  55 */     textPanel.setLayout(new FlowLayout());
/*  56 */     JLabel label = new JLabel("Bitte Ihre EMail Adresse eingeben und mit \"Enter\" bestaetigen:");
/*  57 */     label.setFont(new Font("SansSerif", 0, 21));
/*  58 */     textPanel.add(label);
/*  59 */     add(textPanel);
/*     */ 
/*  62 */     this.textField = new JTextField();
/*  63 */     this.textField.setEditable(false);
/*  64 */     this.textField.setFont(new Font("SansSerif", 0, 25));
/*  65 */     add(this.textField);
/*     */ 
/*  68 */     add(getRowOfButtons(this.numberButton, this.numbers, 2));
/*  69 */     add(getRowOfButtons(this.firstLetterRowButton, this.firstLetterRow, 0));
/*  70 */     add(getRowOfButtons(this.secondLetterRowButton, this.secondLetterRow, 1));
/*  71 */     add(getRowOfButtons(this.thirdLetterRowButton, this.thirdLetterRow, 0));
/*     */   }
/*     */ 
/*     */   private JPanel getRowOfButtons(JButton[] buttons, String[] buttonLabels, int insertSpecialButton)
/*     */   {
/*  76 */     JPanel panel = new JPanel();
/*  77 */     panel.setLayout(new BoxLayout(panel, 0));
/*  78 */     Font font = new Font("Monospaced", 1, 25);
/*     */ 
/*  81 */     for (int i = 0; i < buttons.length; ++i) {
/*  82 */       buttons[i] = new JButton(buttonLabels[i]);
/*  83 */       buttons[i].setFont(font);
/*     */ 
/*  85 */       buttons[i].setMinimumSize(new Dimension(53, 45));
/*  86 */       buttons[i].setMaximumSize(new Dimension(53, 45));
/*  87 */       buttons[i].setPreferredSize(new Dimension(53, 45));
/*  88 */       buttons[i].addActionListener(this);
/*  89 */       panel.add(buttons[i]);
/*     */     }
/*     */ 
/*  93 */     if (insertSpecialButton == 1) {
/*  94 */       this.enter = new JButton("Enter");
/*  95 */       this.enter.setFont(font);
/*  96 */       this.enter.setMinimumSize(new Dimension(130, 45));
/*  97 */       this.enter.setMaximumSize(new Dimension(130, 45));
/*  98 */       this.enter.setPreferredSize(new Dimension(130, 45));
/*  99 */       this.enter.addActionListener(this);
/* 100 */       panel.add(this.enter);
/* 101 */     } else if (insertSpecialButton == 2) {
/* 102 */       this.delete = new JButton("Delete");
/* 103 */       this.delete.setFont(font);
/* 104 */       this.delete.setMinimumSize(new Dimension(130, 45));
/* 105 */       this.delete.setMaximumSize(new Dimension(130, 45));
/* 106 */       this.delete.setPreferredSize(new Dimension(130, 45));
/* 107 */       this.delete.addActionListener(this);
/* 108 */       panel.add(this.delete);
/*     */     }
/*     */ 
/* 111 */     return panel;
/*     */   }
/*     */ 
/*     */   public void actionPerformed(ActionEvent event) {
/* 115 */     if (event.getSource().equals(this.enter))
/*     */     {
/* 117 */       if (JOptionPane.showConfirmDialog(null, "Ist diese EMail Adresse korrekt: \"" + this.textField.getText() + "\"?", "Bestaetigung", 1) != 0)
/*     */         return;
/* 119 */       this.text = this.textField.getText();
/*     */ 
/* 121 */       this.dlg.dispose();
/*     */     }
/* 123 */     else if (event.getSource().equals(this.delete))
/*     */     {
/* 125 */       String text = this.textField.getText();
/* 126 */       int end = 0;
/* 127 */       if (text.length() > 0) {
/* 128 */         end = text.length() - 1;
/*     */       }
/* 130 */       this.textField.setText(text.substring(0, end));
/*     */     }
/*     */     else {
/* 133 */       this.textField.setText(this.textField.getText() + ((JButton)event.getSource()).getText());
/*     */     }
/*     */   }
/*     */ 
/*     */   public static String getInput(Component parent, String title)
/*     */   {
/* 139 */     while (parent != null) {
/* 140 */       if (parent instanceof Frame)
/* 141 */         return getInput(new JDialog((Frame)parent, title, true));
/* 142 */       if (parent instanceof Dialog)
/* 143 */         return getInput(new JDialog((Dialog)parent, title, true));
/* 144 */       parent = parent.getParent();
/*     */     }
/* 146 */     return getInput(new JDialog((Frame)null, title, true));
/*     */   }
/*     */ 
/*     */   private static String getInput(JDialog dlg) {
/* 150 */     Keyboard kb = new Keyboard(dlg);
/* 151 */     dlg.setContentPane(kb);
/* 152 */     dlg.pack();
/* 153 */     dlg.setVisible(true);
/* 154 */     return kb.text;
/*     */   }
/*     */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.Keyboard
 * JD-Core Version:    0.5.3
 */