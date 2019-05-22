package uk.org.keng.scalashade.model;

import uk.org.keng.scalashade.Nat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generic container for a table entry, just holds on to raw bytes and does not attempt decoding.
 */
public class RawEntry implements TableEntry {
    private final int type;
    private final byte[] raw;

    /**
     * Create from raw
     * @param type the entry type
     * @param raw the raw bytes
     */
    public RawEntry(int type, byte[] raw) {
        this.type = type;
        this.raw = raw;
    }

    /**
     * Write entry back to a stream
     * @param bos stream to write to
     * @throws IOException
     */
    @Override
    public void write(ByteArrayOutputStream bos) throws IOException {
        bos.write(type);
        Nat.write(raw.length, bos);
        bos.write(raw);
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
        StringBuilder sb = new StringBuilder();
        sb.append("Type=").append(type).append(" Raw=");
        for (int i = 0; i < Math.min(64, raw.length); i++) {
            if (raw[i] > 32 && raw[i] < 127)
                sb.append((char) raw[i]);
            else
                sb.append(".");
        }
        return sb.toString();
    }
}

