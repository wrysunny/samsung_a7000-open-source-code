package com.sec.android.app.dns.radioepg;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sec.android.app.dns.radioepg.PiData.Programme;
import com.sec.android.app.dns.radioepg.PiData.Programme.Multimedia;

//import com.sec.android.app.dns.LogDns;

public class PiDataHandler extends DefaultHandler {
    // private static final String TAG = "PIDataHandler";
    private PiData mData = null;
    private String mElementValue = null;
    private Programme mProgramme = null;
    private boolean mSchemaLocationOn = false;
    private boolean mSchemaMediaDescriptionOn = false;
    private boolean mSchemaProgrammeOn = false;
    private boolean mSchemaScheduleOn = false;

    /** * This is called to get the tags value **/
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String v = new String(ch, start, length);
        v = v.trim();
        if (!v.isEmpty()) {
            mElementValue = v;
        }
    }

    /** * This will be called when the tags of the XML end. **/
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (localName.equalsIgnoreCase("schedule")) {
            mSchemaScheduleOn = false;
        } else if (mSchemaScheduleOn) {
            if (localName.equalsIgnoreCase("programme")) {
                mData.addProgramme(mProgramme);
                mSchemaProgrammeOn = false;
                mProgramme = null;
            } else if (mSchemaProgrammeOn) {
                if (localName.equalsIgnoreCase("location")) {
                    mSchemaLocationOn = false;
                } else if (localName.equalsIgnoreCase("mediumName")) {
                    mProgramme.setMediumName(mElementValue);
                } else if (localName.equalsIgnoreCase("longName")) {
                    mProgramme.setLongName(mElementValue);
                } else if (localName.equalsIgnoreCase("mediaDescription")) {
                    mSchemaMediaDescriptionOn = false;
                } else if (mSchemaMediaDescriptionOn) {
                    if (localName.equalsIgnoreCase("longDescription")) {
                        mProgramme.setDescription(mElementValue);
                    }
                }
            }
        }
    }

    public PiData getData() {
        return mData;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if (localName.equalsIgnoreCase("schedule")) {
            mSchemaScheduleOn = true;
            mData = new PiData();
        } else if (mSchemaScheduleOn && localName.equalsIgnoreCase("programme")) {
            mSchemaProgrammeOn = true;
            mProgramme = new Programme();
        } else if (mSchemaProgrammeOn && localName.equalsIgnoreCase("location")) {
            mSchemaLocationOn = true;
        } else if (mSchemaLocationOn && localName.equalsIgnoreCase("time")) {
            int size = attributes.getLength();
            String name;
            String value;
            for (int i = 0; i < size; i++) {
                name = attributes.getQName(i);
                value = attributes.getValue(i);
                if (name.equalsIgnoreCase("time")) {
                    mProgramme.setTime(value);
                } else if (name.equalsIgnoreCase("duration")) {
                    mProgramme.setDuration(value);
                } else if (name.equalsIgnoreCase("actualTime")) {
                    mProgramme.setActualTime(value);
                } else if (name.equalsIgnoreCase("actualDuration")) {
                    mProgramme.setActualDuration(value);
                }
            }
        } else if (mSchemaProgrammeOn && localName.equalsIgnoreCase("mediaDescription")) {
            mSchemaMediaDescriptionOn = true;
        } else if (mSchemaMediaDescriptionOn && localName.equalsIgnoreCase("multimedia")) {
            Multimedia m = new Multimedia();
            int size = attributes.getLength();
            String name;
            String value;
            for (int i = 0; i < size; i++) {
                name = attributes.getQName(i);
                value = attributes.getValue(i);
                if (name.equalsIgnoreCase("url")) {
                    m.setUrl(value);
                } else if (name.equalsIgnoreCase("type")) {
                    m.setType(value);
                } else if (name.equalsIgnoreCase("height")) {
                    m.setHeight(value);
                } else if (name.equalsIgnoreCase("width")) {
                    m.setWidth(value);
                }
            }
            mProgramme.addMultimedia(m);
        }
    }
}
