/**
 * 
 */
package net.ijt.rotcrop.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import net.ijt.geom2d.Point2D;
import net.ijt.rotcrop.RotCrop;

/**
 * @author dlegland
 *
 */
public class CropOrientedBoxPlugin implements PlugIn
{

    @Override
    public void run(String arg)
    {
        IJ.log("hello...");
        
        ImagePlus imagePlus = IJ.getImage();
        ImageProcessor image = imagePlus.getProcessor();
        
        Roi roi = imagePlus.getRoi();
        Point2D refPoint = getFirstPoint(roi);
        if (refPoint == null)
        {
            // use center of image as default;
            int sizeX = imagePlus.getWidth();
            int sizeY = imagePlus.getHeight();
            refPoint = new Point2D(sizeX * 0.5, sizeY * 0.5);
        }
        
        // Prepare dialog
        GenericDialog gd = new GenericDialog("Rotated Crop");
        gd.addNumericField("Crop_Size_X", 300, 0);
        gd.addNumericField("Crop_Size_Y", 200, 0);
        gd.addNumericField("Crop_Center_X", refPoint.getX(), 2);
        gd.addNumericField("Crop_Center_Y", refPoint.getY(), 2);
        gd.addNumericField("Orientation (degrees)", 0, 2);
        
        gd.showDialog();
        if (gd.wasCanceled())
        {
            return;
        }
        
        int cropSizeX = (int) gd.getNextNumber();
        int cropSizeY = (int) gd.getNextNumber();
        double cropCenterX = gd.getNextNumber();
        double cropCenterY = gd.getNextNumber();
        double orient = gd.getNextNumber();
        
        int[] dims = new int[] {cropSizeX, cropSizeY};
        Point2D cropCenter = new Point2D(cropCenterX, cropCenterY);
        
        IJ.log("compute");
        ImageProcessor res = RotCrop.rotatedCrop(image, dims, cropCenter, orient);
        
        String newName = imagePlus.getShortTitle() + "-rotCrop";
        ImagePlus resPlus = new ImagePlus(newName, res);
        
        resPlus.copyScale(imagePlus);
        resPlus.show();        
    }
    
    /**
     * Returns the first point within the selected ROI. If the ROI is null,
     * return null.
     * 
     * @param roi
     *            a Region of Interest (ROI)
     * @return the first point used to define the ROI
     */
    private static final Point2D getFirstPoint(Roi roi)
    {
        if (roi == null) return null;
        FloatPolygon poly = roi.getFloatPolygon();
        return new Point2D(poly.xpoints[0], poly.ypoints[0]);
    }

}
