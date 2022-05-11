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
import ij.gui.GUI;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import net.ijt.geom2d.Point2D;
import net.ijt.rotcrop.RotCrop;
import net.ijt.rotcrop.plugins.ChooseNumberWidget.ValueChangeEvent;

/**
 * Plugin for generating a rotated crop from an image.
 * 
 * The main job of the plugin is the create and display an instance of the
 * Frame. The Frame will be responsible for storing the state and calling the
 * necessary methods.
 * 
 * @author dlegland
 *
 */
public class CropOrientedBoxPlugin implements PlugIn
{
    @Override
    public void run(String arg)
    {
        IJ.log("run the crop oriented box plugin");
        
        ImagePlus imagePlus = IJ.getImage();
        
        Roi roi = imagePlus.getRoi();
        Point2D refPoint = getFirstPoint(roi);
        if (refPoint == null)
        {
            // use center of image as default;
            int sizeX = imagePlus.getWidth();
            int sizeY = imagePlus.getHeight();
            refPoint = new Point2D(sizeX * 0.5, sizeY * 0.5);
        }
        
        Frame frame = new Frame(imagePlus, refPoint);
        frame.setVisible(true);
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
        double boxCenterX;
        double boxCenterY;
        double boxAngle;
        // TODO: create a "Box"/"OrientedBox" inner class?


        // ====================================================
        // GUI Widgets
        
        ChooseNumberWidget sizeXWidget;
        ChooseNumberWidget sizeYWidget;
        ChooseNumberWidget boxCenterXWidget;
        ChooseNumberWidget boxCenterYWidget;
        ChooseNumberWidget boxAngleWidget;

        JCheckBox autoUpdateCheckBox;
        JButton runButton;
        
        /** The frame used to display the result of rotated crop. */
        ImageWindow resultFrame = null;
        

        // ====================================================
        // Constructor

        public Frame(ImagePlus imagePlus, Point2D refPoint)
        {
            super("Crop Oriented Box");
            this.imagePlus = imagePlus;
            
            // init default values
            boxSizeX = 400;
            boxSizeY = 400;
            boxCenterX = refPoint.getX();
            boxCenterY = refPoint.getY();
            boxAngle = 0.0;

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
            
            boxCenterXWidget = new ChooseNumberWidget(boxCenterX);
            boxCenterXWidget.addListener(this);
            
            boxCenterYWidget = new ChooseNumberWidget(boxCenterY);
            boxCenterYWidget.addListener(this);
            
            boxAngleWidget = new ChooseNumberWidget(boxAngle);
            boxAngleWidget.addListener(this);
            
            autoUpdateCheckBox = new JCheckBox("Auto-Update", false);
        }

        private void setupLayout()
        {
            // encapsulate into a main panel
            JPanel mainPanel = new JPanel();
            mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

            JPanel sizePanel = GuiHelper.createOptionsPanel("Result Size");
            sizePanel.setLayout(new GridLayout(2, 2));
            sizePanel.add(new JLabel("Size X:"));
            sizePanel.add(sizeXWidget.getPanel());
            sizePanel.add(new JLabel("Size Y:"));
            sizePanel.add(sizeYWidget.getPanel());
            mainPanel.add(sizePanel);
            
            JPanel boxPanel = GuiHelper.createOptionsPanel("Rotated Box");
            boxPanel.setLayout(new GridLayout(3, 2));
            boxPanel.add(new JLabel("Center X:"));
            boxPanel.add(boxCenterXWidget.getPanel());
            boxPanel.add(new JLabel("Center Y:"));
            boxPanel.add(boxCenterYWidget.getPanel());
            boxPanel.add(new JLabel("Angle (degrees):"));
            boxPanel.add(boxAngleWidget.getPanel());
            mainPanel.add(boxPanel);
            
            // also add buttons
            GuiHelper.addInLine(mainPanel, FlowLayout.CENTER, autoUpdateCheckBox, runButton);
            
            // put main panel in the middle of frame
            this.setLayout(new BorderLayout());
            this.add(mainPanel, BorderLayout.CENTER);
        }
        
        public void updateCrop()
        {
            int[] dims = new int[] {boxSizeX, boxSizeY};
            Point2D cropCenter = new Point2D(boxCenterX, boxCenterY);

            ImageProcessor res = RotCrop.rotatedCrop(imagePlus.getProcessor(), dims, cropCenter, boxAngle);
            ImagePlus resultPlus = new ImagePlus("Result", res);
            
            // retrieve frame for displaying result
            if (this.resultFrame == null)
            {
                this.resultFrame = new ImageWindow(resultPlus);
            }
            
            // update display frame, keeping the previous magnification
            double mag = this.resultFrame.getCanvas().getMagnification();
            this.resultFrame.setImage(resultPlus);
            this.resultFrame.getCanvas().setMagnification(mag);
            this.resultFrame.setVisible(true);
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
            else if (evt.getSource() == boxCenterXWidget)
            {
                this.boxCenterX = (int) evt.getNewValue();
            }
            else if (evt.getSource() == boxCenterYWidget)
            {
                this.boxCenterY = (int) evt.getNewValue();
            }
            else if (evt.getSource() == boxAngleWidget)
            {
                this.boxAngle = (int) evt.getNewValue();
            }
            else
            {
                return;
            }
            
            if (this.autoUpdateCheckBox.isSelected())
            {
                updateCrop();
            }
        }
    }
}
