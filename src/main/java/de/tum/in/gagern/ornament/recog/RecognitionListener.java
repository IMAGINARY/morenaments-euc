package de.tum.in.gagern.ornament.recog;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import de.tum.in.gagern.ornament.Group;

public interface RecognitionListener {

    public void recognitionSuccessful(Group group,
				      BufferedImage median,
				      AffineTransform transform)
	throws NoninvertibleTransformException;

    public void recognitionCanceled();

    public void recognitionFailed(Throwable e);

}
