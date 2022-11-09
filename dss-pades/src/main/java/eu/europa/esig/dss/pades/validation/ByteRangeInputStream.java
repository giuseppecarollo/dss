package eu.europa.esig.dss.pades.validation;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Reads InputStream according to the given {@code ByteRange}
 *
 */
public class ByteRangeInputStream extends FilterInputStream {

    /** The ByteRange to be read */
    private final ByteRange byteRange;

    /** Internal variable identifying the current position of InputStream */
    private int position = 0;

    /**
     * Default constructor
     *
     * @param is {@link InputStream} wrapped InputStream to access a PDF document content
     * @param byteRange {@link ByteRange} to read
     */
    public ByteRangeInputStream(final InputStream is, final ByteRange byteRange) {
        super(is);
        Objects.requireNonNull(is, "InputStream cannot be null!");
        Objects.requireNonNull(byteRange, "ByteRange cannot be null!");
        this.byteRange = byteRange;
    }

    @Override
    public int read() throws IOException {
        skipPerRange();

        int b = -1;
        if (position == byteRange.getFirstPartEnd()) {
            int offset =  byteRange.getSecondPartStart() - byteRange.getFirstPartEnd();
            long skipped = super.skip(offset);
            position += skipped;
        } else if (position < byteRange.getFirstPartStart()) {
            int offset = byteRange.getFirstPartStart() - position;
            long skipped = super.skip(offset);
            position += skipped;
        }
        if (isPositionWithinRange(position + 1)) {
            b = super.read();
            if (b != -1) {
                ++position;
            }
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        position += off;
        skipPerRange();

        int readBytes = 0;
        if (isPositionWithinFirstPart(position)) {
            int toRead = byteRange.getFirstPartStart() + byteRange.getFirstPartEnd() - position;
            readBytes = super.read(b, off, Math.min(len, toRead));
            position += readBytes;
        } else if (isPositionWithinSecondPart(position)) {
            int toRead = byteRange.getSecondPartStart() + byteRange.getSecondPartEnd() - position;
            int readBytesSecondPart = super.read(b, off + readBytes, Math.min(len - readBytes, toRead));
            readBytes += readBytesSecondPart;
            position += readBytesSecondPart;
        }
        if (readBytes < 1) {
            return -1;
        }
        return readBytes;
    }

    private void skipPerRange() throws IOException {
        if (position == byteRange.getFirstPartEnd()) {
            int offset =  byteRange.getSecondPartStart() - byteRange.getFirstPartEnd();
            long skipped = skip(offset);
            position += skipped;
        } else if (position < byteRange.getFirstPartStart()) {
            int offset = byteRange.getFirstPartStart() - position;
            long skipped = skip(offset);
            position += skipped;
        }
    }

    private boolean isPositionWithinRange(int position) {
        return isPositionWithinFirstPart(position) || isPositionWithinSecondPart(position);
    }

    private boolean isPositionWithinFirstPart(int position) {
        return position >= byteRange.getFirstPartStart() && position <= byteRange.getFirstPartStart() + byteRange.getFirstPartEnd();
    }

    private boolean isPositionWithinSecondPart(int position) {
        return position >= byteRange.getSecondPartStart() && position <= byteRange.getSecondPartStart() + byteRange.getSecondPartEnd();
    }

}
