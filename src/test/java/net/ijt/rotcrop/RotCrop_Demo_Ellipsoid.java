/**
 * 
 */
package net.ijt.rotcrop;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import net.ijt.geom3d.Point3D;

/**
 * @author dlegland
 *
 */
public class RotCrop_Demo_Ellipsoid
{
    public static final void main(String... args)
    {
        // Load input image
        ImagePlus imagePlus = IJ.openImage(RotCrop_Demo_Ellipsoid.class.getResource("/images/ellipsoid_200x150x100.tif").getFile());
        ImageStack image = imagePlus.getStack();

        int[] dims = new int[] { 100, 100, 100 };
//        Point3D refPoint = new Point3D(250, 200, 50);
//        double[] anglesInDegrees = new double[] { 0.0, 0.0, 0.0 };
        Point3D refPoint = new Point3D(450, 200, 150);
        double[] anglesInDegrees = new double[] { 0.0, 90.0, 0.0 };

        ImageStack res = RotCrop.rotatedCrop(image, dims, refPoint, anglesInDegrees);

        
        ImagePlus resPlus = new ImagePlus("result", res);
        resPlus.show();
    }

}
