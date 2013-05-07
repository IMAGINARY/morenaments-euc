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

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.IOException;
import java.util.List;
import java.util.MissingResourceException;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileFilter;
import de.tum.in.gagern.ornament.*;

public abstract class Export extends FileFilter {

    protected Ornament ornament;
    private String[] extensions;
    protected Color color;
    protected BasicStroke stroke;
    protected OutputStream out;

    protected Export(Ornament ornament, String extension) {
        this(ornament, new String[] { extension });
    }

    protected Export(Ornament ornament, String[] extensions) {
        this.ornament = ornament;
        this.extensions = extensions;
        color = null;
        stroke = null;
    }

    public boolean export(OutputStream out)
            throws IOException, NoninvertibleTransformException {
        this.out = out;
        List lines = ornament.getLines();
        int nLines = lines.size(), maxProgress = nLines + 2;
        ProgressMonitor pm = new ProgressMonitor(ornament, I18n._("exporting"),
                                                 null, 0, maxProgress);
        init();
        prepareStream();
        pm.setProgress(1);
        head();
        BufferedImage bg = ornament.getBackgroundTile();
        if (bg != null) background(bg);
        for (int i = 0; i != nLines && !pm.isCanceled(); ++i) {
            pm.setProgress(2+i);
            export((LinPath)lines.get(i));
        }
        pm.setProgress(2+nLines);
        postBody();
        tail();
        if (out != null) out.close();
        pm.close();
        return true;
    }

    public boolean configure() {
        return true;
    }

    protected void init() throws NoninvertibleTransformException {
        color = null;
        stroke = null;
    }

    protected void prepareStream() throws IOException {
        out = new BufferedOutputStream(out);
    }

    public File fixFile(File file) {
        if (accept(file)) return file;
        else return new File(file.getPath() + "." + getExtensions()[0]);
    }

    public void export(LinPath l)
            throws IOException, NoninvertibleTransformException {
        Color newColor = l.getColor();
        BasicStroke newStroke = l.getStroke();
        if (!newColor.equals(color)) {
            color(newColor);
            color = newColor;
        }
        if (!newStroke.equals(stroke)) {
            stroke(newStroke);
            stroke = newStroke;
        }
        path(l);
    }

    protected void path(LinPath l)
            throws IOException, NoninvertibleTransformException {
        coords(l.getCoordinates());
    }

    protected void head() throws IOException {
    }

    protected void background(BufferedImage img) throws IOException {
    }

    protected void postBody() throws IOException {
    }

    protected void tail() throws IOException {
    }

    protected void color(Color c) throws IOException {
    }

    protected void stroke(BasicStroke s) throws IOException {
    }

    protected void coords(float[] c) throws IOException {
    }

    public boolean accept(File f) {
        if (f.isHidden()) return false;
        if (f.isDirectory()) return true;
        String name = f.getName().toLowerCase();
        String[] exts = getExtensions();
        for (int i = 0; i < exts.length; ++i)
            if (name.endsWith("." + exts[i])) return true;
        return false;
    }

    public abstract String getDescription();

    public String toString() {
        return getDescription();
    }

    public String[] getExtensions() {
        return extensions;
    }

    protected String loadString(String key) throws MissingResourceException {
        String name =
            getClass().getName().replace('.', '/') + "." + key + ".txt";
        InputStream stream =
            getClass().getClassLoader().getResourceAsStream(name);
        String str;
        if (stream == null) {
            throw new MissingResourceException("could not load resource file",
                                               getClass().getName(), key);
        }
        else {
            try {
                char[] buf = new char[2048];
                Reader reader = new InputStreamReader(stream, "UTF-8");
                int pos = 0, len;
                while ((len = reader.read(buf, pos, buf.length-pos)) > 0) {
                    pos += len;
                    if (pos == buf.length) {
                        char[] newArr = new char[buf.length * 4];
                        System.arraycopy(buf, 0, newArr, 0, pos);
                        buf = newArr;
                    }
                }
                return new String(buf, 0, pos);
            }
            catch (IOException e) {
                throw new MissingResourceException(e.toString(),
                                                   getClass().getName(), key);
            }
            finally {
                try {
                    stream.close();
                }
                catch (IOException e) {
                }
            }
        }
    }

}
