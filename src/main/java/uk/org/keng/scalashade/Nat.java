package uk.org.keng.scalashade;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Utility functions for reading/writing NAT encoded values.
 * This is a 8-to-7-bit encoding with the high bit used as a continuation marker on all but the final byte.
 */
public class Nat {
    public static int size(int value) {
        int count = 0;
        do {
            value = value >>7;
            count++;
        } while (value != 0);
        return count;
    }

    public static int read(ByteArrayInputStream in) {
        return read(in, 0);
    }

    private static int read(ByteArrayInputStream in, int carry) {
        int b = in.read();
        if (b == -1)
            throw new CtxException("Unexpected EOF in signature data");
        int acc = (carry << 7) + (b & 0x7f);
        if ((b & 0x80) == 0)
            return acc;
        return read(in, acc);
    }

    public static void write(int nat, ByteArrayOutputStream out) {
        write(nat, out, 0);
    }

    private static void write(int nat, ByteArrayOutputStream out, int flag) {
        int b = nat & 0x7f;
        int h = nat >> 7;
        if (h != 0) write(h, out, 0x80);
        out.write(b | flag);
    }
}
