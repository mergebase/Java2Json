/*
 *  Copyright (C) 2018, MergeBase Software Incorporated ("MergeBase")
 *  of Coquitlam, BC, Canada - https://mergebase.com/
 *  All rights reserved.
 *
 *  MergeBase licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.mergebase;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Java 1.1 compatible utility for converting back and forth between
 * java objects (Hashtable, Vector, String, Number, Boolean, null) and JSON.
 *
 * Note: since Hashtable cannot store null values, we store a pre-constructed
 * NoSuchElementException object in the map instead.  Ha ha.  Look for
 * Java1Json.NULL_OBJ in results. Its getMessage() returns the String "null".
 */
public class Java1Json {

    public static void main(String[] args) throws Exception {
        FileInputStream fin = new FileInputStream(args[0]);
        InputStreamReader isr = new InputStreamReader(fin, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        String line;
        StringBuffer buf = new StringBuffer(1024);
        while ((line = br.readLine()) != null) {
            buf.append(line).append('\n');
        }
        Object o = parse(buf.toString());
        System.out.write(format(o).getBytes("UTF-8"));
    }

    private int pos;
    private char[] json;

    private Java1Json(int pos, char[] json) {
        this.pos = pos;
        this.json = json;
    }

    public final static Object NULL_OBJ = new NoSuchElementException("null");

    private final static Long ZERO = Long.valueOf("0");
    private final static int MAP = 0;
    private final static int LIST = 1;
    private final static int STRING = 2;
    private final static int NUMBER = 3;
    private final static int BOOLEAN = 5;
    private final static int NULL = 6;
    private final static int MODE_WHITESPACE = -1;
    private final static int MODE_NORMAL = 0;
    private final static int MODE_BACKSLASH = 1;

    /**
     * Converts a String of JSON into a Java representation,
     * parsing the result into a structure of nested
     * Hashtable, Vector, Boolean, Long, Double, String and null objects.
     *
     * Note: since Hashtable cannot store null values, we store a pre-constructed
     * NoSuchElementException object in the map instead.  Ha ha.  Look for
     * Java1Json.NULL_OBJ in results. Its getMessage() returns the String "null".
     *
     * @param json String to parse
     * @return A Java representation of the parsed JSON String
     * based on java.util.Hashtable, java.util.Vector, java.lang.Boolean,
     * java.lang.Number, java.lang.String and null, as well as
     * Java1Json.NULL_OBJ to denote null values in any returned Hashtables.
     */
    public static Object parse(String json) {
        char[] c = json.toCharArray();
        Java1Json p = new Java1Json(0, c);

        try {
            int type = nextObject(p);
            Object o = parseObject(type, p);
            finalWhitespace(p);
            return o;

        } catch (RuntimeException re) {
            int charsLeft = c.length - p.pos;
            if (p.pos > 10) {
                // System.out.println("NEAR: [" + new String(c, p.pos - 10, Math.min(10 + charsLeft, 20)));
            } else {
                // System.out.println("NEAR: [" + new String(c, 0, Math.min(10 + charsLeft, Math.min(20, c.length))));
            }
            throw re;
        }
    }

    /**
     * Formats a Java object into a JSON String.
     * <p>
     * Expects the Java object to be a java.util.Hashtable, java.util.Vector,
     * java.lang.String, java.lang.Number, java.lang.Boolean, or null, or
     * nested structure of the above.
     *
     * Note: to store a "null" as a Hashtable value, please use
     * Java1Json.NULL_OBJ.
     *
     * All other object types cause a RuntimeException to be thrown.
     *
     * @param o Java object to convert into a JSON String.
     * @return a valid JSON String
     */
    public static String format(Object o) {
        StringBuffer buf = new StringBuffer(1024);
        prettyPrint(o, 0, buf);
        String s = buf.toString();
        if (o instanceof Hashtable) {
            return "{" + s + "\n}\n";
        } else if (o instanceof Vector) {
            return "[" + s + "\n]\n";
        } else {
            return s;
        }
    }

    private static StringBuffer indent(int level) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < level; i++) {
            buf.append("  ");
        }
        return buf;
    }

    private static Object parseObject(int type, Java1Json p) {
        switch (type) {
            case MAP:
                Hashtable m = new Hashtable();
                while (hasNextItem(p, '}')) {
                    String key = nextString(p);
                    nextChar(p, ':');
                    type = nextObject(p);
                    Object obj = parseObject(type, p);
                    m.put(key, obj);
                }
                return m;

            case LIST:
                Vector l = new Vector();
                while (hasNextItem(p, ']')) {
                    type = nextObject(p);
                    Object obj = parseObject(type, p);
                    l.addElement(obj);
                }
                return l;

            case STRING:
                return nextString(p);

            case NUMBER:
                return nextNumber(p);

            case BOOLEAN:
                return nextBoolean(p);

            case NULL:
                return nextNull(p);

            default:
                throw new RuntimeException("invalid type: " + type);
        }
    }

    private static boolean hasNextItem(Java1Json p, char closingBracket) {
        char prev = p.json[p.pos - 1];
        boolean isMap = closingBracket == '}';

        boolean nextCommaExists = nextChar(p, ',', false);
        if (!nextCommaExists) {
            p.pos--;
        }

        char c = p.json[p.pos];
        if (c == closingBracket) {
            p.pos++;
            return false;
        } else if (nextCommaExists) {
            return true;
        } else {
            if (isMap && prev == '{') {
                return true;
            } else if (!isMap && prev == '[') {
                return true;
            }
            throw new RuntimeException("expected whitespace or comma or " + closingBracket + " but found: " + c);
        }
    }

    private static int nextObject(Java1Json p) {
        for (int i = p.pos; i < p.json.length; i++) {
            p.pos++;
            char c = p.json[i];

            if (!isWhitespace(c)) {
                if (c == '"') {
                    p.pos--;
                    return STRING;
                } else if (c == '{') {
                    return MAP;
                } else if (c == '[') {
                    return LIST;
                } else if (c == '-' || (c >= '0' && c <= '9')) {
                    p.pos--;
                    return NUMBER;
                } else if (c == 'n') {
                    p.pos--;
                    return NULL;
                } else if (c == 't' || c == 'f') {
                    p.pos--;
                    return BOOLEAN;
                } else {
                    throw new RuntimeException("Expected whitespace or JSON literal, but got: " + c);
                }
            }
        }
        return -1; // there is no next object, so we're done
    }

    private static void finalWhitespace(Java1Json p) {
        for (int i = p.pos; i < p.json.length; i++) {
            p.pos++;
            char c = p.json[i];
            if (!isWhitespace(c)) {
                throw new RuntimeException("Expected whitespace or EOF but got: " + c);
            }
        }
        return;
    }

    private static boolean nextChar(Java1Json p, char charToFind) {
        return nextChar(p, charToFind, true);
    }

    private static boolean nextChar(Java1Json p, char charToFind, boolean doThrow) {
        for (int i = p.pos; i < p.json.length; i++) {
            p.pos++;
            char c = p.json[i];

            if (!isWhitespace(c)) {
                if (c == charToFind) {
                    return true;
                } else {
                    if (doThrow) {
                        throw new RuntimeException("Expected whitespace or " + charToFind + " but got: " + c);
                    } else {
                        return false;
                    }
                }
            }
        }
        throw new RuntimeException("Never found " + charToFind);
    }

    private static Object nextNull(Java1Json p) {
        char c = p.json[p.pos++];
        try {
            if (c == 'n') {
                c = p.json[p.pos++];
                if (c == 'u') {
                    c = p.json[p.pos++];
                    if (c == 'l') {
                        c = p.json[p.pos++];
                        if (c == 'l') {
                            return NULL_OBJ;
                        }
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new RuntimeException("expected null literal but ran of out string to parse");
        }
        throw new RuntimeException("expected null literal but ran into bad character: " + c);
    }

    private static Boolean nextBoolean(Java1Json p) {
        char c = p.json[p.pos++];
        try {
            if (c == 't') {
                c = p.json[p.pos++];
                if (c == 'r') {
                    c = p.json[p.pos++];
                    if (c == 'u') {
                        c = p.json[p.pos++];
                        if (c == 'e') {
                            return Boolean.TRUE;
                        }
                    }
                }
            } else if (c == 'f') {
                c = p.json[p.pos++];
                if (c == 'a') {
                    c = p.json[p.pos++];
                    if (c == 'l') {
                        c = p.json[p.pos++];
                        if (c == 's') {
                            c = p.json[p.pos++];
                            if (c == 'e') {
                                return Boolean.FALSE;
                            }
                        }
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new RuntimeException("expected true/false literal but ran of out string to parse");
        }
        throw new RuntimeException("expected true/false literal but ran into bad character: " + c);
    }

    private static Number nextNumber(Java1Json p) {
        StringBuffer buf = new StringBuffer();
        for (int i = p.pos; i < p.json.length; i++) {
            p.pos++;
            char c = p.json[i];
            if (isWhitespace(c) || c == ',' || c == '}' || c == ']') {
                p.pos--;
                break;
            } else if (c == '-' || c == '+' || c == 'e' || c == 'E' || c == '.' || (c >= '0' && c <= '9')) {
                buf.append(c);
            } else {
                throw new RuntimeException("expected number but got: " + c);
            }
        }

        String s = buf.toString();
        char char0 = s.length() > 0 ? s.charAt(0) : '_';
        if (char0 == '+') {
            throw new RuntimeException("number literal cannot start with plus: " + s);
        } else if ("-".equals(s)) {
            throw new RuntimeException("number literal cannot be negative sign by itself");
        }
        boolean isNegative = char0 == '-';

        if (isNegative) {
            s = s.substring(1);
        }

        if ("0".equals(s)) {
            return ZERO;
        }

        if (s.startsWith(".")) {
            throw new RuntimeException("number literal cannot start with decimal point: " + s);
        }
        if (!s.startsWith("0.") && !s.startsWith("0e") && !s.startsWith("0E")) {
            if (s.startsWith("0")) {
                throw new RuntimeException("number literal cannot have leading zero: " + s);
            }
        }

        if (contains(s, ".e") || contains(s, ".E")) {
            throw new RuntimeException("number literal invalid exponential: " + s);
        }

        if (s.endsWith("e") || s.endsWith("E") || s.endsWith("+") || s.endsWith("-") || s.endsWith(".")) {
            throw new RuntimeException("number literal cannot end with [eE+-.] " + s);
        }

        int[] charCounts = charCounts(s);
        int periods = charCounts[0];
        int minuses = charCounts[1];
        int plusses = charCounts[2];
        int eTotal = charCounts[3];
        int plussesAndMinuses = plusses + minuses;

        if (plussesAndMinuses > 0) {
            if (plussesAndMinuses > 1) {
                throw new RuntimeException("invalid number literal - too many plusses/minuses: " + s);
            } else {
                boolean isValidPlus = false;
                boolean isValidMinus = minuses > 0 && (contains(s, "e-") || contains(s, "E-"));
                if (!isValidMinus) {
                    isValidPlus = plusses > 0 && (contains(s, "e+") || contains(s, "E+"));
                }
                if (!isValidPlus && !isValidMinus) {
                    throw new RuntimeException("invalid number literal: " + s);
                }
            }
        }

        if (periods > 1 || eTotal > 1) {
            throw new RuntimeException("invalid number literal: " + s);
        }

        if (isNegative) {
            s = "-" + s;
        }
        if (periods == 1 || eTotal == 1) {
            return new Double(s);
        } else {
            try {
                return new Long(s);
            } catch (NumberFormatException nfe) {
                return new Double(s);
            }
        }
    }


    private static int[] charCounts(String s) {

        // periods, dashes, plusses, lowerOrUpperEs
        int[] counts = {0, 0, 0, 0};

        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '.':
                    counts[0]++;
                    break;
                case '-':
                    counts[1]++;
                    break;
                case '+':
                    counts[2]++;
                    break;
                case 'E':
                case 'e':
                    counts[3]++;
                    break;
                default:
                    break;
            }
        }
        return counts;
    }

    private static String nextString(Java1Json p) {
        int mode = MODE_WHITESPACE;
        StringBuffer buf = new StringBuffer();
        for (int i = p.pos; i < p.json.length; i++) {
            p.pos++;
            char c = p.json[i];
            switch (mode) {
                case MODE_WHITESPACE:
                    if (c == '"') {
                        mode = MODE_NORMAL;
                    } else if (!isWhitespace(c)) {
                        throw new RuntimeException("json expecting double-quote: " + c);
                    }
                    break;
                case MODE_NORMAL:
                    if (c == '\\') {
                        mode = MODE_BACKSLASH;
                    } else if (c == '"') {
                        return buf.toString();
                    } else {
                        if (Character.isISOControl(c)) {
                            StringBuffer hex = new StringBuffer(Integer.toHexString(c));
                            if ("7f".equalsIgnoreCase(hex.toString())) {
                                buf.append(c);
                            } else {
                                for (int j = hex.length(); j < 4; j++) {
                                    hex.insert(0, "0");
                                }
                                throw new RuntimeException("control characters in string literal must be escaped: \\u" + hex);
                            }
                        } else if (c == '\b' || c == '\f' || c == '\n' || c == '\r' || c == '\t') {
                            throw new RuntimeException("json string literal invalid character: " + c);
                        } else {
                            buf.append(c);
                        }
                    }
                    break;
                case MODE_BACKSLASH:
                    switch (c) {
                        case '/':
                            buf.append('/');
                            break;
                        case 'b':
                            buf.append('\b');
                            break;
                        case 'f':
                            buf.append('\f');
                            break;
                        case 'n':
                            buf.append('\n');
                            break;
                        case 'r':
                            buf.append('\r');
                            break;
                        case 't':
                            buf.append('\t');
                            break;
                        case '"':
                            buf.append('"');
                            break;
                        case '\\':
                            buf.append('\\');
                            break;
                        case 'u':
                            StringBuffer hex = new StringBuffer();
                            for (int j = 0; j < 4; j++) {
                                try {
                                    char hexChar = p.json[p.pos++];
                                    if (isHex(hexChar)) {
                                        hex.append(hexChar);
                                    } else {
                                        throw new RuntimeException("invalid \\u encoded character (must be hex): " + hexChar);
                                    }
                                } catch (ArrayIndexOutOfBoundsException aioobe) {
                                    throw new RuntimeException("\\u encoded literal ran out of string to parse");
                                }
                            }
                            buf.append((char) Integer.parseInt(hex.toString(), 16));
                            i += 4;
                            break;
                        default:
                            throw new RuntimeException("invalid backslash protected character: " + c);
                    }
                    mode = MODE_NORMAL;
                    break;
            }
        }
        throw new RuntimeException("never found literal string terminator \"");
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static boolean contains(String string, String thing) {
        return string.indexOf(thing) >= 0;
    }

    private static StringBuffer prettyPrint(final Object obj, final int level, final StringBuffer buf) {
        Enumeration en;
        if (obj instanceof Hashtable) {
            Hashtable m = (Hashtable) obj;
            en = m.keys();
        } else if (obj instanceof Vector) {
            Vector l = (Vector) obj;
            en = l.elements();
        } else {
            Vector l = new Vector();
            l.addElement(obj);
            en = l.elements();
        }

        while (en.hasMoreElements()) {
            Object o = en.nextElement();
            Object val = o;
            buf.append('\n').append(indent(level));
            if (obj instanceof Hashtable) {
                Hashtable m = (Hashtable) obj;
                String key = (String) o;
                buf.append('"');
                jsonSafe(key, buf);
                buf.append('"').append(" : ");
                val = m.get(key);
            }

            if (val == null || val == NULL_OBJ || val instanceof Boolean || val instanceof Number) {
                jsonSafe(val, buf);
            } else if (val instanceof Vector) {
                buf.append('[');
                int lenBefore = buf.length();
                prettyPrint(val, level + 1, buf);
                int lenAfter = buf.length();
                if (lenBefore < lenAfter) {
                    buf.append('\n');
                    buf.append(indent(level));
                }
                buf.append(']');
            } else if (val instanceof Hashtable) {
                buf.append('{');
                int lenBefore = buf.length();
                prettyPrint(val, level + 1, buf);
                int lenAfter = buf.length();
                if (lenBefore < lenAfter) {
                    buf.append('\n');
                    buf.append(indent(level));
                }
                buf.append('}');
            } else if (val instanceof String) {
                buf.append('"');
                jsonSafe(val, buf);
                buf.append('"');
            } else {
                throw new RuntimeException("can only format Hashtable|Vector|String|Number|Boolean|null into JSON. Wrong type: " + o.getClass());
            }

            if (en.hasMoreElements()) {
                buf.append(", ");
            }
        }
        return buf;
    }

    private static void jsonSafe(Object o, StringBuffer buf) {
        final String s;
        if (o == null || o == NULL_OBJ) {
            buf.append("null");
            return;
        } else if (o instanceof Boolean || o instanceof Number) {
            String val = o.toString();
            if ("Infinity".equals(val)) {
                val = "1e99999";
            } else if ("-Infinity".equals(val)) {
                val = "-1e99999";
            }
            buf.append(val);
            return;
        } else if (o instanceof Hashtable || o instanceof Vector) {
            throw new RuntimeException("cannot make Hashtable or Vector into json string literal: " + o);
        } else if (o instanceof String) {
            s = (String) o;
        } else {
            s = o.toString();
        }

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\b':
                    buf.append("\\b");
                    break;
                case '\f':
                    buf.append("\\f");
                    break;
                case '\n':
                    buf.append("\\n");
                    break;
                case '\r':
                    buf.append("\\r");
                    break;
                case '\t':
                    buf.append("\\t");
                    break;
                case '/':
                    buf.append("\\/");
                    break;
                case '\\':
                    buf.append("\\\\");
                    break;
                case '"':
                    buf.append("\\\"");
                    break;
                default:
                    // We're not interested in control characters U+0000 to U+001F aside from
                    // the allowed ones above.
                    if (Character.isISOControl(c)) {
                        String hex = Integer.toHexString(c);
                        buf.append("\\u");
                        for (int j = hex.length(); j < 4; j++) {
                            buf.append('0');
                        }
                        buf.append(hex);
                    } else {
                        buf.append(c);
                    }
                    break;
            }
        }
    }
}
