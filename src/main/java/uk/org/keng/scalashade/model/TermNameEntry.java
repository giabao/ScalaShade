package uk.org.keng.scalashade.model;

import uk.org.keng.scalashade.Nat;
import uk.org.keng.scalashade.Table;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Container for TermName entries. This just contains a UTF-8 encoded string.
 */
public class TermNameEntry implements TableEntry {
    private String _name;

    private byte[] raw;
    private final int type = Table.EntryType.TERM_NAME_ID;

    /**
     * Construct from string
     * @param name the name
     */
    public TermNameEntry(String name) {
        _name = name;
    }

    /**
     * Construct from raw bytes
     * @param raw raw bytes to construct from
     */
    public TermNameEntry(byte[] raw) {
        _name = new String(raw, StandardCharsets.UTF_8);
        this.raw = raw;
    }

    public String name() {
        return _name;
    }

    public void name(String name) {
        this._name = name;
    }

    /**
     * Write entry back to a stream
     * @param bos stream to write to
     * @throws IOException
     */
    @Override
    public void write(ByteArrayOutputStream bos) throws IOException {
        bos.write(type);
        byte[] bytes = _name.getBytes(StandardCharsets.UTF_8);
        Nat.write(bytes.length, bos);
        bos.write(bytes);
    }

    @Override
    public byte[] payload() {
        return raw;
    }

    @Override
    public int type() {
        return type;
    }

    public String toString() {
        return "Type=termName name=" + _name;
    }
}
