/*     */ package de.tum.in.gagern.ornament.export;
/*     */ 
/*     */ import de.tum.in.gagern.ornament.I18n;
/*     */ import de.tum.in.gagern.ornament.Ornament;
/*     */ import java.awt.Color;
/*     */ import java.awt.geom.AffineTransform;
/*     */ import java.awt.geom.Area;
/*     */ import java.awt.geom.PathIterator;
/*     */ import java.io.IOException;
/*     */ import java.io.OutputStreamWriter;
/*     */ import java.io.Writer;
/*     */ import java.text.MessageFormat;
/*     */ import java.util.Date;
/*     */ import java.util.zip.GZIPOutputStream;
/*     */ 
/*     */ public class SvgExport extends OutlineExport
/*     */ {
/*     */   public static final int COMPRESS_NONE = 0;
/*     */   public static final int COMPRESS_GZIP = 1;
/*  42 */   private static final String[] ids = { "svg", "svg.gz" };
/*     */   private int compress;
/*     */   private MessageFormat fmtPattern;
/*     */   private int precision;
/*     */   private AffineTransform at;
/*     */   private Writer writer;
/*     */   private int vw;
/*     */   private int vh;
/*     */   private String vMatrix;
/*     */ 
/*     */   public SvgExport(Ornament ornament, int compress)
/*     */   {
/*  59 */     super(ornament, ids[compress]);
/*  60 */     this.compress = compress;
/*  61 */     this.fmtPattern = new MessageFormat(loadString("pattern"));
/*     */   }
/*     */ 
/*     */   public String getDescription() {
/*  65 */     switch (this.compress)
/*     */     {
/*     */     case 0:
/*  67 */       return I18n._("export.descr.svg");
/*     */     case 1:
/*  69 */       return I18n._("export.descr.svg.gz");
/*     */     }
/*  71 */     throw new IllegalStateException("invalid type");
/*     */   }
/*     */ 
/*     */   protected void prepareStream() throws IOException {
/*  75 */     super.prepareStream();
/*  76 */     if (this.compress == 1) this.out = new GZIPOutputStream(this.out);
/*  77 */     this.writer = new OutputStreamWriter(this.out, "ASCII");
/*     */   }
/*     */ 
/*     */   protected void head() throws IOException {
/*  81 */     int[] vec = new int[4];
/*  82 */     this.ornament.getVectors(vec);
/*  83 */     StringBuffer v = new StringBuffer();
/*  84 */     int maxDimen = Math.abs(vec[0]);
/*  85 */     for (int i = 0; i != 4; ++i) {
/*  86 */       v.append(vec[i]).append(' ');
/*  87 */       maxDimen = Math.max(maxDimen, Math.abs(vec[i]));
/*     */     }
/*  89 */     v.append("0 0");
/*  90 */     this.vMatrix = v.toString();
/*     */ 
/*  92 */     maxDimen *= 64;
/*  93 */     for (this.precision = 1; this.precision < maxDimen; this.precision <<= 1);
/*  94 */     this.at = AffineTransform.getScaleInstance(this.precision, this.precision);
/*  95 */     int width = 640; int height = 480;
/*  96 */     String unit = "px";
/*  97 */     this.vw = (width * this.precision);
/*  98 */     this.vh = (height * this.precision);
/*     */ 
/* 100 */     Object[] args = { Integer.toString(width), Integer.toString(height), unit, Integer.toString(this.vw), Integer.toString(this.vh), this.vMatrix, Ornament.getDescription(), new Date(), Integer.toString(this.precision), this.vMatrix };
/*     */ 
/* 112 */     this.writer.write(MessageFormat.format(loadString("head"), args));
/*     */   }
/*     */ 
/*     */   private String colorToString(Color c) {
/* 116 */     String colStr = Integer.toString(c.getRGB() & 0xFFFFFF, 16);
/* 117 */     for (; colStr.length() < 6; colStr = "0" + colStr);
/* 118 */     colStr = "#" + colStr;
/* 119 */     return colStr;
/*     */   }
/*     */ 
/*     */   protected void area(Area area, Color color) throws IOException {
/* 123 */     PathIterator pi = area.getPathIterator(this.at);
/*     */     String fillRule;
/* 125 */     switch (pi.getWindingRule())
/*     */     {
/*     */     case 0:
/* 127 */       fillRule = "evenodd";
/* 128 */       break;
/*     */     case 1:
/* 130 */       fillRule = "nonzero";
/* 131 */       break;
/*     */     default:
/* 133 */       throw new IllegalArgumentException("invalid winding rule");
/*     */     }
/*     */ 
/* 136 */     Object[] args = { null, Integer.toString(this.precision), fillRule, colorToString(color), this.vMatrix };
/*     */ 
/* 143 */     StringBuffer buf = new StringBuffer();
/* 144 */     this.fmtPattern.format(args, buf, null);
/* 145 */     this.writer.write(buf.toString());
/* 146 */     float[] coords = new float[6];
/* 147 */     char lastOp = '\0';
/* 148 */     int lineLen = 0;
/* 149 */     int sx = 0; int sy = 0; int cx = 0; int cy = 0;
/* 150 */     while (!(pi.isDone()))
/*     */     {
/*     */       char nextOp;
/*     */       int numCoords;
/* 151 */       switch (pi.currentSegment(coords))
/*     */       {
/*     */       case 0:
/* 153 */         nextOp = 'm';
/* 154 */         numCoords = 2;
/* 155 */         sx = Math.round(coords[0]);
/* 156 */         sy = Math.round(coords[1]);
/* 157 */         break;
/*     */       case 1:
/* 159 */         nextOp = 'l';
/* 160 */         numCoords = 2;
/* 161 */         break;
/*     */       case 2:
/* 163 */         nextOp = 'q';
/* 164 */         numCoords = 4;
/* 165 */         break;
/*     */       case 3:
/* 167 */         nextOp = 'c';
/* 168 */         numCoords = 6;
/* 169 */         break;
/*     */       case 4:
/* 171 */         nextOp = 'z';
/* 172 */         numCoords = 0;
/* 173 */         cx = sx;
/* 174 */         cy = sy;
/* 175 */         break;
/*     */       default:
/* 177 */         throw new IllegalArgumentException("invalid segment type");
/*     */       }
/*     */ 
/* 180 */       if (nextOp == lastOp) nextOp = ' ';
/*     */       else {
/* 182 */         lastOp = nextOp;
/*     */       }
/* 184 */       if (numCoords == 0) {
/* 185 */         this.writer.write(nextOp);
/*     */       }
/*     */       else {
/* 188 */         for (int i = 0; i != numCoords; ++i) {
/* 189 */           int iCoord = Math.round(coords[i]);
/* 190 */           if (i % 2 == 0) iCoord -= cx;
/*     */           else iCoord -= cy;
/* 192 */           String number = Integer.toString(iCoord);
/* 193 */           if (lineLen + number.length() > 75) {
/* 194 */             this.writer.write(10);
/* 195 */             lineLen = 0;
/* 196 */             if (nextOp != ' ') {
/* 197 */               this.writer.write(nextOp);
/* 198 */               ++lineLen;
/*     */             }
/*     */           }
/* 201 */           else if ((nextOp != ' ') || (number.charAt(0) != '-')) {
/* 202 */             ++lineLen;
/* 203 */             this.writer.write(nextOp);
/*     */           }
/* 205 */           nextOp = ' ';
/* 206 */           this.writer.write(number);
/* 207 */           lineLen += number.length();
/*     */         }
/* 209 */         cx = Math.round(coords[(numCoords - 2)]);
/* 210 */         cy = Math.round(coords[(numCoords - 1)]);
/*     */       }
/*     */ 
/* 213 */       pi.next();
/*     */     }
/* 215 */     if (lineLen + 3 > 75) this.writer.write(10);
/* 216 */     this.writer.write("\"/>\n");
/*     */   }
/*     */ 
/*     */   protected void tail() throws IOException {
/* 220 */     Object[] args = { colorToString(this.ornament.getBackgroundColor()), Integer.toString(this.vw), Integer.toString(this.vh) };
/*     */ 
/* 222 */     this.writer.write(MessageFormat.format(loadString("foot"), args));
/* 223 */     this.writer.close();
/*     */   }
/*     */ }

/* Location:           /Users/weissman/Downloads/euc.jar
 * Qualified Name:     de.tum.in.gagern.ornament.export.SvgExport
 * JD-Core Version:    0.5.3
 */