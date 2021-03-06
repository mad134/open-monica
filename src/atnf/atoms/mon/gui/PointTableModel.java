//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.gui;

import atnf.atoms.mon.client.*;
import atnf.atoms.mon.*;
import atnf.atoms.time.*;

import javax.swing.table.AbstractTableModel;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Vector;
import java.io.PrintStream;

/**
 * @author David Brodrick
 * @version $Id: PointTableModel.java,v 1.7 2006/02/07 22:05:52 bro764 Exp $
 * @see DataMaintainer
 */
public class PointTableModel extends AbstractTableModel implements PointListener, TableCellRenderer {
  static {
    AbsTime.setTheirDefaultFormat(AbsTime.Format.UTC_STRING);
    RelTime.setTheirDefaultFormat(RelTime.Format.BRIEF);
  }

  /**
   * Names of all PointInteractions to be displayed in the table. A null entry in this
   * Vector is interpreted as indicating that we should display an empty row at that
   * position to improve the layout and readability of the table.
   */
  protected Vector<String> itsPoints = new Vector<String>();

  /** Names of sources selected for display in the table. */
  protected Vector<String> itsSources = new Vector<String>();

  /** Table containing all of the latest values in [source][point] ie [column][row] order. */
  protected PointData[][] itsValues = null;
  
  public PointTableModel() {
    super();
  }

  public PointTableModel(Vector<String> points, Vector<String> sources) {
    super();
    set(points, sources);
  }

  /**  */
  public Vector getPoints() {
    return itsPoints;
  }

  /**  */
  public Vector getSources() {
    return itsSources;
  }

  /**
   * Cease displaying any currently selected points and start displaying the points
   * contained in the specified page.
   */
  public void set(Vector<String> points, Vector<String> sources) {
    // First unsubscribe from the old points, if any
    if (itsPoints != null && itsPoints.size() > 0) {
      for (int i = 0; i < itsPoints.size(); i++) {
        String pname = (String) itsPoints.get(i);
        if (pname == null || pname.equals("")) {
          continue; // Blank row
        }
        for (int j = 0; j < itsSources.size(); j++) {
          DataMaintainer.unsubscribe(itsSources.get(j)+"."+pname, this);
        }
      }
    }

    // Record new settings
    itsPoints = points;
    itsSources = sources;
    if (itsSources!=null && itsPoints!=null) {
      itsValues = new PointData[itsSources.size()][itsPoints.size()];
    }
    fireTableStructureChanged();

    if (itsPoints != null && itsPoints.size() > 0) {
      // This stage might involve some slow network I/O.
      // Rather than hang the GUI, use a thread to do the work
      final PointTableModel themodel = this;
      Thread realWork = new Thread() {
        public void run() {
          // Build vector containing all point names
          // /This is pretty dumb since not all sources will have all points
          Vector<String> pnames = new Vector<String>();
          for (int i = 0; i < itsPoints.size(); i++) {
            String pname = itsPoints.get(i);
            if (pname == null || pname.equals("") || pname.equals("-")) {
              continue; // Blank row
            }
            for (int j = 0; j < itsSources.size(); j++) {
              pnames.add((String) itsSources.get(j) + "." + pname);
            }
          }

          // Subscribe to each of the points
          DataMaintainer.subscribe(pnames, themodel);

          // We've finished the slow network I/O, tell GUI to update
          Runnable tellSwing = new Runnable() {
            public void run() {
              // fireTableStructureChanged();
              // Make table redraw the rows so they get the point names, etc.
              // fireTableRowsUpdated(0,itsPoints.size()-1);
              fireTableDataChanged();
            }
          };
          try {
            SwingUtilities.invokeLater(tellSwing);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      };
      realWork.start();
    }
  }

  /** Try to come up with sensible column widths. */
  public void setSizes(JTable table, JScrollPane scroll) {
    if (itsPoints != null && itsPoints.size() > 0) {
      // Set sized based on number of rows
      Dimension prefsize = new Dimension(220 + 80 + 90 * itsSources.size(), 18 * (itsPoints.size() + 1));
      scroll.setPreferredSize(prefsize);
      scroll.setMaximumSize(prefsize);
      scroll.setMinimumSize(new Dimension(200, 100));

      // Set background colour for unused area - need to access the 'parent'
      Component parent = table.getParent();
      if (parent != null) {
        parent.setBackground(Color.lightGray);
        if (parent instanceof JComponent) {
          // possibly turned off elsewhere
          ((JComponent) parent).setOpaque(true);
        }
      }

      TableColumn column = table.getColumnModel().getColumn(getColumnCount() - 1);
      column.setPreferredWidth(80);
      column.setMaxWidth(120);
      column.setMinWidth(0);
      column = table.getColumnModel().getColumn(0);
      column.setPreferredWidth(220);
      column.setMinWidth(150);

      // fireTableStructureChanged();
    }
  }

  /**
   * Return the row in which the specified point is being displayed.
   * @return Row index or -1 if there is no row for the named point.
   */
  protected int getRowForPoint(String pname) {
    int res = -1;
    if (itsPoints != null) {
      for (int p = 0; p < itsPoints.size(); p++) {
        String thiss = (String) itsPoints.get(p);
        if (thiss != null && thiss.equals(pname)) {
          res = p;
          break;
        }
      }
    }
    return res;
  }

  /**
   * Return the monitor point structure for the point which is displayed in the specified
   * row. If no point is displayed in the specified row, null will be returned.
   * @param row The row to get the monitor point structure for.
   * @return The monitor point structure for the specified row, or null if no point is
   * displayed in the specified row.
   */
  public PointDescription getPointForRow(int row) {
    PointDescription res = null;
    if (row >= 0 && row < getRowCount()) {
      String pname = (String) itsPoints.get(row);
      if (pname != null && !pname.equals("")) {
        // Not all sources may have this point, so brute force it until
        // we find a source which has the required point. Pretty dumb.
        for (int s = 0; s < itsSources.size(); s++) {
          PointDescription temp = null;
          temp = PointDescription.getPoint(itsSources.get(s)+"."+pname);
          if (temp != null) {
            // We found the answer, record it and we're done!
            res = temp;
            break;
          }
        }
      }
    }
    return res;
  }

  /**
   * Return the monitor point structure for the point which is displayed in the specified
   * cell. If no point is displayed there, null will be returned.
   * @param row The row specification for the cell.
   * @param column The column specification for the cell
   * @return The monitor point structure for the specified cell, or null if no point is
   * displayed in the specified cell.
   */
  public PointDescription getPoint(int row, int column) {
    PointDescription res = null;
    if (row >= 0 && row < getRowCount() && column > 0 && column < getColumnCount() - 1) {
      String pname = (String) itsPoints.get(row);
      if (pname != null && !pname.equals("")) {
        res = PointDescription.getPoint(itsSources.get(column - 1) + "." + pname);
      }
    }
    return res;
  }

  /**
   * Return the column in which the specified source is being displayed.
   * @return Column index for the given source or -1 if there is no column for that
   * source.
   */
  protected int getColumnForSource(String source) {
    int res = -1;
    if (itsSources != null) {
      for (int s = 0; s < itsSources.size(); s++) {
        String thiss = (String) itsSources.get(s);
        if (thiss != null && thiss.equals(source)) {
          res = s + 1;
          break;
        }
      }
    }
    return res;
  }

  /**
   * Return the source displayed in the specified column. Will return -1 if there is no
   * source for the column.
   */
  protected String getSourceForColumn(int column) {
    String res = null;
    if (column - 1 >= 0 && column - 1 < itsSources.size()) {
      res = (String) itsSources.get(column - 1);
    }
    return res;
  }

  /**
   * Return the number of rows in the table. The number of rows will correspond to te
   * number of points we are to display: one point is displayed in each row. We may also
   * have blank rows used for making the table a bit easier to read.
   * @return Number of rows in the table.
   */
  public int getRowCount() {
    if (itsPoints == null) {
      return 0;
    } else {
      return itsPoints.size();
    }
  }

  /**
   * Return the number of columns in the table. This will correspond to the number of
   * information sources selected for display, plus one column for the point name and
   * another column for the units.
   * @return Number of columns in the table.
   */
  public int getColumnCount() {
    // Column for point name + one column for each source + one for units
    if (itsSources == null) {
      return 2;
    } else {
      return itsSources.size() + 2;
    }
  }

  public String getColumnName(int column) {
    String res = null;
    if (column == 0) {
      res = "Point";
    } else if (column == getColumnCount() - 1) {
      res = "Units";
    } else {
      res = getSourceForColumn(column);
    }
    return res;
  }

  public Object getValueAt(int row, int column) {
    Object res = null;
    String pname = (String) itsPoints.get(row);
    if (pname == null) {
      // The point for this row is null, leave the row blank for formatting
      return "";
    }

    if (column == 0) {
      // Point name was requested
      PointDescription pm = getPointForRow(row);
      if (pm != null) {
        res = pm.getLongDesc();
        if (res == null || res.equals("")) {
          // No useful name is defined, so substitute the points name
          res = pm.getName();
        }
      }
    } else if (column == getColumnCount() - 1) {
      // Units were requested
      res = "";
      PointDescription pm = getPointForRow(row);
      if (pm != null && pm.getUnits() != null) {
        res = new JLabel(pm.getUnits());
      }
    } else {
      column = column-1;
      PointDescription pm = getPointForRow(row);
      PointData pd = null;
      if (itsValues!=null && column<itsValues.length && row<itsValues[0].length) {
        pd = itsValues[column][row];
      } else {
        System.err.println("PointTableModel.onPointEvent: Cannot Get Value For [" + column + "][" + row + "]");
      }
      if (pd != null && pm != null) {
        if (pd.isValid()) {
          res = pd.getData();
        } else {
          res = new JLabel("?", SwingConstants.CENTER);
          ((JLabel) res).setToolTipText("Current Value Not Available");
          ((JLabel) res).setForeground(Color.lightGray);
          ((JLabel) res).setBackground(Color.white);
        }
      } else {
        res = null;// new JLabel("No Data");
      }
    }
    return res;
  }

  /** Return a useful tool tip for the specified cell. */
  public String getToolTip(int row, int column) {
    String res = null;
    if (column == 0) {
      PointDescription pm = getPointForRow(row);
      if (pm != null) {
        res = pm.getName();
      }
    } else if (column == getColumnCount() - 1) {
      PointDescription pm = getPointForRow(row);
      if (pm != null) {
        res = "Units for " + pm.getLongDesc();
      }
    } else {
      PointDescription pm = getPoint(row, column);
      if (pm != null) {
        String desc = pm.getLongDesc();
        if (desc == null || desc.equals("")) {
          // No useful name is defined, so substitute the points name
          desc = pm.getName();
        }
        res = desc + " for \"" + pm.getSource() + "\"";
      }
    }
    return res;
  }

  /**
   * Called whenever a new value is available for one of the points we are displaying in
   * the table.
   */
  public void onPointEvent(Object source, PointEvent evt) {
    if (!evt.isRaw()) {
      PointData newval = evt.getPointData();
      if (newval != null) {
        int row = getRowForPoint(newval.getNameOnly());
        int col = getColumnForSource(newval.getSource());
        if (row >= 0 && col >= 0) {
          if (itsValues!=null && (col-1)<itsValues.length && row<itsValues[0].length) {
            itsValues[col-1][row] = newval;
          } else {
            System.err.println("PointTableModel.onPointEvent: Cannot Insert Value at [" + col + "][" + row + "]");
          }
          fireTableCellUpdated(row, col);
        }
      }
    }
    // System.err.println("PointTableModel: Event!");
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
          int column) {
    Component res = null;
    if (value == null) {
      return null;
    }

    if (value instanceof Component) {
      res = (Component) value;
    } else {
      // System.err.println("Creating new Component!");
      res = new JLabel(value.toString());
    }

    PointDescription pm = getPoint(row, column);
    if (pm != null) {
      PointData pd = itsValues[column-1][row];
      if (pd != null) {
        long period = pm.getPeriod();
        long age = (new AbsTime().add(ClockErrorMonitor.getClockError())).getValue() - pd.getTimestamp().getValue();
        if (period != 0 && age > 2 * period) {
          // The point is old, so alter the foreground color
          res.setForeground(Color.lightGray);
        } else if (pd.getAlarm()) {
          // Highlight this cell
          res.setForeground(Color.red);
          if (res instanceof JComponent) {
            ((JComponent) res).setOpaque(true);
          }
          res.setBackground(Color.yellow);
        }
      }
    }

    // Make it a different colour if it has been clicked
    // but only if is a data display cell
    // if (hasFocus && column>0 && column<getColumnCount()-1) {
    // res.setForeground(Color.black);
    // }

    if (res instanceof JComponent && ((JComponent) res).getToolTipText() == null) {
      ((JComponent) res).setToolTipText(getToolTip(row, column));
    }
    return res;
  }

  /**
   * Dump current data to the given output stream. This is the mechanism through which
   * data can be exported to a file.
   * @param p The print stream to write the data to.
   */
  public synchronized void export(PrintStream p) {
    if (itsSources.size() == 0 || itsPoints.size() == 0) {
      p.println("#There is no data to be exported.");
      return;
    }

    // Print table header
    String line = "Point";
    for (int i = 0; i < itsSources.size(); i++) {
      line += ", " + (String) itsSources.get(i);
    }
    p.println(line);

    // Print table data
    for (int i = 0; i < itsPoints.size(); i++) {
      PointDescription pm = getPointForRow(i);
      if (pm == null) {
        continue; // Blank row
      }

      line = pm.getLongDesc();
      for (int j = 0; j < itsSources.size(); j++) {
        line += ", ";

        PointData pd = itsValues[j][i];
        if (pd == null || pd.getData() == null) {
          continue;
        }

        if (pd.isValid()) {
          line += pd.getData().toString();
        } else {
          line += "?";
        }
      }
      p.println(line);
    }
  }
}
