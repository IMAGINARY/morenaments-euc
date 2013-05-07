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
import java.io.IOException;
import java.util.Properties;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.mail.Session;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.util.ByteArrayDataSource;
import javax.activation.DataHandler;

import de.tum.in.gagern.ornament.*;

public class MailExportMechanism extends ExportMechanism {

    public MailExportMechanism(Ornament main) {
        super(main);
    }

    public void export() {
        Export export = chooseExport();
        if (export == null) return;
        if (!export.configure()) return;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            export.export(os);
            byte[] data = os.toByteArray();
            String to = Keyboard.getInput(main, I18n._("Email Address"));
            if (to == null) return;
            sendMail(to, export, data);
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
        catch (MessagingException ex) {
            JOptionPane.showMessageDialog
                (main, ex.getLocalizedMessage(), I18n._("Send"),
                 JOptionPane.ERROR_MESSAGE);
        }
    }

    private Export chooseExport() {
        return (Export) JOptionPane.showInputDialog
            (main, I18n._("Choose file format"), I18n._("Export"),
             JOptionPane.QUESTION_MESSAGE, null, exports, exports[0]);
    }

    private static String[] mailProperties = {
        "mail.debug",
        "mail.from",
        "mail.mime.address.strict",
        "mail.host",
        // "mail.store.protocol",
        "mail.transport.protocol",
        "mail.user",
        "mail.smtp.class",
        "mail.smtp.host",
        "mail.smtp.port",
        "mail.smtp.user",
        "mail.smtp.localhost",
        "mail.subject",
    };

    private void sendMail(String to, Export export, byte[] data)
        throws MessagingException {

        Properties properties = new Properties(System.getProperties());
        for (int i = 0; i != mailProperties.length; ++i) {
            String value = main.getParameter(mailProperties[i]);
            if (value == null) continue;
            properties.setProperty(mailProperties[i], value);
        }
        Session session = Session.getInstance(properties);

        // create a message
        MimeMessage msg = new MimeMessage(session);
        //msg.setFrom(new InternetAddress(from));
        InternetAddress[] address = {new InternetAddress(to)};
        msg.setRecipients(MimeMessage.RecipientType.TO, address);
        //msg.setSubject(subject);

        // create and fill the first message part
        MimeBodyPart mbp1 = new MimeBodyPart();
        mbp1.setText(I18n._("mail.body"));

        // create the second message part
        MimeBodyPart mbp2 = new MimeBodyPart();

        // attach the file to the message
        String mimeType = "application/octet-stream";
        ByteArrayDataSource ds = new ByteArrayDataSource(data, mimeType);
        mbp2.setDataHandler(new DataHandler(ds));
        mbp2.setFileName("ornament." + export.getExtensions()[0]);

        // create the Multipart and add its parts to it
        MimeMultipart mp = new MimeMultipart();
        mp.addBodyPart(mbp1);
        mp.addBodyPart(mbp2);

        // add the Multipart to the message
        msg.setContent(mp);

        // set the Date: header
        msg.setSentDate(new Date());

        // send the message
        Transport.send(msg);
    }

}
