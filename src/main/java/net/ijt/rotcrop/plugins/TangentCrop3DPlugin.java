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
import javax.swing.border.EmptyBorder;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GUI;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.plugin.PlugIn;
import net.ijt.geom3d.Point3D;
import net.ijt.rotcrop.RotCrop;
import net.ijt.rotcrop.plugins.ChooseNumberWidget.ValueChangeEvent;

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

    public class Frame extends JFrame implements ActionListener, ChooseNumberWidget.Listener
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
        
        ChooseNumberWidget sizeXWidget;
        ChooseNumberWidget sizeYWidget;
        ChooseNumberWidget sizeZWidget;
        ChooseNumberWidget boxCenterXWidget;
        ChooseNumberWidget boxCenterYWidget;
        ChooseNumberWidget boxCenterZWidget;
        ChooseNumberWidget gradientRangeWidget;

        JCheckBox autoUpdateCheckBox;
        JButton runButton;
        
        /** The frame used to display the result of rotated crop. */
        ImageWindow resultFrame = null;
        

        // ====================================================
        // Constructor

        public Frame(ImagePlus imagePlus, Point3D refPoint)
        {
            super("Tangent Box");
            this.imagePlus = imagePlus;
            
            // init default values
            boxSizeX = 50;
            boxSizeY = 50;
            boxSizeZ = 50;
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
            runButton = new JButton("Run!");
            runButton.addActionListener(this);
            
            sizeXWidget = new ChooseNumberWidget(boxSizeX, 0);
            sizeXWidget.addListener(this);
            
            sizeYWidget = new ChooseNumberWidget(boxSizeY, 0);
            sizeYWidget.addListener(this);
            
            sizeZWidget = new ChooseNumberWidget(boxSizeZ, 0);
            sizeZWidget.addListener(this);
            
            boxCenterXWidget = new ChooseNumberWidget(boxCenterX);
            boxCenterXWidget.addListener(this);
            
            boxCenterYWidget = new ChooseNumberWidget(boxCenterY);
            boxCenterYWidget.addListener(this);
            
            boxCenterZWidget = new ChooseNumberWidget(boxCenterZ);
            boxCenterZWidget.addListener(this);
            
            gradientRangeWidget = new ChooseNumberWidget(gradientRange);
            gradientRangeWidget.addListener(this);
            
            autoUpdateCheckBox = new JCheckBox("Auto-Update", false);
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
            sizePanel.add(sizeXWidget.getPanel());
            sizePanel.add(new JLabel("Size Y:"));
            sizePanel.add(sizeYWidget.getPanel());
            sizePanel.add(new JLabel("Size Z:"));
            sizePanel.add(sizeZWidget.getPanel());
            mainPanel.add(sizePanel);
            
            JPanel boxPanel = GuiHelper.createOptionsPanel("Box Center");
            boxPanel.setLayout(new GridLayout(3, 2));
            boxPanel.add(new JLabel("Center X:"));
            boxPanel.add(boxCenterXWidget.getPanel());
            boxPanel.add(new JLabel("Center Y:"));
            boxPanel.add(boxCenterYWidget.getPanel());
            boxPanel.add(new JLabel("Center Z:"));
            boxPanel.add(boxCenterZWidget.getPanel());
            mainPanel.add(boxPanel);
            
            JPanel gradientPanel = GuiHelper.createOptionsPanel("Gradient");
            gradientPanel.setLayout(new GridLayout(1, 2));
            gradientPanel.add(new JLabel("Gradient Range:"));
            gradientPanel.add(gradientRangeWidget.getPanel());
            mainPanel.add(gradientPanel);
            
            // also add buttons
            GuiHelper.addInLine(mainPanel, FlowLayout.CENTER, autoUpdateCheckBox, runButton);
            
            // put main panel in the middle of frame
            this.setLayout(new BorderLayout());
            this.add(mainPanel, BorderLayout.CENTER);
        }
        
        public void updateCrop()
        {
            int[] dims = new int[] {boxSizeX, boxSizeY, boxSizeZ};
            Point3D cropCenter = new Point3D(boxCenterX, boxCenterY, boxCenterZ);
            
            IJ.log("Rot Crop With params: ");
            IJ.log(String.format("  box size: %d x %d x %d", boxSizeX, boxSizeY, boxSizeZ));
            IJ.log(String.format("  refPoint: " + cropCenter));
            IJ.log(String.format("  sigma: %5.2f", gradientRange));
            
//            double[] angles = new double[] {boxRotZ, boxRotY, boxRotX};

            
            ImageStack res = RotCrop.tangentCrop(imagePlus.getStack(), cropCenter, dims, gradientRange);
            ImagePlus resultPlus = new ImagePlus("Result", res);
            
            // retrieve frame for displaying result
            if (this.resultFrame == null)
            {
                this.resultFrame = new StackWindow(resultPlus);
                this.resultFrame.setVisible(true);
            }
            
            // keep current slice
//            int slice = this.resultFrame.getImagePlus().getSlice();
//            IJ.log("slice: " + slice);
            
            // update display frame, keeping the previous magnification
//            double mag = this.resultFrame.getCanvas().getMagnification();
            this.resultFrame.setImage(resultPlus);

            // restore previous display settings
//            this.resultFrame.getCanvas().setMagnification(mag);
//            this.resultFrame.showSlice(slice);
//            resultPlus.setSlice(slice);
//            IJ.log("slice again2: " + this.resultFrame.getImagePlus().getSlice());

        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            updateCrop();
        }

        @Override
        public void valueChanged(ValueChangeEvent evt)
        {
            if (evt.getSource() == sizeXWidget)
            {
                this.boxSizeX = (int) evt.getNewValue();
            }
            else if (evt.getSource() == sizeYWidget)
            {
                this.boxSizeY = (int) evt.getNewValue();
            }
            else if (evt.getSource() == sizeZWidget)
            {
                this.boxSizeZ = (int) evt.getNewValue();
            }
            else if (evt.getSource() == boxCenterXWidget)
            {
                this.boxCenterX = (int) evt.getNewValue();
            }
            else if (evt.getSource() == boxCenterYWidget)
            {
                this.boxCenterY = (int) evt.getNewValue();
            }
            else if (evt.getSource() == boxCenterZWidget)
            {
                this.boxCenterZ = (int) evt.getNewValue();
            }
            else if (evt.getSource() == gradientRangeWidget)
            {
                this.gradientRange = evt.getNewValue();
            }
            else
            {
                System.err.println("TangentCrop3DPlugin: unknown widget updated...");
                return;
            }
            
            if (this.autoUpdateCheckBox.isSelected())
            {
                updateCrop();
            }
        }
    }
}
