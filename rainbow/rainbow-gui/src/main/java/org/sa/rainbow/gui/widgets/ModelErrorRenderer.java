package org.sa.rainbow.gui.widgets;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.sa.rainbow.gui.RainbowWindow;

public class ModelErrorRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
	private static Color m_red = RainbowWindow.bleach(Color.red, 0.75);

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		TableModel tm = table.getModel();
		if (!"".equals(tm.getValueAt(row, 4))) {
			c.setBackground(m_red);
		}
		else if (c.getBackground() == m_red) c.setBackground(Color.WHITE);
		return c;
	}
}