/**
 * 
 */
package net.ijt.rotcrop.plugins;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * @author dlegland
 *
 */
public class GuiHelper
{
    public static final JPanel createOptionsPanel(String title)
    {
        JPanel panel = new JPanel();
        panel.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), BorderFactory.createTitledBorder(title)));
        panel.setAlignmentX(0.0f);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        return panel;
    }
    
    public static final void addInLine(JPanel panel, Component... comps)
    {
        addInLine(panel, FlowLayout.LEFT, comps);
    }
    
    public static final void addInLine(JPanel panel, int alignment, Component... comps)
    {
        JPanel rowPanel = new JPanel(new FlowLayout(alignment));
        rowPanel.setAlignmentX(0.0f);
        for (Component c : comps)
        {
            rowPanel.add(c);
        }
        panel.add(rowPanel);
    }
    
}
