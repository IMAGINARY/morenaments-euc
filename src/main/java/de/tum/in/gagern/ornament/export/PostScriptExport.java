/* $Id$

Ornament 1.2 Copyright (C) 2003-2006 Martin von Gagern <Martin.vGagern@gmx.net>
Ornament drawing application/applet using crystallographic groups

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 */

package de.tum.in.gagern.ornament.export;

import de.tum.in.gagern.ornament.*;

import java.awt.Color;
import java.awt.BasicStroke;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

public class PostScriptExport extends Export {

    public static final int TYPE_PS = 1;
    public static final int TYPE_PSGZ = 2;
    public static final int TYPE_EPS = 3;

    private static final String[] ids = { null, "ps", "ps.gz", "eps" };

    protected Writer writer;
    private int type;
    private float[] colorComps;
    private double[] matrix;
    private int[] vectors;

    private boolean base85 = true;

    public PostScriptExport(Ornament ornament, int type) {
        super(ornament, ids[type]);
        if (type < TYPE_PS || type > TYPE_EPS)
            throw new IllegalArgumentException("invalid type: " + type);
        this.type = type;
        colorComps = new float[3];
        matrix = new double[6];
        vectors = new int[4];
    }

    public String getDescription() {
        switch (type) {
        case TYPE_PS:
            return I18n._("export.descr.ps");
        case TYPE_PSGZ:
            return I18n._("export.descr.ps.gz");
        case TYPE_EPS:
            return I18n._("export.descr.eps");
        }
        throw new IllegalStateException("invalid type");
    }

    protected void prepareStream() throws IOException {
        super.prepareStream();
        if (type == TYPE_PSGZ) out = new GZIPOutputStream(out);
        writer = new OutputStreamWriter(out, "ASCII");
    }

    protected void head() throws IOException {

        StringBuffer t = new StringBuffer();
        Group g = ornament.getGroup();
        for (int i = 0; i < g.countTransforms(); ++i) {
            if (i != 0) t.append('\n');
            g.getTransform(i).getMatrix(matrix);
            for (int j = 0; j < 6; ++j) {
                if (j != 0) t.append(' ');
                t.append(matrix[j]);
            }
        }

        StringBuffer v = new StringBuffer();
        ornament.getVectors(vectors);
        v.append(vectors[0]).append(' ')
            .append(-vectors[1]).append(' ')
            .append(vectors[2]).append(' ')
            .append(-vectors[3])
            .append(" 0 H");

        StringBuffer bg = new StringBuffer();
        ornament.getBackgroundColor().getRGBColorComponents(colorComps);
        bg.append(colorComps[0]).append(' ')
            .append(colorComps[1]).append(' ')
            .append(colorComps[2]);

        Object[] args = {
            new Integer(type),
            new Integer(595),
            new Integer(842),
            t.toString(),
            v.toString(),
            bg.toString(),
            Ornament.getDescription(),
            new Date(),
        };
        writer.write(MessageFormat.format(loadString("head"), args));

    }

    protected void tail() throws IOException {
        writer.write(loadString("foot"));
        writer.close();
    }

    protected void color(Color c) throws IOException {
        c.getRGBColorComponents(colorComps);
        printPlain(colorComps[0]);
        writer.write(' ');
        printPlain(colorComps[1]);
        writer.write(' ');
        printPlain(colorComps[2]);
        writer.write(" setrgbcolor\n");
    }

    protected void stroke(BasicStroke s) throws IOException {
        printPlain(s.getLineWidth());
        writer.write(" setlinewidth\n");
    }

    protected void coords(float[] c) throws IOException {
        float minx, maxx, miny, maxy;
        minx = maxx = c[0];
        miny = maxy = c[1];
        for (int i=2; i<c.length; i+=2) {
            if (minx > c[i]) minx = c[i];
            if (maxx < c[i]) maxx = c[i];
            if (miny > c[i+1]) miny = c[i+1];
            if (maxy < c[i+1]) maxy = c[i+1];
        }
        writer.write("{ ");
        if (base85) {
            writer.write("<~");
            printBase85(0x95300000 | (c.length + 4));
        }
        else {
            writer.write("{\n");
        }
        print(minx);
        print(miny);
        print(maxx);
        print(maxy);
        print(c[0]);
        print(c[1]);
        for (int i = 2; i < c.length; ++i) {
            if (i % 16 == 2) writer.write('\n');
            print(c[i] - c[i-2]);
        }
        if (base85) {
            writer.write("\n~>");
        }
        else {
            writer.write("\n}");
        }
        writer.write(" <0B0001");
        int count = c.length / 2 - 1;
        while (count > 255-32) {
            writer.write("FF04");
            count -= 255-32;
        }
        count += 32;
        printHexDigit(count / 16);
        printHexDigit(count % 16);
        writer.write("04> } L\n");
    }

    private void print(float f) throws IOException {
        if (base85) printBase85(f);
        else printPlain(f);
    }

    private void printPlain(float f) throws IOException {
        writer.write(Float.toString(f));
    }

    private void printBase85(int value) throws IOException {
        if (value == 0) {
            writer.write('z');
            return;
        }
        long v = value & 0xffffffffL;
        for (int i = 85 * 85 * 85 * 85; i >= 1; i /= 85) {
            writer.write((int)(33 + (v / i) % 85));
        }
    }

    private void printBase85(float value) throws IOException {
        printBase85(Float.floatToIntBits(value));
    }

    private void printHexDigit(int digit) throws IOException {
        if (digit < 0 || digit >= 16) throw new IllegalArgumentException();
        if (digit < 10) writer.write(digit + '0');
        else writer.write(digit + ('A' - 10));
    }

}
