package io.adenium.network;

public class CheckedResponse {
    private Message message;
    private int     flags;

    public CheckedResponse(Message response, int flags) {
        this.message    = response;
        this.flags      = flags;
    }

    public Message getMessage() {
        return message;
    }

    public int getFlags() {
        return flags;
    }

    public boolean noErrors() {
        return flags == 0;
    }

    public boolean containsFullResponse() {
        return flags == 0 || (flags & ResponseMetadata.ValidationBits.EntireResponse) == ResponseMetadata.ValidationBits.EntireResponse;
    }

    public boolean containsPartialResponse() {
        return (flags & ResponseMetadata.ValidationBits.PartialResponse) == ResponseMetadata.ValidationBits.PartialResponse;
    }
}
