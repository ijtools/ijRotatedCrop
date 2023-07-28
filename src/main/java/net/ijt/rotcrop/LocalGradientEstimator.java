/**
 * 
 */
package net.ijt.rotcrop;

import ij.ImageStack;
import ij.process.ImageProcessor;
import net.ijt.geom2d.Point2D;
import net.ijt.geom2d.Vector2D;
import net.ijt.geom3d.Point3D;
import net.ijt.geom3d.Vector3D;

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
    
    /**
     * The kernel for computing 2D gradient in the x direction.
     * First index is y, second one is x.
     */
    double[][] kernel2d;
    
    /**
     * The kernel for computing 3D gradient in the x direction.
     * First index is z, second one is y, third one is x.
     */
    double[][][] kernel3d;
    
    
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
        this.kernel3d = createKernel3D(sigma);
        
//        printKernel(kernel3d, 100);
    }
    
    public Vector2D evaluate(ImageProcessor image, Point2D position)
    {
        // retrive image size
        int sizeX = image.getWidth();
        int sizeY = image.getHeight();
        
        // size of the kernels along each dimension
        int n = this.kernel2d.length;
        int r = (n - 1) / 2;
        
        // rounded coordinates of position
        int x0 = (int) Math.round(position.x());
        int y0 = (int) Math.round(position.y());
        
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

    public Vector3D evaluate(ImageStack image, Point3D position)
    {
        // retrive image size
        int sizeX = image.getWidth();
        int sizeY = image.getHeight();
        int sizeZ = image.getSize();
        
        // size of the kernels along each dimension
        int n = this.kernel3d.length;
        int r = (n - 1) / 2;
        
        // rounded coordinates of position
        int x0 = (int) Math.round(position.x());
        int y0 = (int) Math.round(position.y());
        int z0 = (int) Math.round(position.z());
        
        // gradient components
        double gx = 0.0;
        double gy = 0.0;
        double gz = 0.0;
        
        // iterate over kernel elements
        for (int k = 0; k < n; k++)
        {
            int z = Math.max(Math.min(k + z0 - r, sizeZ), 0);
            for (int j = 0; j < n; j++)
            {
                int y = Math.max(Math.min(j + y0 - r, sizeY), 0);
                for (int i = 0; i < n; i++)
                {
                    int x = Math.max(Math.min(i + x0 - r, sizeX), 0);
                    double value = image.getVoxel(x, y, z);

                    gx += (value * kernel3d[k][j][i]);
                    gy += (value * kernel3d[i][k][j]);
                    gz += (value * kernel3d[i][j][k]);
                }
            }
        }
        
        return new Vector3D(gx, gy, gz);
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
    
    public static final double[][][] createKernel3D(double sigma)
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
        
//        printKernel(ks, 100);
//        printKernel(kd, 100);
        
        // create kernels by multiplying the two linear kernels
        double[][][] xKernel = new double[n][n][n];
        
        // iterate over matrix elements
        double sum = 0.0;
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                for (int k = 0; k < n; k++)
                {
                    double val = ks[i] * ks[j] * kd[k];
                    xKernel[i][j][k] = val;
                    if (val > 0)
                    {
                        sum += val;
                    }
                }
            }
        }
        
        // normalization of kernel
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                for (int k = 0; k < n; k++)
                {
                    xKernel[i][j][k] /= sum;
                }
            }
        }
        
        return xKernel;
    }
    
//    private static final void printKernel(double[] kernel, double s)
//    {
//        System.out.print("kernel = [");
//        for (int i = 0; i < kernel.length; i++)
//        {
//            System.out.print(String.format(Locale.ENGLISH, " %5.3f", kernel[i] * s));
//        }
//        System.out.println("]");
//    }
//    
//    private static final void printKernel(double[][][] kernel, double s)
//    {
//        for (int k = 0; k < kernel.length; k++)
//        {
//            System.out.println("slice" + k + ":");
//            for (int j = 0; j < kernel[0].length; j++)
//            {
//                for (int i = 0; i < kernel[0][0].length; i++)
//                {
//                    System.out.print(String.format(Locale.ENGLISH, " %5.3f", kernel[k][j][i] * s));
//                }
//                System.out.println("");
//            }
//        }
//    }
}
