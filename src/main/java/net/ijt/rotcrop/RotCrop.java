/**
 * 
 */
package net.ijt.rotcrop;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import net.ijt.geom2d.AffineTransform2D;
import net.ijt.geom2d.Point2D;
import net.ijt.interp.Function2D;
import net.ijt.interp.TransformedImage2D;

/**
 * @author dlegland
 *
 */
public class RotCrop
{
    public static final ImageProcessor rotatedCrop(ImageProcessor image, int[] dims, Point2D refPoint, double angleInDegrees)
    {
        // retrieve image dimensions
        int sizeX = dims[0];
        int sizeY = dims[1];

        // create elementary transforms
        AffineTransform2D trBoxCenter = AffineTransform2D.createTranslation(-sizeX / 2, -sizeY / 2);
        AffineTransform2D rot = AffineTransform2D.createRotation(Math.toRadians(angleInDegrees));
        AffineTransform2D trRefPoint = AffineTransform2D.createTranslation(refPoint.getX(), refPoint.getY());

        // concatenate into global display-image-to-source-image transform
        AffineTransform2D transfo = trRefPoint.concatenate(rot).concatenate(trBoxCenter);

        // Create interpolation class, that encapsulates both the image and the
        // transform
        Function2D interp = new TransformedImage2D(image, transfo);

        // allocate result image
        ImageProcessor res = new ByteProcessor(sizeX, sizeY);

        // iterate over pixel of target image
        for (int y = 0; y < sizeY; y++)
        {
            for (int x = 0; x < sizeX; x++)
            {
                res.setf(x, y, (float) interp.evaluate(x, y));
            }
        }

        return res;
    }
}
