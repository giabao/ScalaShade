package uk.org.keng.scalashade;

import uk.org.keng.scalashade.model.ConstantTypeEntry;
import uk.org.keng.scalashade.model.ExtModClassRefEntry;
import uk.org.keng.scalashade.model.RawEntry;
import uk.org.keng.scalashade.model.TableEntry;
import uk.org.keng.scalashade.model.TermNameEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collection of entries from the ScalaSignature. Most types of entry are simply stored as raw bytes. The two cases
 * we are interested in are decoded and indexed. Methods are provided for renaming a namespace.
 */
public class Table {

    /**
     * Identifiers for tables entries that we are interested in
     */
    public static final class EntryType {
        public final static int TERM_NAME_ID = 1;
        public final static int EXT_MOD_CLASS_REF_ID = 10;
        public final static int CONSTANT_TYPE_ID = 15;

        public static final int LITERAL = 23;
        public static final int LITERALunit = 24;
        public static final int LITERALboolean = 25;
        public static final int LITERALbyte = 26;
        public static final int LITERALshort = 27;
        public static final int LITERALchar = 28;
        public static final int LITERALint = 29;
        public static final int LITERALlong = 30;
        public static final int LITERALfloat = 31;
        public static final int LITERALdouble = 32;
        public static final int LITERALstring = 33;
        public static final int LITERALnull = 34;
        public static final int LITERALclass = 35;
        public static final int LITERALenum = 36;
    }

    private final List<TableEntry> entries = new ArrayList<>();
    private final Map<Integer, ExtModClassRefEntry> extModClassRefEntriesMap = new HashMap<>();
    private final Map<Integer, TermNameEntry> termNameMap = new HashMap<>();
    private final Map<Integer, ConstantTypeEntry> constantTypeEntryMap = new HashMap<>();

    /**
     * Create a new table entry from its type & raw bytes.
     *
     * @param type the entry type
     * @param raw the raw bytes for the entry
     */
    void addEntry(int type, byte[] raw) {
        switch (type) {
            case EntryType.TERM_NAME_ID:
                TermNameEntry term = new TermNameEntry(raw);
                entries.add(term);
                termNameMap.put(entries.size() - 1, term);
                break;
            case EntryType.EXT_MOD_CLASS_REF_ID:
                ExtModClassRefEntry classRef = new ExtModClassRefEntry(raw);
                entries.add(classRef);
                extModClassRefEntriesMap.put(entries.size() - 1, classRef);
                break;
            case EntryType.CONSTANT_TYPE_ID:
                ConstantTypeEntry constantTypeEntry = new ConstantTypeEntry(raw);
                entries.add(constantTypeEntry);
                constantTypeEntryMap.put(entries.size() - 1, constantTypeEntry);
                break;
            default:
                entries.add(new RawEntry(type, raw));
                break;
        }
    }


    void findOutWhatItIs(TableEntry entry, List<Integer> list, String replace) {
        switch (entry.type()) {
//            case LITERALunit    => Constant(())
//            case LITERALboolean => Constant(readLong(len) != 0L)
//            case LITERALbyte    => Constant(readLong(len).toByte)
//            case LITERALshort   => Constant(readLong(len).toShort)
//            case LITERALchar    => Constant(readLong(len).toChar)
//            case LITERALint     => Constant(readLong(len).toInt)
//            case LITERALlong    => Constant(readLong(len))
//            case LITERALfloat   => Constant(intBitsToFloat(readLong(len).toInt))
//            case LITERALdouble  => Constant(longBitsToDouble(readLong(len)))
            case EntryType.LITERALstring:
                int index = Nat.read(new ByteArrayInputStream(entry.payload()));
                TableEntry e = entries.get(index);
                String s = new String(e.payload(), StandardCharsets.UTF_8);
                if (s.startsWith(replace)) {
                    list.add(index);
                }
                break;
//            case LITERALnull    => Constant(null)
//            case LITERALclass   => Constant(readTypeRef())
//            case LITERALenum    => Constant(readSymbolRef())
//            case _              => noSuchConstantTag(tag, len)
        }
    }

    /**
     * Write the table as a byte stream
     *
     * @param out the stream to write to
     * @throws IOException
     */
    void write(ByteArrayOutputStream out) throws IOException {
        Nat.write(entries.size(), out);
        for (TableEntry e : entries) {
            e.write(out);
        }
    }

    /**
     * Replace a namespace in the table with another namespace
     *
     * @param replace the namespace to replace, use '.' separators as usual
     * @param with    the namespace to use instead, use '.' separators as usual
     * @return the number of namespaces that were updated
     */
    int replace(String replace, String with) {

        // Locate extModClassRef entries that are exact match for namespace
        ArrayList<Integer> matched = new ArrayList<>();
        for (Map.Entry<Integer, ExtModClassRefEntry> entry : extModClassRefEntriesMap.entrySet()) {
            ExtModClassRefEntry e = entry.getValue();
            String ref = resolveClassRef(e);
            if (ref != null && ref.equals(replace)) {
                matched.add(entry.getKey());
            }
        }

        // Correct the entry
        for (int index : matched) {
            updateClassRef(extModClassRefEntriesMap.get(index), with.split("\\."));
        }

        // Locate ConstantType entries holding string literals and relocate the
        // corresponding TermNameEntry if applicable
        List<Integer> entriesToRelocate = new ArrayList<>();
        for (Map.Entry<Integer, ConstantTypeEntry> entry : constantTypeEntryMap.entrySet()) {
            ConstantTypeEntry e = entry.getValue();
            TableEntry unknownEntry = entries.get(e.constant_Ref());
            findOutWhatItIs(unknownEntry, entriesToRelocate, replace);
        }
        entriesToRelocate.forEach(index -> {
            TermNameEntry termNameEntry = (TermNameEntry) entries.get(index);
            String replacedString = new String(termNameEntry.payload(), StandardCharsets.UTF_8).replace(replace, with);
            termNameEntry.name(replacedString);
            System.out.println(termNameEntry.name());
        });

        return matched.size();
    }

    /**
     * Construct full namespace for an ExtModClassRef entry
     *
     * @param ref the entry
     * @return the namespace it encodes
     */
    private String resolveClassRef(ExtModClassRefEntry ref) {
        TermNameEntry e = termNameMap.get(ref.nameRef());
        if (e == null) return null;
        String base = "";
        if (ref.symbolRef() != -1) {
            ExtModClassRefEntry classRef = extModClassRefEntriesMap.get(ref.symbolRef());
            if (classRef == null) return null;
            base = resolveClassRef(classRef)+".";
        }
        return base + e.name();
    }

    /**
     * Update a ExtModClassRef to encode a different namespace, the existing entry is re-used but any parent
     * components of the namespace result in new ExtModClassRef being added to the table to avoid disrupting
     * other entries which may depend on the parent ExtModClassRef/TermName entries of the existing entry for
     * purposes we don't understand.
     *
     * @param ref  the entry to update
     * @param with array of namespace components to use instead
     */
    private void updateClassRef(ExtModClassRefEntry ref, String[] with) {
        entries.add(new TermNameEntry(with[with.length - 1]));
        int termRef = entries.size()-1;
        int symbolRef = -1;
        if (with.length>1)
            symbolRef = addClassRef(Arrays.copyOf(with, with.length - 1));
        ref.update(termRef, symbolRef);
    }

    /**
     * Create a new ExtModClassRef that encodes the passed namespace components
     *
     * @param with namespace components
     * @return index of new entry in table
     */
    private int addClassRef(String[] with) {
        // Recursively add parent class ref entries to get correct symbolRef
        int symbolRef = -1;
        if (with.length > 1) {
            symbolRef = addClassRef(Arrays.copyOf(with, with.length - 1));
        }

        // Add this one using last string as a term
        entries.add(new TermNameEntry(with[with.length - 1]));
        entries.add(new ExtModClassRefEntry(entries.size() - 1, symbolRef));
        return entries.size() - 1;
    }

    private static final String separator = System.getProperty("line.separator");

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TableEntry entry : entries) {
            sb.append(entry.toString()).append(separator);
        }
        return sb.toString();
    }

}
