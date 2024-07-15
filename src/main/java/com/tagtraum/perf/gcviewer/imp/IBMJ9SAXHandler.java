/*
 * =================================================
 * Copyright 2007 Justin Kilimnik (IBM UK)
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.perf.gcviewer.imp;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

import com.tagtraum.perf.gcviewer.model.AbstractGCEvent;
import com.tagtraum.perf.gcviewer.model.GCEvent;
import com.tagtraum.perf.gcviewer.model.GCModel;
import com.tagtraum.perf.gcviewer.model.GCResource;
import com.tagtraum.perf.gcviewer.util.NumberParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Simple (only for the -Xgcpolicy:optthruput output) IBMJ9 verbose GC reader.
 * Implemented as a SAX parser since XML based.
 *
 * @author <a href="mailto:justink@au1.ibm.com">Justin Kilimnik (IBM)</a>
 */
public class IBMJ9SAXHandler extends DefaultHandler {
    private GCModel model;
    private GCResource gcResource;
    private DateFormat cycleStartGCFormat5 = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US);
    private DateFormat cycleStartGCFormat6 = new SimpleDateFormat("MMM dd HH:mm:ss yyyy", Locale.US);
    private DateFormat current = cycleStartGCFormat5;
    protected AF currentAF;
    int currentTenured = 0; // 0 = none, 1=pre, 2=mid, 3=end
    private Date begin = null;

    public IBMJ9SAXHandler(GCResource gcResource, GCModel model) {
        this.gcResource = gcResource;
        this.model = model;
    }

    private Logger getLogger() {
        return gcResource.getLogger();
    }

    protected Date parseTime(String ts) throws ParseException {
        try {
            return current.parse(ts);
        }
        catch (ParseException e) {
            if (current != cycleStartGCFormat6) {

                current = cycleStartGCFormat6;
                return parseTime(ts);
            }
            throw e;
        }
    }

    private long parseLongAttribute(Attributes attrs, String attributeName) {
        String valueStr = attrs.getValue(attributeName);
        if (valueStr != null) {
            return Long.parseLong(valueStr);
        }
        return -1;
    }


    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) throws SAXException {
        try {
            handleAFElement(qName, attrs);
        } catch (ParseException e) {
            handleParseException(e, qName, attrs);
        } catch (NumberFormatException e) {
            handleNumberFormatException(e, qName);
        }
    }

    private void handleAFElement(String qName, Attributes attrs) throws ParseException {
        if (currentAF == null && "af".equals(qName)) {
            handleAFStart(attrs);
        } else if (currentAF != null) {
            switch (qName) {
                case "time":
                    handleTimeElement(attrs);
                    break;
                case "gc":
                    handleGCElement(attrs);
                    break;
                case "timesms":
                    handleTimesmsElement(attrs);
                    break;
                case "tenured":
                    handleTenuredElement(attrs);
                    break;
                case "soa":
                    handleSOAElement(attrs);
                    break;
                case "loa":
                    handleLOAElement(attrs);
                    break;
                default:
                    getLogger().warning("Unhandled element: " + qName);
                    break;
            }
        }
    }

    private void handleAFStart(Attributes attrs) throws ParseException {
        currentAF = new AF();
        currentAF.id = attrs.getValue("id");
        currentAF.type = attrs.getValue("type");
        currentAF.timestamp = parseTime(attrs.getValue("timestamp"));

        if (begin == null) {
            begin = currentAF.timestamp;
            currentAF.elapsedTime = 0L;
        } else {
            currentAF.elapsedTime = (currentAF.timestamp.getTime() - begin.getTime()) / 1000;
            System.out.println("ElapsedTime: " + currentAF.elapsedTime);
        }
    }

    private void handleTimeElement(Attributes attrs) {
        String totalMs = attrs.getValue("totalms");
        if (totalMs != null) {
            currentAF.totalTime = NumberParser.parseDouble(totalMs) / 1000;
        }
    }

    private void handleGCElement(Attributes attrs) {
        currentAF.gcType = attrs.getValue("type");
    }

    private void handleTimesmsElement(Attributes attrs) {
        String mark = attrs.getValue("mark");
        String sweep = attrs.getValue("sweep");
        if (mark != null && sweep != null) {
            currentAF.gcTimeMark = NumberParser.parseDouble(mark);
            currentAF.gcTimeSweep = NumberParser.parseDouble(sweep);
        }
    }

    private void handleTenuredElement(Attributes attrs) {
        currentTenured++;
        if (currentTenured == 1) {
            currentAF.initialFreeBytes = parseLongAttribute(attrs, "freebytes");
            currentAF.initialTotalBytes = parseLongAttribute(attrs, "totalbytes");
        } else if (currentTenured == 3) {
            currentAF.afterFreeBytes = parseLongAttribute(attrs, "freebytes");
            currentAF.afterTotalBytes = parseLongAttribute(attrs, "totalbytes");
        }
    }

    private void handleSOAElement(Attributes attrs) {
        if (currentTenured == 1) {
            currentAF.initialSOAFreeBytes = parseLongAttribute(attrs, "freebytes");
            currentAF.initialSOATotalBytes = parseLongAttribute(attrs, "totalbytes");
        } else if (currentTenured == 3) {
            currentAF.afterSOAFreeBytes = parseLongAttribute(attrs, "freebytes");
            currentAF.afterSOATotalBytes = parseLongAttribute(attrs, "totalbytes");
        }
    }

    private void handleLOAElement(Attributes attrs) {
        if (currentTenured == 1) {
            currentAF.initialLOAFreeBytes = parseLongAttribute(attrs, "freebytes");
            currentAF.initialLOATotalBytes = parseLongAttribute(attrs, "totalbytes");
        } else if (currentTenured == 3) {
            currentAF.afterLOAFreeBytes = parseLongAttribute(attrs, "freebytes");
            currentAF.afterLOATotalBytes = parseLongAttribute(attrs, "totalbytes");
        }
    }

    private void handleParseException(ParseException e, String qName, Attributes attrs) {
        if (current != cycleStartGCFormat6) {
            current = cycleStartGCFormat6;
            try {
                if (attrs != null) {
                    parseTime(attrs.getValue("timestamp"));
                } else {
                    getLogger().warning("Attributes are null in handleParseException");
                }
            } catch (ParseException parseException) {
                parseException.printStackTrace();
            }
        }
        e.printStackTrace();
    }


    private void handleNumberFormatException(NumberFormatException e, String qName) {
        getLogger().warning("Error parsing number in element: " + qName);
        e.printStackTrace();
    }


    public void endElement(String namespaceURI, String simpleName,
            String qualifiedName) throws SAXException {

        if ("af".equals(qualifiedName)) {
            System.out.println("In AF endElement!");
            if (currentAF != null) {
                GCEvent event = new GCEvent();
                if (!"tenured".equals(currentAF.type)) {
                    getLogger().warning("Unhandled AF type: " + currentAF.type);
                }
                if (!"global".equals(currentAF.gcType)) {
                    getLogger().warning("Different GC type: " + currentAF.gcType);
                }
                else {
                    event.setType(AbstractGCEvent.Type.FULL_GC);
                }
                if (currentAF.initialTotalBytes != -1
                        && currentAF.initialFreeBytes != -1) {
                    event.setPreUsed(currentAF.getPreUsedInKb());
                }

                if (currentAF.afterTotalBytes != -1
                        && currentAF.afterFreeBytes != -1) {
                    event.setPostUsed(currentAF.getPostUsedInKb());
                }

                if (currentAF.afterTotalBytes != -1) {
                    event.setTotal(currentAF.getTotalInKb());
                }

                // event.setTimestamp(currentAF.timestamp.getTime());
                event.setTimestamp(currentAF.elapsedTime);

                if (currentAF.totalTime >= 0) {
                    event.setPause(currentAF.totalTime);
                }

                if (currentAF.afterSOATotalBytes != -1
                        && currentAF.afterSOAFreeBytes != -1
                        && currentAF.initialSOAFreeBytes != -1
                        && currentAF.initialSOATotalBytes != -1) {

                    final GCEvent detailEvent = new GCEvent();
                    detailEvent.setTimestamp(currentAF.elapsedTime);
                    detailEvent.setType(AbstractGCEvent.Type.PS_YOUNG_GEN);
                    detailEvent.setPreUsed(currentAF.getPreUsedSoaInKb());
                    detailEvent.setPostUsed(currentAF.getPostUsedSoaInKb());
                    detailEvent.setTotal(currentAF.getTotalSoaInKb());
                    event.add(detailEvent);
                }

                if (currentAF.afterLOATotalBytes != -1
                        && currentAF.afterLOAFreeBytes != -1
                        && currentAF.initialLOAFreeBytes != -1
                        && currentAF.initialLOATotalBytes != -1) {

                    final GCEvent detailEvent = new GCEvent();
                    detailEvent.setTimestamp(currentAF.elapsedTime);
                    detailEvent.setType(AbstractGCEvent.Type.PS_OLD_GEN);
                    detailEvent.setPreUsed(currentAF.getPreUsedLoaInKb());
                    detailEvent.setPostUsed(currentAF.getPostUsedLoaInKb());
                    detailEvent.setTotal(currentAF.getTotalLoaInKb());
                    event.add(detailEvent);
                }

                model.add(event);
                currentTenured = 0;
                currentAF = null;
            }
            else {
                getLogger().warning("Found end <af> tag with no begin tag");
            }

        }
    }

}

/**
 * Holder of GC information for standard Allocation Failures
 */
class AF {
    String type;
    String id;
    Date timestamp;
    long elapsedTime;
    double intervalms = -1;
    long minRequestedBytes = -1;
    double timeExclusiveAccessMs = -1;
    long initialFreeBytes = -1;
    long initialTotalBytes = -1;
    long initialSOAFreeBytes = -1;
    long initialSOATotalBytes = -1;
    long initialLOAFreeBytes = -1;
    long initialLOATotalBytes = -1;
    long afterFreeBytes = -1;
    long afterTotalBytes = -1;
    long afterSOAFreeBytes = -1;
    long afterSOATotalBytes = -1;
    long afterLOAFreeBytes = -1;
    long afterLOATotalBytes = -1;
    String gcType;
    double gcIntervalms = -1;
    int gcSoftRefsCleared = -1;
    int gcWeakRefsCleared = -1;
    int gcPhantomRefsCleared = -1;
    double gcTimeMark = -1;
    double gcTimeSweep = -1;
    double gcTimeCompact = -1;
    double gcTime = -1;
    double totalTime = -1;

    public int getPreUsedInKb() {
        return (int) ((initialTotalBytes - initialFreeBytes) / 1024);
    }

    public int getPostUsedInKb() {
        return (int) ((afterTotalBytes - afterFreeBytes) / 1024);
    }

    public int getTotalInKb() {
        return (int) (afterTotalBytes / 1024);
    }

    public int getPreUsedSoaInKb() {
        return (int) ((initialSOATotalBytes - initialSOAFreeBytes) / 1024);
    }

    public int getPostUsedSoaInKb() {
        return (int) ((afterSOATotalBytes - afterSOAFreeBytes) / 1024);
    }

    public int getTotalSoaInKb() {
        return (int) (afterSOATotalBytes / 1024);
    }

    public int getPreUsedLoaInKb() {
        return (int) ((initialLOATotalBytes - initialLOAFreeBytes) / 1024);
    }

    public int getPostUsedLoaInKb() {
        return (int) ((afterLOATotalBytes - afterLOAFreeBytes) / 1024);
    }

    public int getTotalLoaInKb() {
        return (int) (afterLOATotalBytes / 1024);
    }
}
