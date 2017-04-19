package net.openvpn.openvpn;

import android.util.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Iterator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class XMLRPC {
    public static final String DATETIME_FORMAT = "yyyyMMdd'T'HH:mm:ss";
    public static final String TAG_DATA = "data";
    public static final String TAG_MEMBER = "member";
    public static final String TAG_NAME = "name";
    public static final String TAG_VALUE = "value";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_BASE64 = "base64";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_DATE_TIME_ISO8601 = "dateTime.iso8601";
    public static final String TYPE_DOUBLE = "double";
    public static final String TYPE_I4 = "i4";
    public static final String TYPE_I8 = "i8";
    public static final String TYPE_INT = "int";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_STRUCT = "struct";
    private static final SimpleDateFormat dateFormat;

    public interface Serializable {
        Object getSerializable();
    }

    public static class Tag {
        static final String FAULT = "fault";
        static final String FAULT_CODE = "faultCode";
        static final String FAULT_STRING = "faultString";
        static final String LOG = "XMLRPC";
        static final String METHOD_CALL = "methodCall";
        static final String METHOD_NAME = "methodName";
        static final String METHOD_RESPONSE = "methodResponse";
        static final String PARAM = "param";
        static final String PARAMS = "params";
    }

    public static class XMLRPCException extends Exception {
        public XMLRPCException(Exception e) {
            super(e);
        }

        public XMLRPCException(String string) {
            super(string);
        }
    }

    public static class XMLRPCFault extends XMLRPCException {
        private int faultCode;
        private String faultString;

        public XMLRPCFault(String faultString, int faultCode) {
            super("XMLRPC Fault: " + faultString + " [code " + faultCode + "]");
            this.faultString = faultString;
            this.faultCode = faultCode;
        }

        public String getFaultString() {
            return this.faultString;
        }

        public int getFaultCode() {
            return this.faultCode;
        }
    }

    static {
        dateFormat = new SimpleDateFormat(DATETIME_FORMAT);
    }

    public static Object parse_response(XmlPullParser pullParser) throws XmlPullParserException, XMLRPCException, IOException {
        pullParser.nextTag();
        pullParser.require(2, null, "methodResponse");
        pullParser.nextTag();
        String tag = pullParser.getName();
        if (tag.equals("params")) {
            pullParser.nextTag();
            pullParser.require(2, null, "param");
            pullParser.nextTag();
            return deserialize(pullParser);
        } else if (tag.equals("fault")) {
            pullParser.nextTag();
            Map<String, Object> map = (Map) deserialize(pullParser);
            throw new XMLRPCFault((String) map.get("faultString"), ((Integer) map.get("faultCode")).intValue());
        } else {
            throw new XMLRPCException("Bad tag <" + tag + "> in XMLRPC response - neither <params> nor <fault>");
        }
    }

    public static void serialize(XmlSerializer serializer, Object object) throws IOException {
        if ((object instanceof Integer) || (object instanceof Short) || (object instanceof Byte)) {
            serializer.startTag(null, TYPE_I4).text(object.toString()).endTag(null, TYPE_I4);
        } else if (object instanceof Long) {
            serializer.startTag(null, TYPE_I8).text(object.toString()).endTag(null, TYPE_I8);
        } else if ((object instanceof Double) || (object instanceof Float)) {
            serializer.startTag(null, TYPE_DOUBLE).text(object.toString()).endTag(null, TYPE_DOUBLE);
        } else if (object instanceof Boolean) {
            serializer.startTag(null, TYPE_BOOLEAN).text(((Boolean) object).booleanValue() ? "1" : "0").endTag(null, TYPE_BOOLEAN);
        } else if (object instanceof String) {
            serializer.startTag(null, TYPE_STRING).text(object.toString()).endTag(null, TYPE_STRING);
        } else if ((object instanceof Date) || (object instanceof Calendar)) {
            serializer.startTag(null, TYPE_DATE_TIME_ISO8601).text(dateFormat.format(object)).endTag(null, TYPE_DATE_TIME_ISO8601);
        } else if (object instanceof byte[]) {
            serializer.startTag(null, TYPE_BASE64).text(Base64.encodeToString((byte[]) object, 2)).endTag(null, TYPE_BASE64);
        } else if (object instanceof List) {
            serializer.startTag(null, TYPE_ARRAY).startTag(null, TAG_DATA);
            for (Object o : (List) object) {
                serializer.startTag(null, TAG_VALUE);
                serialize(serializer, o);
                serializer.endTag(null, TAG_VALUE);
            }
            serializer.endTag(null, TAG_DATA).endTag(null, TYPE_ARRAY);
        } else if (object instanceof Object[]) {
            serializer.startTag(null, TYPE_ARRAY).startTag(null, TAG_DATA);
            Object[] objects = (Object[]) object;
            int i = 0;
            while (true) {
                int length = objects.length;
                Object o;
                if (i < length) {
                    o = objects[i];
                    serializer.startTag(null, TAG_VALUE);
                    serialize(serializer, o);
                    serializer.endTag(null, TAG_VALUE);
                    i++;
                } else {
                    serializer.endTag(null, TAG_DATA).endTag(null, TYPE_ARRAY);
                    return;
                }
            }
        } else if (object instanceof Map) 
        {
            serializer.startTag(null, TYPE_STRUCT);
            /*
            for (Entry<String, Object> entry : ((Map) object).entrySet()) {
                String key = (String) entry.getKey();
                Object value = entry.getValue();
                serializer.startTag(null, TAG_MEMBER);
                serializer.startTag(null, TAG_NAME).text(key).endTag(null, TAG_NAME);
                serializer.startTag(null, TAG_VALUE);
                serialize(serializer, value);
                serializer.endTag(null, TAG_VALUE);
                serializer.endTag(null, TAG_MEMBER);
            }
            */
						// guu self made it version
						try{
							Map<String,Object> _obj = (HashMap<String,Object>)object;
            	for (final Map.Entry<String, Object> entry : _obj.entrySet()) {
                final String s2 = entry.getKey();
                final Object value = entry.getValue();
                serializer.startTag(null, "member");
                serializer.startTag(null, "name").text(s2).endTag((String)null, TAG_NAME);
                serializer.startTag(null, "value");
                serialize(serializer, value);
                serializer.endTag(null, "value");
                serializer.endTag(null, "member");
            	}
						}
						catch(ClassCastException e){
								throw new IOException("Cannot serialize CastException ");
						}
						catch(IOException e)
						{
								
						}
            serializer.endTag(null, TYPE_STRUCT);
            
        } else if (object instanceof Serializable) {
            serialize(serializer, ((Serializable) object).getSerializable());
        } else {
            throw new IOException("Cannot serialize " + object);
        }
    }

    public static Object deserialize(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, null, TAG_VALUE);
        if (parser.isEmptyElementTag()) {
            return "";
        }
        Object obj;
        boolean hasType = true;
        String typeNodeName = null;
        try {
            parser.nextTag();
            typeNodeName = parser.getName();
            if (typeNodeName.equals(TAG_VALUE) && parser.getEventType() == 3) {
                return "";
            }
        } catch (XmlPullParserException e) {
            hasType = false;
        }
        if (hasType) {
            if (!typeNodeName.equals(TYPE_INT)) {
                if (!typeNodeName.equals(TYPE_I4)) {
                    if (typeNodeName.equals(TYPE_I8)) {
                        obj = Long.valueOf(Long.parseLong(parser.nextText()));
                    } else {
                        if (typeNodeName.equals(TYPE_DOUBLE)) {
                            obj = Double.valueOf(Double.parseDouble(parser.nextText()));
                        } else {
                            if (typeNodeName.equals(TYPE_BOOLEAN)) {
                                obj = parser.nextText().equals("1") ? Boolean.TRUE : Boolean.FALSE;
                            } else {
                                if (typeNodeName.equals(TYPE_STRING)) {
                                    obj = parser.nextText();
                                } else {
                                    if (typeNodeName.equals(TYPE_DATE_TIME_ISO8601)) {
                                        String value = parser.nextText();
                                        try {
                                            obj = dateFormat.parseObject(value);
                                        } catch (ParseException e2) {
                                            throw new IOException("Cannot deserialize dateTime " + value);
                                        }
                                    }
                                    if (typeNodeName.equals(TYPE_BASE64)) {
                                        BufferedReader reader = new BufferedReader(new StringReader(parser.nextText()));
                                        StringBuffer sb = new StringBuffer();
                                        while (true) {
                                            String line = reader.readLine();
                                            if (line == null) {
                                                break;
                                            }
                                            sb.append(line);
                                        }
                                        obj = Base64.decode(sb.toString(), 0);
                                    } else {
                                        if (typeNodeName.equals(TYPE_ARRAY)) {
                                            parser.nextTag();
                                            parser.require(2, null, TAG_DATA);
                                            parser.nextTag();
                                            List<Object> list = new ArrayList();
                                            while (parser.getName().equals(TAG_VALUE)) {
                                                list.add(deserialize(parser));
                                                parser.nextTag();
                                            }
                                            parser.require(3, null, TAG_DATA);
                                            parser.nextTag();
                                            parser.require(3, null, TYPE_ARRAY);
                                            obj = list.toArray();
                                        } else {
                                            if (typeNodeName.equals(TYPE_STRUCT)) {
                                                parser.nextTag();
                                                Map<String, Object> map = new HashMap();
                                                while (parser.getName().equals(TAG_MEMBER)) {
                                                    String memberName = null;
                                                    Object obj2 = null;
                                                    while (true) {
                                                        parser.nextTag();
                                                        String name = parser.getName();
                                                        if (!name.equals(TAG_NAME)) {
                                                            if (!name.equals(TAG_VALUE)) {
                                                                break;
                                                            }
                                                            obj2 = deserialize(parser);
                                                        } else {
                                                            memberName = parser.nextText();
                                                        }
                                                    }
                                                    if (!(memberName == null || obj2 == null)) {
                                                        map.put(memberName, obj2);
                                                    }
                                                    parser.require(3, null, TAG_MEMBER);
                                                    parser.nextTag();
                                                }
                                                parser.require(3, null, TYPE_STRUCT);
                                                Map<String, Object> obj3 = map;
                                            } else {
                                                throw new IOException("Cannot deserialize " + parser.getName());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            obj = Integer.valueOf(Integer.parseInt(parser.nextText()));
        } else {
            obj = parser.getText();
        }
        parser.nextTag();
        parser.require(3, null, TAG_VALUE);
        return obj;
    }
}
