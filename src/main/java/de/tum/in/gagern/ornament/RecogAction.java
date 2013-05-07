package de.tum.in.gagern.ornament;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import de.tum.in.gagern.ornament.recog.*;

class RecogAction extends AbstractAction implements RecognitionListener {

    private Ornament main;
    private JFileChooser fc;

    public RecogAction(Ornament main) {
        super(I18n.dots(I18n._("Recognize")));
        this.main = main;
    }

    public void actionPerformed(ActionEvent evnt) {
        try {
            recognize();
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog
                (main, e.toString(), I18n._("Recognize"),
                 JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recognize() throws IOException {
        if (fc == null) fc = new JFileChooser(".");
        if (fc.showOpenDialog(main)
            != JFileChooser.APPROVE_OPTION) return;
        File file=fc.getSelectedFile();
        if (file == null) return;
        BufferedImage img = ImageIO.read(file);
        Recognizer recog = new Recognizer(main, this, img, main.isDebug());
        Thread thread = new Thread(recog, "Recognizer");
        thread.setDaemon(true);
        thread.start();
    }

    public void recognitionSuccessful(final Group group,
                                      final BufferedImage median,
                                      final AffineTransform transform)
    {
        final int ax = (int)Math.round(transform.getScaleX());
        final int ay = (int)Math.round(transform.getShearY());
        final int bx = (int)Math.round(transform.getShearX());
        final int by = (int)Math.round(transform.getScaleY());
        EventQueue.invokeLater(new Runnable() { public void run() {
            main.setRecognizedImage(group, ax, ay, bx, by, median);
        }});
    }

    public void recognitionCanceled() {
    }

    public void recognitionFailed(final Throwable e) {
        e.printStackTrace();
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog
                        (main, e.toString(), I18n._("Recognize"),
                         JOptionPane.ERROR_MESSAGE);
                }
            });
    }

}
