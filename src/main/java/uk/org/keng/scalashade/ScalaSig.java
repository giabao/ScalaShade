/*
 * Copyright 2015 Kevin Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.keng.scalashade;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Partial signature decoding, modification & encoding, reversed from
 * https://github.com/scala/scala/blob/2.11.x/src/scalap/scala/tools/scalap/scalax/rules/scalasig/ScalaSig.scala
 * <p/>
 * This deliberately decodes the minimum required to allow path replacement to avoid unwanted dependencies on the
 * format being used. Briefly the format is constructed as a table of entries. The key entry is an 'EXT_MOD_CLASS_REF_ID'
 * which joins together a term name (a string) to an optional parent 'EXT_MOD_CLASS_REF_ID'. Thus each 'EXT_MOD_CLASS_REF_ID'
 * defines and absolute namespace of 1..n components depending on how many parents exist.
 * <p/>
 * To change a namespace we locate the correct 'EXT_MOD_CLASS_REF_ID' and replace its term name and parent (recursively) by
 * creating new entries at the end of the list (to avoid disrupting any other dependencies that might exist) as needed.
 */


/**
 * Container for ScalaSignature data.
 */
class ScalaSig {

    private final int _majorVersion;
    private final int _minorVersion;
    private final Table _table;

    /**
     * Construct a new signature, use {@link #parse(byte[])} to create
     *
     * @param majorVersion major version
     * @param minorVersion minor version
     * @param table        table of entries in signature
     */
    private ScalaSig(int majorVersion, int minorVersion, Table table) {
        _majorVersion = majorVersion;
        _minorVersion = minorVersion;
        _table = table;
    }

    /**
     * Parse a signature from a byte stream
     *
     * @param in input bytes to parse
     * @return the decoded signature data
     * @throws CtxException
     */
    static ScalaSig parse(byte[] in) throws CtxException {
        ByteArrayInputStream bis = new ByteArrayInputStream(in);

        // Pull version info & check OK
        int major = Nat.read(bis);
        int minor = Nat.read(bis);
        if (major!=5 || minor!=0) {
            throw new CtxException("Unexpected signature version found: "+major+"."+minor);
        }

        // Pull table
        int tblEntries = Nat.read(bis);
        Table table = new Table();
        for (int e = 0; e < tblEntries; e++) {
            int type = bis.read();
            int size = Nat.read(bis);
            byte[] raw = new byte[size];
            if (bis.read(raw, 0, size) != size) {
                throw new CtxException("Unexpected EOF in signature data");
            }
            table.addEntry(type, raw);
        }

        // The input stream should be consumed at this point but a 'feature' of the encoding is
        // that there may be a trailer 0 byte, just check all look good
        int trail = bis.read();
        if (trail == 0)
            trail = bis.read();
        if (trail != -1)
            throw new CtxException("Unexpected additional byte found at end of signature");

        // All good so create signature

        return new ScalaSig(major, minor, table);
    }

    /**
     * Replace a namespace in the table with another namespace
     *
     * @param replace the namespace to replace, use '.' separators as usual
     * @param with    the namespace to use instead, use '.' separators as usual
     * @return the number of namespaces that were updated
     */
    int replace(String replace, String with) {
        return _table.replace(replace, with);
    }

    /**
     * Get a byte array containing the encoded signature
     * @return the byte array
     */
    byte[] asBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Nat.write(_majorVersion, out);
            Nat.write(_minorVersion, out);
            _table.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new CtxException("Unexpected error converting signature to byte array", e);
        }
    }

    private static final String separator = System.getProperty("line.separator");

    @Override
    public String toString() {
        return "Major version: " + _majorVersion + separator +
                "Minor version: " + _minorVersion + separator + _table.toString();
    }
}


