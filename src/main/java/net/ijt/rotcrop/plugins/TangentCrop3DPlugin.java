/**
 * 
 */
package net.ijt.rotcrop.plugins;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GUI;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import net.ijt.geom3d.AffineTransform3D;
import net.ijt.geom3d.Point3D;
import net.ijt.interp.Function3D;
import net.ijt.interp.TransformedImage3D;
import net.ijt.rotcrop.RotCrop;

/**
 * Plugin for generating a rotated crop from an image, by estimating the crop
 * orientation from gradient around the center point.
 * 
 * The main job of the plugin is the create and display an instance of the
 * Frame. The Frame will be responsible for storing the state and calling the
 * necessary methods.
 * 
 * @author dlegland
 *
 */
public class TangentCrop3DPlugin implements PlugIn
{
    @Override
    public void run(String arg)
    {
        IJ.log("run the tangent crop plugin");
        
        ImagePlus imagePlus = IJ.getImage();
        
        if (imagePlus.getStackSize() == 1)
        {
            IJ.error("Requires image to be a stack");
            return;
        }
        
        // use center of image as default position for box center
        int sizeX = imagePlus.getWidth();
        int sizeY = imagePlus.getHeight();
        int sizeZ = imagePlus.getStackSize();
        Point3D refPoint = new Point3D(sizeX * 0.5, sizeY * 0.5, sizeZ * 0.5);
        
        Frame frame = new Frame(imagePlus, refPoint);
        frame.setVisible(true);
        
        // add mouse listener to the input image window to track box positioning
        Canvas canvas = imagePlus.getWindow().getCanvas();
        canvas.addMouseListener(frame);
    }

    public class Frame extends JFrame implements MouseListener
    {
        // ====================================================
        // Static fields

        /**
         * Version ID.
         */
        private static final long serialVersionUID = 1L;

        // ====================================================
        // Class properties
        
        ImagePlus imagePlus;
        
        int boxSizeX;
        int boxSizeY;
        int boxSizeZ;
        double boxCenterX;
        double boxCenterY;
        double boxCenterZ;
        
        double gradientRange;
        // TODO: create a "Box"/"OrientedBox" inner class?


        // ====================================================
        // GUI Widgets
        
        JSpinner sizeXWidget;
        JSpinner sizeYWidget;
        JSpinner sizeZWidget;
        JSpinner boxCenterXWidget;
        JSpinner boxCenterYWidget;
        JSpinner boxCenterZWidget;
        JSpinner gradientRangeWidget;

        JCheckBox autoPreviewCheckBox;
        JButton previewButton;
        JButton runButton;
        
        /** The frame used to display the result of rotated crop. */
        ImageWindow previewFrame = null;
        

        // ====================================================
        // Constructor

        public Frame(ImagePlus imagePlus, Point3D refPoint)
        {
            super("Tangent Crop 3D");
            this.imagePlus = imagePlus;
            
            // init default values
            boxSizeX = 100;
            boxSizeY = 100;
            boxSizeZ = 100;
            boxCenterX = refPoint.x();
            boxCenterY = refPoint.y();
            boxCenterZ = refPoint.z();
            gradientRange = 3.0;

            setupWidgets();
            setupLayout();

            this.pack();

            GUI.center(this);
            setVisible(true);
        }

        private void setupWidgets()
        {
            sizeXWidget = new JSpinner(new SpinnerNumberModel(boxSizeX, 0, 10000, 1));
            sizeXWidget.addChangeListener(evt -> 
            {
                this.boxSizeX = ((SpinnerNumberModel) sizeXWidget.getModel()).getNumber().intValue();
                updatePreviewIfNeeded();
            });
            
            sizeYWidget = new JSpinner(new SpinnerNumberModel(boxSizeY, 0, 10000, 1));
            sizeYWidget.addChangeListener(evt -> 
            {
                this.boxSizeY = ((SpinnerNumberModel) sizeYWidget.getModel()).getNumber().intValue();
                updatePreviewIfNeeded();
            });
            
            sizeZWidget = new JSpinner(new SpinnerNumberModel(boxSizeZ, 0, 10000, 1));
            sizeZWidget.addChangeListener(evt -> 
            {
                this.boxSizeZ = ((SpinnerNumberModel) sizeZWidget.getModel()).getNumber().intValue();
                updatePreviewIfNeeded();
            });
            
            boxCenterXWidget = new JSpinner(new SpinnerNumberModel(boxCenterX, 0, 10000, 1));
            boxCenterXWidget.addChangeListener(evt -> 
            {
                this.boxCenterX = ((SpinnerNumberModel) boxCenterXWidget.getModel()).getNumber().intValue();
                updatePreviewIfNeeded();
            });
            
            boxCenterYWidget = new JSpinner(new SpinnerNumberModel(boxCenterY, 0, 10000, 1));
            boxCenterYWidget.addChangeListener(evt -> 
            {
                this.boxCenterY = ((SpinnerNumberModel) boxCenterYWidget.getModel()).getNumber().intValue();
                updatePreviewIfNeeded();
            });
            
            boxCenterZWidget = new JSpinner(new SpinnerNumberModel(boxCenterZ, 0, 10000, 1));
            boxCenterZWidget.addChangeListener(evt -> 
            {
                this.boxCenterZ = ((SpinnerNumberModel) boxCenterZWidget.getModel()).getNumber().intValue();
                updatePreviewIfNeeded();
            });
            
            gradientRangeWidget = new JSpinner(new SpinnerNumberModel(gradientRange, 0, 10000, 1));
            gradientRangeWidget.addChangeListener(evt -> 
            {
                this.gradientRange = ((SpinnerNumberModel) gradientRangeWidget.getModel()).getNumber().intValue();
                updatePreviewIfNeeded();
            });
            
            autoPreviewCheckBox = new JCheckBox("Auto-Update", false);
            autoPreviewCheckBox.addItemListener(evt -> updatePreviewIfNeeded());
            
            previewButton = new JButton("Preview");
            previewButton.addActionListener(evt -> updatePreview());
            
            runButton = new JButton("Create Result Image");
            runButton.addActionListener(evt -> displayResult());
        }

        private void setupLayout()
        {
            // encapsulate into a main panel
            JPanel mainPanel = new JPanel();
            mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

            JPanel sizePanel = GuiHelper.createOptionsPanel("Result Size");
            sizePanel.setLayout(new GridLayout(3, 2));
            sizePanel.add(new JLabel("Size X:"));
            sizePanel.add(sizeXWidget);
            sizePanel.add(new JLabel("Size Y:"));
            sizePanel.add(sizeYWidget);
            sizePanel.add(new JLabel("Size Z:"));
            sizePanel.add(sizeZWidget);
            mainPanel.add(sizePanel);
            
            JPanel boxPanel = GuiHelper.createOptionsPanel("Box Center");
            boxPanel.setLayout(new GridLayout(3, 2));
            boxPanel.add(new JLabel("Center X:"));
            boxPanel.add(boxCenterXWidget);
            boxPanel.add(new JLabel("Center Y:"));
            boxPanel.add(boxCenterYWidget);
            boxPanel.add(new JLabel("Center Z:"));
            boxPanel.add(boxCenterZWidget);
            mainPanel.add(boxPanel);
            
            JPanel gradientPanel = GuiHelper.createOptionsPanel("Gradient");
            gradientPanel.setLayout(new GridLayout(1, 2));
            gradientPanel.add(new JLabel("Gradient Range:"));
            gradientPanel.add(gradientRangeWidget);
            mainPanel.add(gradientPanel);
            
            // also add buttons
            GuiHelper.addInLine(mainPanel, FlowLayout.CENTER, autoPreviewCheckBox, previewButton);
            GuiHelper.addInLine(mainPanel, FlowLayout.CENTER, runButton);
            
            // put main panel in the middle of frame
            this.setLayout(new BorderLayout());
            this.add(mainPanel, BorderLayout.CENTER);
        }
        
        public void updatePreview()
        {
            // retrieve data
            ImageStack stack = imagePlus.getStack();
            int[] dims = new int[] {boxSizeX, boxSizeY, boxSizeZ};
            Point3D cropCenter = new Point3D(boxCenterX, boxCenterY, boxCenterZ);
            
            // compute the transform
            AffineTransform3D transfo = RotCrop.computeTangentCropTransform(stack, cropCenter, dims, gradientRange);
            
            // Create interpolation class, that encapsulates both the image and the
            // transform
            Function3D interp = new TransformedImage3D(stack, transfo);

            ImageProcessor preview = RotCrop.orthoSlices(interp, dims);
            ImagePlus previewPlus = new ImagePlus("Tangent Crop Preview", preview);

            // retrieve frame for displaying result
            if (this.previewFrame == null)
            {
                this.previewFrame = new ImageWindow(previewPlus);
            }
            // update display frame, keeping the previous magnification
            double mag = this.previewFrame.getCanvas().getMagnification();
            this.previewFrame.setImage(previewPlus);
            this.previewFrame.getCanvas().setMagnification(mag);
            this.previewFrame.setVisible(true);
        }
        
        public void displayResult()
        {
            int[] dims = new int[] {boxSizeX, boxSizeY, boxSizeZ};
            Point3D cropCenter = new Point3D(boxCenterX, boxCenterY, boxCenterZ);
            
            IJ.log("Rot Crop With params: ");
            IJ.log(String.format("  box size: %d x %d x %d", boxSizeX, boxSizeY, boxSizeZ));
            IJ.log(String.format("  refPoint: " + cropCenter));
            IJ.log(String.format("  sigma: %5.2f", gradientRange));
            
            // compute the crop
            ImageStack res = RotCrop.tangentCrop(imagePlus.getStack(), cropCenter, dims, gradientRange);
            ImagePlus resultPlus = new ImagePlus("Result", res);
            
            // display in a new frame
            ImageWindow resultFrame = new StackWindow(resultPlus);
            resultFrame.setVisible(true);
        }

        private void updatePreviewIfNeeded()
        {
            if (this.autoPreviewCheckBox.isSelected())
            {
                updatePreview();
            }
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            Point mousePosition = imagePlus.getWindow().getCanvas().getCursorLoc();
            this.boxCenterX = mousePosition.x;
            ((SpinnerNumberModel) this.boxCenterXWidget.getModel()).setValue(this.boxCenterX);
            this.boxCenterY = mousePosition.y;
            ((SpinnerNumberModel) this.boxCenterYWidget.getModel()).setValue(this.boxCenterY);
            this.boxCenterZ = imagePlus.getCurrentSlice() - 1;
            ((SpinnerNumberModel) this.boxCenterZWidget.getModel()).setValue(this.boxCenterZ);
            
            if (this.autoPreviewCheckBox.isSelected())
            {
                updatePreview();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
        }
    }
}
