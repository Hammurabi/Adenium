package io.adenium.network;

public interface ResponseMetadata {
    public static final class ValidationBits {
        public static final int
            FullResponse    = 0, // aka noerrors
            PartialResponse = 1,
            EntireResponse  = 2,
            InvalidResponse = 4,
            SpamfulResponse = 8,
            InvalidType     = 16;
        ;
    }

    int getResponseBits(Message responseMessage);
}
