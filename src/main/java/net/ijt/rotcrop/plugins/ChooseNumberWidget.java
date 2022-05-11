/**
 * 
 */
package net.ijt.rotcrop.plugins;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * @author dlegland
 *
 */
public class ChooseNumberWidget implements ActionListener, KeyListener
{
    // ====================================================
    // Class properties

    double value;
    int nDigits = 2;

    JPanel panel;

    JTextField textField;
    JButton minusButton;
    JButton plusButton;
    
    /** A list of listeners to react to value changes */
    ArrayList<Listener> listeners = new ArrayList<Listener>(4);
    

    // ====================================================
    // Constructor

    public ChooseNumberWidget(double value)
    {
        this(value, 2);
    }
    
    public ChooseNumberWidget(double value, int nDigits)
    {
        // init reference values
        this.value = value;
        this.nDigits = nDigits;
        
        // setup widgets
        this.textField = createNumericTextField(this.value);
        this.minusButton = createPlusMinusButton("-");
        this.plusButton = createPlusMinusButton("+");

        // setup Layout
        this.panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(textField);
        panel.add(minusButton);
        panel.add(plusButton);
    }

    private JTextField createNumericTextField(double initialValue)
    {
        String text = doubleToString(initialValue);
        JTextField textField = new JTextField(text, 5);
        textField.addKeyListener(this);
        return textField;
    }
    
    private JButton createPlusMinusButton(String label)
    {
        JButton button = new JButton(label);
        button.setMargin(new Insets(0,3,0,3));
        button.addActionListener(this);
        return button;
    }


    // ====================================================
    // Methods

    double getValue()
    {
        return this.value;
    }

    public JPanel getPanel()
    {
        return this.panel;
    }

    // ====================================================
    // Callbacks

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        if (evt.getSource() == minusButton)
        {
            // decrement the value by 1
            this.value = this.value - 1.0;
            textField.setText(doubleToString(this.value));
            fireValueChangeEvent();
        }
        else if (evt.getSource() == plusButton)
        {
            // increment the value by 1
            this.value = this.value + 1.0;
            textField.setText(doubleToString(this.value));
            fireValueChangeEvent();
        }
    }
    
    
    @Override
    public void keyTyped(KeyEvent evt)
    {
        if (evt.getSource() == textField)
        {
            processTextUpdate((JTextField) evt.getSource());
            fireValueChangeEvent();
            textField.requestFocus();
        }    
    }

    @Override
    public void keyPressed(KeyEvent evt)
    {
    }

    @Override
    public void keyReleased(KeyEvent evt)
    {
    }

    private void processTextUpdate(JTextField textField)
    {
        try
        {
            this.value = Double.parseDouble(textField.getText());
        }
        catch (NumberFormatException ex)
        {
            return;
        }
    }

    private String doubleToString(double value)
    {
        String pattern = "%." + nDigits + "f";
        return String.format(Locale.ENGLISH, pattern, value);
    }


    // ====================================================
    // Management of obervers
   
    public void addListener(Listener lst)
    {
        this.listeners.add(lst);
    }
    
    private void fireValueChangeEvent()
    {
        ValueChangeEvent evt = new ValueChangeEvent(this, this.value);
        for (Listener lst : listeners)
        {
            lst.valueChanged(evt);
        }
    }
    
    /**
     * An observer for value changes.
     * 
     * @author dlegland
     *
     */
    public static interface Listener
    {
        public void valueChanged(ValueChangeEvent evt);
    }
    
    public static class ValueChangeEvent
    {
     
        ChooseNumberWidget source;
        double newValue;
        
        public ValueChangeEvent(ChooseNumberWidget source, double newValue)
        {
            this.source = source;
            this.newValue = newValue;
        }

        public ChooseNumberWidget getSource()
        {
            return source;
        }

        public double getNewValue()
        {
            return newValue;
        }

    }
}
