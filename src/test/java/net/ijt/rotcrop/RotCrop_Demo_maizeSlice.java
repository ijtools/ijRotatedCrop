/**
 * 
 */
package net.ijt.rotcrop;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import net.ijt.geom2d.Point2D;

/**
 * @author dlegland
 *
 */
public class RotCrop_Demo_maizeSlice
{
    public static final void main(String... args)
    {
        // Load input image
        ImagePlus imagePlus = IJ.openImage(RotCrop_Demo_maizeSlice.class.getResource("/images/wheatGrain_tomo_slice.tif").getFile());
        ImageProcessor image = imagePlus.getProcessor();
        
        int[] dims = new int[] {400, 300};
        Point2D refPoint = new Point2D(410, 80);
        double angle = 15.0;
        
        ImageProcessor res = RotCrop.rotatedCrop(image, dims, refPoint, angle);
        
        // encapsulate into an ImagePlus for display
        ImagePlus resPlus = new ImagePlus("result", res);
        resPlus.show();
    }

}
