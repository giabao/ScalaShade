package uk.org.keng.scalashade.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Interface for all entry objects
 */
public interface TableEntry {

    /**
     * Write byte representation of table entry into the stream
     *
     * @param out stream to write onto
     * @throws IOException
     */
    void write(ByteArrayOutputStream out) throws IOException;

    byte[] payload();

    int type();

}
