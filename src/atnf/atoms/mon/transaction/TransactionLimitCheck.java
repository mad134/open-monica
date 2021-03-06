//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package atnf.atoms.mon.transaction;

import java.util.Timer;
import java.util.TimerTask;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.*;
import atnf.atoms.time.AbsTime;

/**
 * This Transaction checks if a set of points are all within their defined <i>limits</i>. If the values for one or more points are
 * not currently available then no output will be generated unless there is an available point that is outside its limits in which
 * case we should clearly indicate an alarm condition even though some other points are unavailable.
 * <P>
 * The behaviour of this translation is controlled by the arguments given to it in the point definition: <br>
 * The check is made at a frequency determined by the first argument, which (for consistency although it is overkill) must be
 * specified in microseconds. <tt>eg, "10000000"</tt>. This should be the same as the overall update period declared for this
 * monitor point. <br>
 * Some times a monitor point might have just one or two spurious readings so the second argument specifies how many update cycles
 * the listened-to points must be outside their limits before we indicate a problem. <tt>eg, "3"</tt>. <br>
 * If the values are okay then the third argument string will be used for the output. <tt>eg, this might be "OK"</tt>. <br>
 * If one or more of the values it outside their nominated limits for a sufficient number of cycles then the fourth argument string
 * will be used as output. <tt>eg, "ALARM"</tt>. <br>
 * All other arguments are interpreted as the names of the points to be checked.
 * 
 * @author David Brodrick
 */
public class TransactionLimitCheck extends Transaction {
  /** String to use as output when all points are okay. */
  protected String itsOutput1 = null;

  /** String to use as output when one or more points are out of limits. */
  protected String itsOutput2 = null;

  /**
   * Holds the last few results of our analysis of the listened-to points. This is used for when more than one update must indicate
   * an out of limits state before we use the alternate output string. The size is determined by the second init argument. An entry
   * of false indicates that there is no reason for alarm while true indicates an alarm condition.
   */
  protected Boolean[] itsHistory = null;

  /** The names of the monitor points we must check. */
  protected String[] itsPoints = null;

  /** The timer used to trigger the periodic check. */
  /**
   * Timer used to trigger the periodic check. TODO: Using a single static instance has limited scaling potential. What would a
   * better scheme be?
   */
  protected static Timer theirTimer = new Timer();

  public TransactionLimitCheck(PointDescription parent, String[] args) {
    super(parent, args);
    setChannel("NONE"); // Set the channel type - not used for us

    if (args.length < 5) {
      throw new IllegalArgumentException("Requires at least 5 arguments");
    }

    itsHistory = new Boolean[Integer.parseInt(args[1])];
    itsOutput1 = args[2]; // String output for when data's okay
    itsOutput2 = args[3]; // String output for when data's not okay
    itsPoints = new String[args.length - 4];
    for (int i = 4; i < args.length; i++) {
      itsPoints[i - 4] = args[i];
      // If the point has $1 source name macro, then expand it
      if (itsPoints[i - 4].indexOf("$1") > -1) {
        itsPoints[i - 4] = MonitorUtils.replaceTok(itsPoints[i - 4], itsParent.getSource());
      }
    }
    long updatefreq = Long.parseLong(args[0]) / 1000;
    theirTimer.schedule(new CheckerTask(), updatefreq, updatefreq);
  }

  /** Check all of the points and fire a new result. */
  private class CheckerTask extends TimerTask {
    public void run() {
      PointData res = new PointData(itsParent.getFullName());

      // shuffle all the old values down
      for (int i = 0; i < itsHistory.length - 1; i++) {
        itsHistory[i] = itsHistory[i + 1];
      }
      // check the latest point values and store the summary
      itsHistory[itsHistory.length - 1] = doUpdate();

      // Can be declared "bad" only if whole history is bad
      boolean allbad = true;
      for (int i = 0; i < itsHistory.length; i++) {
        if (itsHistory[i] == null || itsHistory[i].booleanValue() == false) {
          allbad = false;
          break;
        }
      }

      if (allbad) {
        res.setData(itsOutput2);
      } else if (itsHistory[itsHistory.length - 1] != null && itsHistory[itsHistory.length - 1].booleanValue() == false) {
        res.setData(itsOutput1);
        // Otherwise leave data as null
      }

      // Fire the result
      PointEvent pe = new PointEvent(this, res, true);
      try {
        itsParent.firePointEvent(pe);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * Check each monitor point and return <tt>true</tt> if any are outside their limits, <tt>null</tt> if some points were
     * unavilable, or <tt>false</tt> if no points are outside their limits.
     */
    protected Boolean doUpdate() {
      Boolean[] vals = new Boolean[itsPoints.length];
      long now = (new AbsTime()).getValue();

      // Check the current values of each monitor point
      for (int i = 0; i < itsPoints.length; i++) {
        // Get point info and the latest data
        PointDescription pm = PointDescription.getPoint(itsPoints[i]);
        PointData pd = PointBuffer.getPointData(itsPoints[i]);
        if (pm == null || pd == null) {
          vals[i] = null;
          break;
        }

        // Don't use the data if it is too stale
        long age = now - pd.getTimestamp().getValue();
        if (pm.getPeriod() != 0 && age > 2 * pm.getPeriod()) {
          vals[i] = null;
          break;
        }

        // Check the value of this point and set result
        if (pd.getAlarm()) {
          vals[i] = new Boolean(true);
        } else {
          vals[i] = new Boolean(false);
        }
      }

      // Summarise the results
      Boolean res = new Boolean(false);
      // If any are null, then the result cannot be false (good)
      for (int i = 0; i < vals.length; i++) {
        if (vals[i] == null) {
          res = null;
          break;
        }
      }
      // However if any are outside of limits the result must be true (bad)
      for (int i = 0; i < vals.length; i++) {
        if (vals[i] != null && vals[i].booleanValue()) {
          res = new Boolean(true);
          break;
        }
      }

      return res;
    }
  }
}
