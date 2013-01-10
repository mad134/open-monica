// **********************************************************************
//
// Copyright (c) 2003-2010 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

// Ice version 3.4.1

package atnf.atoms.mon.comms;

// <auto-generated>
//
// Generated from file `MoniCA.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>


public interface _MoniCAIceDel extends Ice._ObjectDel
{
    String[] getAllPointNames(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    PointDescriptionIce[] getPoints(String[] names, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    PointDescriptionIce[] getAllPoints(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    boolean addPoints(PointDescriptionIce[] newpoints, String username, String passwd, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    PointDataIce[][] getArchiveData(String[] names, long start, long end, long maxsamples, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    PointDataIce[] getData(String[] names, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    PointDataIce[] getBefore(String[] names, long t, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    PointDataIce[] getAfter(String[] names, long t, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    boolean setData(String[] names, PointDataIce[] values, String username, String passwd, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    String[] getAllSetups(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    boolean addSetup(String setup, String username, String passwd, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    AlarmIce[] getAllAlarms(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    AlarmIce[] getCurrentAlarms(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    boolean acknowledgeAlarms(String[] pointnames, boolean ack, String username, String passwd, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    boolean shelveAlarms(String[] pointnames, boolean shelve, String username, String passwd, java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    String[] getEncryptionInfo(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;

    long getCurrentTime(java.util.Map<String, String> __ctx)
        throws IceInternal.LocalExceptionWrapper;
}
