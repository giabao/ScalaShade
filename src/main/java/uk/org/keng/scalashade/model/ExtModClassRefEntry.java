package uk.org.keng.scalashade.model;

import uk.org.keng.scalashade.Nat;
import uk.org.keng.scalashade.Table;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Container for table class ref entries. These have a nameRef which refers to a termName entry and a symbolRef which
 * refers to a parent ExtModClassRefEntry. To decode them you walk the hierarchy until you find one without a
 * symbolRef, its optional in the byte encoding. We represent that by setting the symbolRef to -1.
 */
public class ExtModClassRefEntry implements TableEntry {

    private int _nameRef;
    private int _symbolRef;
    private byte[] raw;
    private final int type = Table.EntryType.EXT_MOD_CLASS_REF_ID;

    /**
     * Create from existing name & symbol entries
     * @param nameRef the index of a termName entry for the name
     * @param symbolRef the index of a "parent" ExtModClassRef
     */
    public ExtModClassRefEntry(int nameRef, int symbolRef) {
        _nameRef = nameRef;
        _symbolRef = symbolRef;
    }

    /**
     * Decode from raw bytes
     * @param raw the bytes
     */
    public ExtModClassRefEntry(byte[] raw) {
        this.raw = raw;
        ByteArrayInputStream in = new ByteArrayInputStream(raw);
        _nameRef = Nat.read(in);

        // Symbol is optional in the encoding
        _symbolRef = -1;
        if (in.available() > 0) {
            _symbolRef = Nat.read(in);
        }
    }

    public int nameRef() {
        return _nameRef;
    }

    public int symbolRef() {
        return _symbolRef;
    }

    /**
     * Update this entry with new name & symbol entries
     * @param nameRef the index of a termName entry for the name
     * @param symbolRef the index of a "parent" ExtModClassRef
     */
    public void update(int nameRef, int symbolRef) {
        _nameRef = nameRef;
        _symbolRef = symbolRef;
    }

    /**
     * Write entry back to a stream
     * @param bos stream to write to
     * @throws IOException
     */
    public void write(ByteArrayOutputStream bos) throws IOException {
        bos.write(type);
        if (_symbolRef != -1) {
            Nat.write(Nat.size(_nameRef)+Nat.size(_symbolRef), bos);
            Nat.write(_nameRef, bos);
            Nat.write(_symbolRef, bos);
        } else {
            Nat.write(Nat.size(_nameRef), bos);
            Nat.write(_nameRef, bos);
        }
    }

    @Override
    public int type() {
        return type;
    }

    @Override
    public byte[] payload() {
        return raw;
    }

    @Override
    public String toString() {
        return "Type=extModClassRef " + "nameRef=" + _nameRef + " _symbolRef=" + _symbolRef;
    }
}
