/**
 * 
 */
package net.ijt.rotcrop;

import java.util.ArrayList;
import java.util.Locale;

import org.junit.Test;

import net.ijt.geom3d.AffineTransform3D;
import net.ijt.geom3d.Point3D;

/**
 * @author dlegland
 *
 */
public class RotCropTest
{

    /**
     * Test method for {@link net.ijt.rotcrop.RotCrop#computeTransform(int[], int[], net.ijt.geom3d.Point3D, double[])}.
     */
    @Test
    public final void testComputeTransform()
    {
        int[] boxDims = new int[] {20, 20, 20};
        Point3D refPoint = new Point3D(100.0, 100.0, 100.0);
        double[] rotAngles = new double[] {0.0, 90.0, 0.0};
        AffineTransform3D transfo = RotCrop.computeTransform(refPoint, boxDims, rotAngles);
        
        ArrayList<Point3D> corners = new ArrayList<Point3D>(8);
        corners.add(new Point3D(  0.0,   0.0,   0.0));
        corners.add(new Point3D(300.0,   0.0,   0.0));
        corners.add(new Point3D(  0.0, 300.0,   0.0));
        corners.add(new Point3D(300.0, 300.0,   0.0));
        corners.add(new Point3D(  0.0,   0.0, 300.0));
        corners.add(new Point3D(300.0,   0.0, 300.0));
        corners.add(new Point3D(  0.0, 300.0, 300.0));
        corners.add(new Point3D(300.0, 300.0, 300.0));
        
        for (Point3D corner : corners)
        {
            Point3D cornerT = transfo.transform(corner);
            print("corner %s -> %s", formatCoords(corner), formatCoords(cornerT)); 
        }
        
    }
    
    private static final void print(String pattern, Object... args)
    {
        System.out.println(String.format(Locale.ENGLISH, pattern, args));
    }
    
    private static final String formatCoords(Point3D p)
    {
        String pattern = "(%7.2f, %7.2f, %7.2f)";
        return String.format(Locale.ENGLISH, pattern, p.getX(), p.getY(), p.getZ());
        
    }

}
