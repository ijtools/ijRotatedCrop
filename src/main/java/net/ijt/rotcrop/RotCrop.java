/**
 * 
 */
package net.ijt.rotcrop;

import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import net.ijt.geom2d.AffineTransform2D;
import net.ijt.geom2d.Point2D;
import net.ijt.geom3d.AffineTransform3D;
import net.ijt.geom3d.Point3D;
import net.ijt.interp.Function2D;
import net.ijt.interp.Function3D;
import net.ijt.interp.TransformedImage2D;
import net.ijt.interp.TransformedImage3D;

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
    
    public static final ImageStack rotatedCrop(ImageStack image, int[] dims, Point3D refPoint, double[] anglesInDegrees)
    {
        // retrieve image dimensions
        int sizeX = dims[0];
        int sizeY = dims[1];
        int sizeZ = dims[2];

        // create elementary transforms
        AffineTransform3D trBoxCenter = AffineTransform3D.createTranslation(-sizeX / 2, -sizeY / 2, -sizeZ / 2);
        AffineTransform3D rotZ = AffineTransform3D.createRotationOz(Math.toRadians(anglesInDegrees[0]));
        AffineTransform3D rotY = AffineTransform3D.createRotationOy(Math.toRadians(anglesInDegrees[1]));
        AffineTransform3D rotX = AffineTransform3D.createRotationOx(Math.toRadians(anglesInDegrees[2]));
        AffineTransform3D trRefPoint = AffineTransform3D.createTranslation(refPoint.getX(), refPoint.getY(), refPoint.getZ());

        // concatenate into global display-image-to-source-image transform
        AffineTransform3D transfo = trRefPoint.concatenate(rotX).concatenate(rotY).concatenate(rotZ).concatenate(trBoxCenter);

        // Create interpolation class, that encapsulates both the image and the
        // transform
        Function3D interp = new TransformedImage3D(image, transfo);

        // allocate result image
        ImageStack res = ImageStack.create(sizeX, sizeY, sizeZ, 8);

        // iterate over pixel of target image
        for (int z = 0; z < sizeZ; z++)
        {
            for (int y = 0; y < sizeY; y++)
            {
                for (int x = 0; x < sizeX; x++)
                {
                    res.setVoxel(x, y, z, interp.evaluate(x, y, z));
                }
            }
        }
        return res;
    }
}
