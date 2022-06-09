/**
 * 
 */
package net.ijt.rotcrop.plugins;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
        
        // add mouse listener to the input image window to track box positioning
        Canvas canvas = imagePlus.getWindow().getCanvas();
        canvas.addMouseListener(frame);
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

    public class Frame extends JFrame implements ActionListener, ChangeListener, ItemListener, MouseListener
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
        
        int boxSizeX = 400;
        int boxSizeY = 400;
        double boxCenterX = 0.0;
        double boxCenterY = 0.0;
        double boxAngle = 0.0;
        // TODO: create a "Box"/"OrientedBox" inner class?


        // ====================================================
        // GUI Widgets
        
        JSpinner sizeXWidget;
        JSpinner sizeYWidget;
        JSpinner boxCenterXWidget;
        JSpinner boxCenterYWidget;
        JSpinner boxAngleWidget;

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
            
            
            sizeXWidget = new JSpinner(new SpinnerNumberModel(boxSizeX, 0, 10000, 1));
            sizeXWidget.addChangeListener(this);
            
            sizeYWidget = new JSpinner(new SpinnerNumberModel(boxSizeY, 0, 10000, 1));
            sizeYWidget.addChangeListener(this);
            
            boxCenterXWidget = new JSpinner(new SpinnerNumberModel(boxCenterX, 0, 10000, 1));
            boxCenterXWidget.addChangeListener(this);
            
            boxCenterYWidget = new JSpinner(new SpinnerNumberModel(boxCenterY, 0, 10000, 1));
            boxCenterYWidget.addChangeListener(this);
            
            boxAngleWidget = new JSpinner(new SpinnerNumberModel(boxAngle, -180, 180, 1));
            boxAngleWidget.addChangeListener(this);
            
            autoUpdateCheckBox = new JCheckBox("Auto-Update", false);
            autoUpdateCheckBox.addItemListener(this);
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
            boxPanel.add(new JLabel("Angle (degrees):"));
            boxPanel.add(boxAngleWidget);
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
            else if (evt.getSource() == boxAngleWidget)
            {
                this.boxAngle = ((SpinnerNumberModel) boxAngleWidget.getModel()).getNumber().doubleValue();
            }
            else
            {
                System.err.println("CropOrientedBoxPlugin: unknown widget updated...");
                return;
            }
            
            if (this.autoUpdateCheckBox.isSelected())
            {
                updateCrop();
            }
        }
        
        @Override
        public void itemStateChanged(ItemEvent evt)
        {
            if (evt.getSource() == autoUpdateCheckBox)
            {
                if (this.autoUpdateCheckBox.isSelected())
                {
                    updateCrop();
                }
            }
            else
            {
                System.err.println("CropOrientedBoxPlugin: unknown widget updated...");
                return;
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
            
            if (this.autoUpdateCheckBox.isSelected())
            {
                updateCrop();
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
