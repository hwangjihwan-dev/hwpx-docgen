package io.github.docgen.core;

import java.io.IOException;

/** Indicates that an input file is not a usable HWPX document. */
public class HwpxFormatException extends IOException {
    private static final long serialVersionUID = 1L;

    public HwpxFormatException(String message) {
        super(message);
    }

    public HwpxFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
