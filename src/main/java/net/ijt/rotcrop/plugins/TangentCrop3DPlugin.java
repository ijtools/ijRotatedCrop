/**
 * 
 */
package net.ijt.rotcrop.plugins;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
    }

    public class Frame extends JFrame implements ActionListener, ChangeListener
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
            boxCenterX = refPoint.getX();
            boxCenterY = refPoint.getY();
            boxCenterZ = refPoint.getZ();
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
            sizeXWidget.addChangeListener(this);
            
            sizeYWidget = new JSpinner(new SpinnerNumberModel(boxSizeY, 0, 10000, 1));
            sizeYWidget.addChangeListener(this);
            
            sizeZWidget = new JSpinner(new SpinnerNumberModel(boxSizeZ, 0, 10000, 1));
            sizeZWidget.addChangeListener(this);
            
            boxCenterXWidget = new JSpinner(new SpinnerNumberModel(boxCenterX, 0, 10000, 1));
            boxCenterXWidget.addChangeListener(this);
            
            boxCenterYWidget = new JSpinner(new SpinnerNumberModel(boxCenterY, 0, 10000, 1));
            boxCenterYWidget.addChangeListener(this);
            
            boxCenterZWidget = new JSpinner(new SpinnerNumberModel(boxCenterZ, 0, 10000, 1));
            boxCenterZWidget.addChangeListener(this);
            
            gradientRangeWidget = new JSpinner(new SpinnerNumberModel(gradientRange, 0, 10000, 1));
            gradientRangeWidget.addChangeListener(this);
            
            autoPreviewCheckBox = new JCheckBox("Auto-Update", false);
            previewButton = new JButton("Preview");
            previewButton.addActionListener(this);
            
            runButton = new JButton("Create Result Image");
            runButton.addActionListener(this);
            
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

        @Override
        public void actionPerformed(ActionEvent evt)
        {
            if (evt.getSource() == previewButton)
            {
                updatePreview();
            }
            else if (evt.getSource() == runButton)
            {
                displayResult();
            }
            else
            {
                System.err.println("TangentCrop3DPlugin: unknown widget updated...");
                return;
            }
        }

        @Override
        public void stateChanged(ChangeEvent evt)
        {
            if (evt.getSource() == sizeXWidget)
            {
                this.boxSizeX = ((SpinnerNumberModel) sizeXWidget.getModel()).getNumber().intValue();
            }
            else if (evt.getSource() == sizeYWidget)
            {
                this.boxSizeY = ((SpinnerNumberModel) sizeYWidget.getModel()).getNumber().intValue();
            }
            else if (evt.getSource() == sizeZWidget)
            {
                this.boxSizeZ = ((SpinnerNumberModel) sizeZWidget.getModel()).getNumber().intValue();
            }
            else if (evt.getSource() == boxCenterXWidget)
            {
                this.boxCenterX = ((SpinnerNumberModel) boxCenterXWidget.getModel()).getNumber().doubleValue();
            }
            else if (evt.getSource() == boxCenterYWidget)
            {
                this.boxCenterY = ((SpinnerNumberModel) boxCenterYWidget.getModel()).getNumber().doubleValue();
            }
            else if (evt.getSource() == boxCenterZWidget)
            {
                this.boxCenterZ = ((SpinnerNumberModel) boxCenterZWidget.getModel()).getNumber().doubleValue();
            }
            else if (evt.getSource() == gradientRangeWidget)
            {
                this.gradientRange = ((SpinnerNumberModel) gradientRangeWidget.getModel()).getNumber().doubleValue();
            }
            else
            {
                System.err.println("TangentCrop3DPlugin: unknown widget updated...");
                return;
            }
            
            if (this.autoPreviewCheckBox.isSelected())
            {
                updatePreview();
            }
        }
    }
}
