//
// Copyright (C) CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//

package atnf.atoms.mon.translation;

import atnf.atoms.mon.*;
import atnf.atoms.mon.util.MailSender;
import atnf.atoms.mon.util.MonitorUtils;

import java.lang.reflect.*;
import org.apache.log4j.Logger;

/**
 * Sends an email when the input changes value. The email is sent using the local hosts standard email transport.
 * 
 * <P>
 * The definition requires three or four string arguments:
 * <ul>
 * <li><b>Recipient:</b> The email address to send the email to, eg "fred@email.com".
 * <li><b>Sender:</b> Optional argument. The email address of the sender "mary@email.com". If no address is specified, the current
 * user's default address from the system will be used.
 * <li><b>Subject:</b> The subject line of the email, eg "Warning from MoniCA".
 * <li><b>Body:</b> The body text of the email, eg "Warning\nThe new value is $V".
 * </ul>
 * 
 * The subject and body templates can macros which will be substituted before the email is sent. These are documented in the
 * MonitorUtils.doSubstituion method.
 * 
 * @author David Brodrick
 * @mod Balt
 */
public class TranslationEmailOnChange extends Translation {
  /** The previous data value. */
  protected Object itsLastValue;

  /** The email recipient. */
  protected String itsRecipient;

  /** The email subject line template. */
  protected String itsSubject;

  /** The email body template. */
  protected String itsBody;

  /** The email sender. */
  protected String itsSender;

  /** Logger. */
  protected static Logger theirLogger = Logger.getLogger(TranslationEmailOnChange.class);

  public TranslationEmailOnChange(PointDescription parent, String[] init) {
    super(parent, init);
    if (init.length < 3) {
      throw new IllegalArgumentException("Requires at least three arguments");
    }
    if (init.length == 3) {
      itsRecipient = init[0];
      itsSender = "";
      itsSubject = init[1];
      itsBody = init[2].replaceAll("\\\\n", "\n").replaceAll("\\\\r", "\r");
    } else if (init.length == 4) {
      itsRecipient = init[0];
      itsSender = init[1];
      itsSubject = init[2];
      itsBody = init[3].replaceAll("\\\\n", "\n").replaceAll("\\\\r", "\r");
    }
  }

  /** Detects when the data value has changed. */
  protected boolean detectTrigger(PointData pd) {
    boolean res;
    Object newvalue = pd.getData();
    if (newvalue == null || itsLastValue == null) {
      // Don't email about null values
      res = false;
    } else if (newvalue instanceof Number && itsLastValue instanceof Number) {
      // Compare numbers
      if (((Number) newvalue).doubleValue() == ((Number) itsLastValue).doubleValue()) {
        res = false;
      } else {
        res = true;
      }
    } else {
      // Try to compare values using reflection
      try {
        Method equalsMethod = newvalue.getClass().getMethod("equals", new Class[] { Object.class });
        Object eq = equalsMethod.invoke(newvalue, new Object[] { itsLastValue });
        res = !((Boolean) eq).booleanValue();
      } catch (Exception e) {
        theirLogger.warn("(" + pd.getName() + "): " + e);
        res = false;
      }
    }
    return res;
  }

  /** Just return the input, but send an email if value changed. */
  public PointData translate(PointData data) {
    if (detectTrigger(data)) {
      String subject = MonitorUtils.doSubstitutions(itsSubject, data, itsParent);
      String body = MonitorUtils.doSubstitutions(itsBody, data, itsParent);
      if (itsSender == "") {
        MailSender.sendMail(itsRecipient, subject, body);
      } else {
        MailSender.sendMail(itsRecipient, itsSender, subject, body);
      }
    }
    itsLastValue = data.getData();
    return data;
  }
}
