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

import java.awt.geom.NoninvertibleTransformException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.AccessControlException;
import javax.jnlp.FileSaveService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JOptionPane;
import de.tum.in.gagern.ornament.*;

public class ServiceExportMechanism extends ExportMechanism {

    private FileSaveService fss;
    private AccessControlException prevExn;

    public ServiceExportMechanism(Ornament main,
                                  AccessControlException prevExn) {
        super(main);
        this.prevExn = prevExn;
    }

    public boolean init() {
        if (!super.init()) return false;
        try {
            fss = (FileSaveService)
                ServiceManager.lookup(FileSaveService.class.getName());
            return true;
        }
        catch (UnavailableServiceException e) {
            Object[] args = {
                prevExn.getLocalizedMessage(),
                e.getLocalizedMessage(),
                prevExn.toString(),
                e.toString()
            };
            JOptionPane.showMessageDialog
                (main, I18n._("File save service unavailable", args),
                 I18n._("Export"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public void export() {
        Export export = chooseExport();
        if (export == null) return;
        if (!export.configure()) return;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            export.export(os);
            ByteArrayInputStream is =
                new ByteArrayInputStream(os.toByteArray());
            fss.saveFileDialog(null, export.getExtensions(), is, null);
        }
        catch (IOException ex) {
            JOptionPane.showMessageDialog
                (main, ex.getLocalizedMessage(), I18n._("Export"),
                 JOptionPane.ERROR_MESSAGE);
        }
        catch (NoninvertibleTransformException ex) {
            JOptionPane.showMessageDialog
                (main, ex.getLocalizedMessage(), I18n._("Export"),
                 JOptionPane.ERROR_MESSAGE);
        }
    }

    private Export chooseExport() {
        return (Export) JOptionPane.showInputDialog
            (main, I18n._("Choose file format"), I18n._("Export"),
             JOptionPane.QUESTION_MESSAGE, null, exports, exports[0]);
    }

}
