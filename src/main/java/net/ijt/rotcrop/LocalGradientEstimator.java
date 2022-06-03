/**
 * 
 */
package net.ijt.rotcrop;

import ij.process.ImageProcessor;
import net.ijt.geom2d.Point2D;
import net.ijt.geom2d.Vector2D;

/**
 * Evaluates the gradient within a 2D or 3D image for a specific position.
 * 
 * The use of this methods requires less memory than the computation of the
 * gradient for the whole image. If the number of positions is very large, it
 * may be more efficient to evaluate from interpolated gradient image instead.
 * 
 * @author dlegland
 *
 */
public class LocalGradientEstimator
{
    /**
     * The parameter that describes the range of the gradient. Default value is
     * 2.0.
     */
    double sigma = 2.0;
    
    double[][] kernel2d;
    
    
    /**
     * Default empty constructor.
     */
    public LocalGradientEstimator()
    {
        this(2.0);
    }
    
    public LocalGradientEstimator(double sigma)
    {
        this.sigma = sigma;
        this.kernel2d = createKernel2D(sigma);
    }
    
    public Vector2D evaluate(ImageProcessor image, Point2D position)
    {
        // retrive image size
        int sizeX = image.getWidth();
        int sizeY = image.getHeight();
        
//        % computation will be performed using point-wise multiplication, so we need
//        % to use the symetric kernels
//        kx = kx(end:-1:1, end:-1:1);
//        ky = ky(end:-1:1, end:-1:1);
        
        // size of the kernels along each dimension
        int n = this.kernel2d.length;
        int r = (n - 1) / 2;
//        ks1 = size(kx, 1);
//        ks2 = size(kx, 2);
//        kr1 = floor((ks1 - 1) / 2);
//        kr2 = floor((ks2 - 1) / 2);
        
        // rounded coordinates of position
        int x0 = (int) Math.round(position.getX());
        int y0 = (int) Math.round(position.getY());
        
        // gradient components
        double gx = 0.0;
        double gy = 0.0;
        
        // iterate over kernel elements
        for (int i = 0; i < n; i++)
        {
            int y = Math.max(Math.min(i + y0 - r, sizeY), 0);
            for (int j = 0; j < n; j++)
            {
                int x = Math.max(Math.min(j + x0 - r, sizeX), 0);
                double value = image.getf(x, y);
                
                gx += (value * kernel2d[i][j]);
                gy += (value * kernel2d[j][i]);
            }
        }
        
        return new Vector2D(gx, gy);
    }

    public static final double[][] createKernel2D(double sigma)
    {
        // compute size according to sigma
        int r = (int) Math.ceil(2 * sigma);
        int n = 2 * r + 1;
        
        // pre-compute linear smoothing and derivative kernels
        double[] ks = new double[n];
        double[] kd = new double[n];
        for (int i = 0; i < n; i++)
        {
            double x = i - r;
            ks[i] = Math.exp(-((x / sigma) * (x / sigma)) * 0.5);
            kd[i] = (x / sigma) * ks[i];
        }
        
        // create kernels by multiplying the two linear kernels
        double[][] xKernel = new double[n][n];
        
        // iterate over matrix elements
        double sum = 0.0;
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                double val = ks[i] * kd[j];
                xKernel[i][j] = val;
                if (val > 0)
                {
                    sum += val;
                }
            }
        }
        
        // normalization of kernel
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                xKernel[i][j] /= sum;
            }
        }
        
        return xKernel;
    }
    
}
