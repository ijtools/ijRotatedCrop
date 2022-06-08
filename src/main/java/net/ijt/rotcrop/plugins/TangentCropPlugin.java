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
import ij.gui.GUI;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import net.ijt.geom2d.Point2D;
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
public class TangentCropPlugin implements PlugIn
{
    @Override
    public void run(String arg)
    {
        IJ.log("run the tangent crop plugin");
        
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
        double boxCenterX;
        double boxCenterY;
        
        double gradientRange;
        // TODO: create a "Box"/"OrientedBox" inner class?


        // ====================================================
        // GUI Widgets
        
        JSpinner sizeXWidget;
        JSpinner sizeYWidget;
        JSpinner boxCenterXWidget;
        JSpinner boxCenterYWidget;
        JSpinner gradientRangeWidget;

        JCheckBox autoUpdateCheckBox;
        JButton runButton;
        
        /** The frame used to display the result of rotated crop. */
        ImageWindow resultFrame = null;
        

        // ====================================================
        // Constructor

        public Frame(ImagePlus imagePlus, Point2D refPoint)
        {
            super("Tangent Box");
            this.imagePlus = imagePlus;
            
            // init default values
            boxSizeX = 400;
            boxSizeY = 400;
            boxCenterX = refPoint.getX();
            boxCenterY = refPoint.getY();
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
            
            sizeXWidget = new JSpinner(new SpinnerNumberModel(boxSizeX, 0, 10000, 1));
            sizeXWidget.addChangeListener(this);
            
            sizeYWidget = new JSpinner(new SpinnerNumberModel(boxSizeY, 0, 10000, 1));
            sizeYWidget.addChangeListener(this);
            
            boxCenterXWidget = new JSpinner(new SpinnerNumberModel(boxCenterX, 0, 10000, 1));
            boxCenterXWidget.addChangeListener(this);
            
            boxCenterYWidget = new JSpinner(new SpinnerNumberModel(boxCenterY, 0, 10000, 1));
            boxCenterYWidget.addChangeListener(this);
            
            gradientRangeWidget = new JSpinner(new SpinnerNumberModel(gradientRange, 0, 1000, 1));
            gradientRangeWidget.addChangeListener(this);
            
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
            sizePanel.add(sizeXWidget);
            sizePanel.add(new JLabel("Size Y:"));
            sizePanel.add(sizeYWidget);
            mainPanel.add(sizePanel);
            
            JPanel boxPanel = GuiHelper.createOptionsPanel("Rotated Box");
            boxPanel.setLayout(new GridLayout(3, 2));
            boxPanel.add(new JLabel("Center X:"));
            boxPanel.add(boxCenterXWidget);
            boxPanel.add(new JLabel("Center Y:"));
            boxPanel.add(boxCenterYWidget);
            boxPanel.add(new JLabel("Gradient Range (pixels):"));
            boxPanel.add(gradientRangeWidget);
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

            ImageProcessor res = RotCrop.tangentCrop(imagePlus.getProcessor(), cropCenter, dims, gradientRange);
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
            else if (evt.getSource() == boxCenterXWidget)
            {
                this.boxCenterX = ((SpinnerNumberModel) boxCenterXWidget.getModel()).getNumber().doubleValue();
            }
            else if (evt.getSource() == boxCenterYWidget)
            {
                this.boxCenterY = ((SpinnerNumberModel) boxCenterYWidget.getModel()).getNumber().doubleValue();
            }
            else if (evt.getSource() == gradientRangeWidget)
            {
                this.gradientRange = ((SpinnerNumberModel) gradientRangeWidget.getModel()).getNumber().doubleValue();
            }
            else
            {
                System.err.println("TangentCropPlugin: unknown widget updated...");
                return;
            }
            
            if (this.autoUpdateCheckBox.isSelected())
            {
                updateCrop();
            }
        }
    }
}
