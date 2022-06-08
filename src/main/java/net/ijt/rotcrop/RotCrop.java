/**
 * 
 */
package net.ijt.rotcrop;

import ij.IJ;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import net.ijt.geom2d.AffineTransform2D;
import net.ijt.geom2d.Point2D;
import net.ijt.geom2d.Vector2D;
import net.ijt.geom3d.AffineTransform3D;
import net.ijt.geom3d.DefaultAffineTransform3D;
import net.ijt.geom3d.Point3D;
import net.ijt.geom3d.Vector3D;
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

        // Computes the transform that will map indices from within result image
        // into coordinates within source image
        AffineTransform3D transfo = computeTransform(refPoint, dims, anglesInDegrees);
        
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
    
    public static final ImageProcessor tangentCrop(ImageProcessor image, Point2D refPoint, int[] dims, double gradientSigma)
    {
        // retrieve image dimensions
        int sizeX = dims[0];
        int sizeY = dims[1];
        
        LocalGradientEstimator gradEst = new LocalGradientEstimator(gradientSigma);
        Vector2D grad = gradEst.evaluate(image, refPoint);
        double angle = Math.atan2(grad.getY(), grad.getX()) - Math.PI/2;

        // create elementary transforms
        AffineTransform2D trBoxCenter = AffineTransform2D.createTranslation(-sizeX / 2, -sizeY / 2);
        AffineTransform2D rot = AffineTransform2D.createRotation(angle);
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
    
    public static final AffineTransform2D computeTransform(int[] boxSize, Point2D refPoint, double boxAngle)
    {
        // create elementary transforms
        AffineTransform2D trBoxCenter = AffineTransform2D.createTranslation(-boxSize[0] * 0.5, -boxSize[1] * 0.5);
        AffineTransform2D rot = AffineTransform2D.createRotation(boxAngle);
        AffineTransform2D trRefPoint = AffineTransform2D.createTranslation(refPoint.getX(), refPoint.getY());

        // concatenate into global display-image-to-source-image transform
        AffineTransform2D transfo = trRefPoint.concatenate(rot).concatenate(trBoxCenter);
        return transfo;
    }
    
    public static final AffineTransform3D computeTransform(Point3D boxCenter, int[] boxSize, double[] anglesInDegrees)
    {
        // create a translation to put center of the box at the origin
        int sizeX = boxSize[0];
        int sizeY = boxSize[1];
        int sizeZ = boxSize[2];
        AffineTransform3D trBoxCenter = AffineTransform3D.createTranslation(-sizeX / 2, -sizeY / 2, -sizeZ / 2);
        
        // then, apply 3D rotation by Euler angles followed by translation to
        // put origin (= box center) on the reference point
        AffineTransform3D transfo = rotateAndShift(anglesInDegrees, boxCenter).concatenate(trBoxCenter);
        
//        System.out.println("transfo: " + transfo);
        return transfo;
    }
    

    public static final ImageStack tangentCrop(ImageStack image, Point3D refPoint, int[] dims, double gradientSigma)
    {
        // retrieve box dimensions
        int sizeX = dims[0];
        int sizeY = dims[1];
        int sizeZ = dims[2];
        
//        // evaluate gradient around chosen point
//        LocalGradientEstimator gradEst = new LocalGradientEstimator(gradientSigma);
//        Vector3D grad = gradEst.evaluate(image, refPoint).normalize();
//        IJ.log("  Gradient: " + grad);
//        
//        // find the basis vector the less orthogonal to the gradient
//        Vector3D[] basisVectors = new Vector3D[] {new Vector3D(1, 0, 0), new Vector3D(0, 1, 0), new Vector3D(0, 0, 1)};
//        Vector3D[] crossProds = new Vector3D[3];
//        double maxVal = 0.0;
//        int indMax = 1;
//        for (int i = 0; i < 3; i++)
//        {
//            crossProds[i] = Vector3D.crossProduct(grad, basisVectors[i]);
//            double norm = crossProds[i].norm(); 
//            if (norm > maxVal)
//            {
//                maxVal = norm;
//                indMax = i;
//            }
//        }
//        
//        // identify two vectors orthogonal to the outwards normal
//        Vector3D vt1 = crossProds[indMax];
//        Vector3D vt2 = Vector3D.crossProduct(grad, vt1);
//        IJ.log("  vt1: " + vt1);
//        IJ.log("  vt2: " + vt2);
//        
//        // convert eigen vectors to rotation matrix
//        // (concatenate column vectors corresponding to eigen vectors, and transpose)
//        double m00 = vt1.getX();
//        double m01 = vt1.getY();
//        double m02 = vt1.getZ();
//        double m10 = vt2.getX();
//        double m11 = vt2.getY();
//        double m12 = vt2.getZ();
//        double m20 = grad.getX();
//        double m21 = grad.getY();
//        double m22 = grad.getZ();
//        AffineTransform3D rot = new DefaultAffineTransform3D(m00, m10, m20, 0,  m01, m11, m21, 0,  m02, m12, m22, 0);
//         
//        // create elementary transforms
//        AffineTransform3D trBoxCenter = AffineTransform3D.createTranslation(-(sizeX - 1) * 0.5, -(sizeY - 1) * 0.5, -(sizeZ - 1) * 0.5);
//        AffineTransform3D trRefPoint = AffineTransform3D.createTranslation(refPoint);
//
//        // concatenate into global display-image-to-source-image transform
//        // create the transform matrix that maps from box coords to global coords
//        AffineTransform3D transfo = trRefPoint.concatenate(rot).concatenate(trBoxCenter);

        AffineTransform3D transfo = computeTangentCropTransform(image, refPoint, dims, gradientSigma);
        
        // Create interpolation class, that encapsulates both the image and the
        // transform
        Function3D interp = new TransformedImage3D(image, transfo);

        // allocate result image
        ImageStack res = ImageStack.create(sizeX, sizeY, sizeZ, 8);
        
        fillStack(res, interp);
//
//        // iterate over voxel of target image
//        for (int z = 0; z < sizeZ; z++)
//        {
//            for (int y = 0; y < sizeY; y++)
//            {
//                for (int x = 0; x < sizeX; x++)
//                {
//                    res.setVoxel(x, y, z, interp.evaluate(x, y, z));
//                }
//            }
//        }
        
        return res;
    }
    
    public static final AffineTransform3D computeTangentCropTransform(ImageStack image, Point3D refPoint, int[] dims, double gradientSigma)
    {
        // retrieve box dimensions
        int sizeX = dims[0];
        int sizeY = dims[1];
        int sizeZ = dims[2];
        
        // evaluate gradient around chosen point
        LocalGradientEstimator gradEst = new LocalGradientEstimator(gradientSigma);
        Vector3D grad = gradEst.evaluate(image, refPoint).normalize();
        IJ.log("  Gradient: " + grad);
        
        // find the basis vector the less orthogonal to the gradient
        Vector3D[] basisVectors = new Vector3D[] {new Vector3D(1, 0, 0), new Vector3D(0, 1, 0), new Vector3D(0, 0, 1)};
        Vector3D[] crossProds = new Vector3D[3];
        double maxVal = 0.0;
        int indMax = 1;
        for (int i = 0; i < 3; i++)
        {
            crossProds[i] = Vector3D.crossProduct(grad, basisVectors[i]);
            double norm = crossProds[i].norm(); 
            if (norm > maxVal)
            {
                maxVal = norm;
                indMax = i;
            }
        }
        
        // identify two vectors orthogonal to the outwards normal
        Vector3D vt1 = crossProds[indMax];
        Vector3D vt2 = Vector3D.crossProduct(grad, vt1);
        IJ.log("  vt1: " + vt1);
        IJ.log("  vt2: " + vt2);
        
        // convert eigen vectors to rotation matrix
        // (concatenate column vectors corresponding to eigen vectors, and transpose)
        double m00 = vt1.getX();
        double m01 = vt1.getY();
        double m02 = vt1.getZ();
        double m10 = vt2.getX();
        double m11 = vt2.getY();
        double m12 = vt2.getZ();
        double m20 = grad.getX();
        double m21 = grad.getY();
        double m22 = grad.getZ();
        AffineTransform3D rot = new DefaultAffineTransform3D(m00, m10, m20, 0,  m01, m11, m21, 0,  m02, m12, m22, 0);
         
        // create elementary transforms
        AffineTransform3D trBoxCenter = AffineTransform3D.createTranslation(-(sizeX - 1) * 0.5, -(sizeY - 1) * 0.5, -(sizeZ - 1) * 0.5);
        AffineTransform3D trRefPoint = AffineTransform3D.createTranslation(refPoint);

        // concatenate into global display-image-to-source-image transform
        // create the transform matrix that maps from box coords to global coords
        AffineTransform3D transfo = trRefPoint.concatenate(rot).concatenate(trBoxCenter);
        
        return transfo;
    }
    
    /**
     * Fills the voxels of the specified stack according to the values obtained
     * from the Function3D instance.
     * 
     * @param stack
     *            the stack to fill
     * @param fun
     *            the function used to populate stack values
     */
    public static final void fillStack(ImageStack stack, Function3D fun)
    {
        // retrieve stack size
        int sizeX = stack.getWidth();
        int sizeY = stack.getHeight();
        int sizeZ = stack.getSize();
        
        // iterate over stack voxels
        for (int z = 0; z < sizeZ; z++)
        {
            for (int y = 0; y < sizeY; y++)
            {
                for (int x = 0; x < sizeX; x++)
                {
                    stack.setVoxel(x, y, z, fun.evaluate(x, y, z));
                }
            }
        }
    }
    
    /**
     * Computes the box-to-world transform, that will transform coordinates from
     * the box basis into the world (global) basis. The origin in the box basis
     * will be mapped into the box center in the global basis.
     * 
     * @param anglesInDegrees
     *            the three Euler angles (in degrees) that define the box
     *            orientation.
     * @param boxCenter
     *            the center of the box
     * @return an affine transform that can be used to compute coordinates of
     *         box corners in global basis
     */
    public static final AffineTransform3D rotateAndShift(double[] anglesInDegrees, Point3D refPoint)
    {
        AffineTransform3D rotZ = AffineTransform3D.createRotationOz(Math.toRadians(anglesInDegrees[0]));
        AffineTransform3D rotY = AffineTransform3D.createRotationOy(Math.toRadians(anglesInDegrees[1]));
        AffineTransform3D rotX = AffineTransform3D.createRotationOx(Math.toRadians(anglesInDegrees[2]));
        AffineTransform3D trans = AffineTransform3D.createTranslation(refPoint);
        
        // concatenate into global display-image-to-source-image transform
        return trans.concatenate(rotZ).concatenate(rotY).concatenate(rotX);
    }
    
    public static final ImageProcessor orthoSlices(ImageStack stack)
    {
        int sizeX = stack.getWidth();
        int sizeY = stack.getHeight();
        int sizeZ = stack.getSize();
        
        int posX = sizeX / 2;
        int posY = sizeY / 2;
        int posZ = sizeZ / 2;
        
        int sizeX2 = 2 * sizeX + sizeY;
        int sizeY2 = Math.max(sizeY,  sizeZ);
        ImageProcessor res = new ByteProcessor(sizeX2, sizeY2);
        
        // add XY slice
        for (int y = 0; y < sizeY; y++)
        {
            for (int x = 0; x < sizeX; x++)
            {
                res.set(x, y, (int) stack.getVoxel(x, y, posZ));
            }
        }
        
        // add XZ slice
        for (int z = 0; z < sizeZ; z++)
        {
            for (int x = 0; x < sizeX; x++)
            {
                res.set(x + sizeX, z, (int) stack.getVoxel(x, posY, z));
            }
        }
        
        // add YZ slice
        for (int z = 0; z < sizeZ; z++)
        {
            for (int y = 0; y < sizeY; y++)
            {
                res.set(y + 2 * sizeX, z, (int) stack.getVoxel(posX, y, z));
            }
        }
        
        return res;
    }

    public static final ImageProcessor orthoSlices(Function3D fun, int[] stackDims)
    {
        int sizeX = stackDims[0];
        int sizeY = stackDims[1];
        int sizeZ = stackDims[2];
        
        int posX = sizeX / 2;
        int posY = sizeY / 2;
        int posZ = sizeZ / 2;
        
        int sizeX2 = 2 * sizeX + sizeY;
        int sizeY2 = Math.max(sizeY,  sizeZ);
        ImageProcessor res = new ByteProcessor(sizeX2, sizeY2);
        
        // add XY slice
        for (int y = 0; y < sizeY; y++)
        {
            for (int x = 0; x < sizeX; x++)
            {
                res.set(x, y, (int) fun.evaluate(x, y, posZ));
            }
        }
        
        // add XZ slice
        for (int z = 0; z < sizeZ; z++)
        {
            for (int x = 0; x < sizeX; x++)
            {
                res.set(x + sizeX, z, (int) fun.evaluate(x, posY, z));
            }
        }
        
        // add YZ slice
        for (int z = 0; z < sizeZ; z++)
        {
            for (int y = 0; y < sizeY; y++)
            {
                res.set(y + 2 * sizeX, z, (int) fun.evaluate(posX, y, z));
            }
        }
        
        return res;
    }

}
