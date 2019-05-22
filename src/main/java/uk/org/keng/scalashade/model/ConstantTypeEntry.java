package uk.org.keng.scalashade.model;

import uk.org.keng.scalashade.Nat;
import uk.org.keng.scalashade.Table;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ConstantTypeEntry implements TableEntry {

    private final int constant_Ref;
    private final byte[] raw;
    private final int type = Table.EntryType.CONSTANT_TYPE_ID;

    public ConstantTypeEntry(byte[] raw) {
        ByteArrayInputStream in = new ByteArrayInputStream(raw);
        constant_Ref = Nat.read(in);
        this.raw = raw;
    }

    public int constant_Ref() {
        return constant_Ref;
    }

    @Override
    public void write(ByteArrayOutputStream bos) throws IOException {
        bos.write(type);
        Nat.write(raw.length, bos);
        bos.write(raw);
    }

    @Override
    public byte[] payload() {
        return raw;
    }

    @Override
    public int type() {
        return type;
    }
}


