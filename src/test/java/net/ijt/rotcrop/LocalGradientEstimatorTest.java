/**
 * 
 */
package net.ijt.rotcrop;

import static org.junit.Assert.*;

import org.junit.Test;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import net.ijt.geom2d.Point2D;
import net.ijt.geom2d.Vector2D;

/**
 * @author dlegland
 *
 */
public class LocalGradientEstimatorTest
{

    /**
     * Test method for {@link net.ijt.rotcrop.LocalGradientEstimator#evaluate(ij.process.ImageProcessor, net.ijt.geom2d.Point2D)}.
     */
    @Test
    public final void testEvaluate()
    {
        ImageProcessor image = new ByteProcessor(20, 20);
        for (int y = 0; y < 20; y++)
        {
            double y2 = y - 10.0;
            for (int x = 0; x < 20; x++)
            {
                double x2 = x - 10.0;
                double h = Math.sqrt(x2 * x2 + y2 * y2);
                if (h > 7.0)
                {
                    image.set(x, y, 255);
                }
            }
        }
        
        LocalGradientEstimator gradEst = new LocalGradientEstimator(3.0);
        Vector2D grad = gradEst.evaluate(image, new Point2D(15.0, 5.0)).normalize();
        
        System.out.println("grad: " + grad.getX() + ", " + grad.getY());
    }

    /**
     * Test method for {@link net.ijt.rotcrop.LocalGradientEstimator#createKernel2D(double)}.
     */
    @Test
    public final void testCreateKernel2D_sigma1()
    {
        double[][] kernel = LocalGradientEstimator.createKernel2D(1.0);
        int n = kernel.length;
        
        double sum = 0.0;
        double posSum = 0.0;
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                double v = kernel[i][j];
                sum += v;
                if(v > 0) posSum += v;
            }
        }
        
        assertEquals(0.0, sum, 0.01);
        assertEquals(1.0, posSum, 0.01);
    }

    /**
     * Test method for {@link net.ijt.rotcrop.LocalGradientEstimator#createKernel2D(double)}.
     */
    @Test
    public final void testCreateKernel2D_sigma2()
    {
        double[][] kernel = LocalGradientEstimator.createKernel2D(2.0);
        int n = kernel.length;
        
        double sum = 0.0;
        double posSum = 0.0;
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                double v = kernel[i][j];
                sum += v;
                if(v > 0) posSum += v;
//                System.out.print(String.format(Locale.ENGLISH, " %5.2f", kernel[i][j]));
            }
//            System.out.println("");
        }
        
        assertEquals(0.0, sum, 0.01);
        assertEquals(1.0, posSum, 0.01);
    }

}
