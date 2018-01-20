package eu.germanorizzo.proj.mus.utils;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@SuppressWarnings("serial")
public class TableContent extends DefaultTableCellRenderer implements TableModel {
    public final List<Row> contents = Collections.synchronizedList(new ArrayList<>());
    private final Column[] columns;

    public TableContent(Column... columns) {
        this.columns = columns;
    }

    public void apply(JTable tbl) {
        tbl.setModel(this);
        tbl.setDefaultRenderer(Object.class, this);
        for (int i = 0; i < columns.length; i++) {
            Column col = columns[i];
            if (col.width < 0) {
                tbl.getColumnModel().getColumn(i).setMinWidth(0);
                tbl.getColumnModel().getColumn(i).setMaxWidth(Integer.MAX_VALUE);
            } else {
                tbl.getColumnModel().getColumn(i).setMinWidth(col.width);
                tbl.getColumnModel().getColumn(i).setMaxWidth(col.width);
            }
        }
    }

    public int getRowCount() {
        return contents.size();
    }

    public int getColumnCount() {
        return columns.length;
    }

    public String getColumnName(int columnIndex) {
        return columns[columnIndex].title;
    }

    public Class<?> getColumnClass(int columnIndex) {
        return columns[columnIndex].clas;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            return contents.get(rowIndex).getValue(columnIndex);
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    public void addTableModelListener(TableModelListener l) {
    }

    public void removeTableModelListener(TableModelListener l) {
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Component ret = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                column);
        ret.setBackground(contents.get(row).getColor());
        ret.setForeground(Color.BLACK);
        return ret;
    }

    public interface Row {
        Color getColor();

        Object getValue(int colNum);
    }

    public static class Column {
        final Class<?> clas;
        final String title;
        final int width;

        public Column(Class<?> clas, String title, int width) {
            this.clas = clas;
            this.title = title;
            this.width = width;
        }

        public Column(Class<?> clas, String title) {
            this(clas, title, -1);
        }
    }

    //public static class RowBean implements Row {
    //    private final Color color;
    //    private final Object[] columns;
    //    public RowBean(Color color, Object... columns) {
    //        this.color = color;
    //        this.columns = columns;
    //    }
    //    public RowBean(Object... columns) {
    //        this(Color.WHITE, columns);
    //    }
    //    public Color getColor() {
    //        return color;
    //    }
    //    public Object getValue(int colNum) {
    //        return columns[colNum];
    //    }
    //}

    public static class RowCalc implements Row {
        private static final Object[] NO_ARR = new Object[0];

        private final Supplier<Color> forColor;
        private final IntFunction<Object> forColumns;

        public RowCalc(IntFunction<Object> forColumns, Supplier<Color> forColor) {
            this.forColor = forColor;
            this.forColumns = forColumns;
        }

        public Color getColor() {
            if (forColor != null)
                return forColor.get();
            return Color.WHITE;
        }

        public Object getValue(int colNum) {
            if (forColumns != null)
                return forColumns.apply(colNum);
            return NO_ARR;
        }
    }
}
