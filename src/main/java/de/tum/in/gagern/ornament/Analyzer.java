/*     */ package de.tum.in.gagern.ornament;
/*     */ 
/*     */ import javax.swing.JPanel;
/*     */ 
/*     */ class Analyzer extends JPanel
/*     */ {
/*     */   static final int TURN2 = 1;
/*     */   static final int TURN3 = 2;
/*     */   static final int TURN4 = 4;
/*     */   static final int TURN6 = 8;
/*     */   static final int TIMES2 = 3;
/*     */   static final int TIMES3 = 7;
/*     */   static final int TIMES4 = 15;
/*     */   static final int MIRROR = 16;
/*     */   static final int GLIDE = 256;
/*     */   static final int ON = 256;
/*     */   static final int NOTON = 32768;
/*     */   static final int ALL = 129892351;
/*  57 */   static final int[] PROPS = { 0, 1, 16, 256, 272, 4145, 590097, 8389377, 8983345, 5, 42030069, 10821429, 2, 16787314, 17835890, 11, 92389371 };
/*     */   int yes;
/*     */   int no;
/*     */ 
/*     */   boolean isSet(int bit, boolean value)
/*     */   {
/* 106 */     return ((((value) ? this.yes : this.no) & bit) == bit);
/*     */   }
/*     */ 
/*     */   void set(int bit, boolean value) {
/* 110 */     if (value) this.yes |= bit;
/*     */     else this.no |= bit;
/* 112 */     int possible = 0; int nposs = 0; int newyes = 129892351; int newno = 0;
/* 113 */     for (int i = 0; i < PROPS.length; ++i) {
/* 114 */       int p = PROPS[i];
/* 115 */       if (((p & this.yes) == this.yes) && ((p & this.no) == this.no)) {
/* 116 */         possible = i;
/* 117 */         ++nposs;
/* 118 */         newyes &= p;
/* 119 */         newno |= p;
/*     */       }
/*     */     }
/* 122 */     this.yes = newyes;
/* 123 */     this.no = (newno ^ 0x7BDFFFF);
/*     */   }
/*     */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.Analyzer
 * JD-Core Version:    0.5.3
 */