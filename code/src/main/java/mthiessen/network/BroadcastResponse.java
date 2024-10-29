package mthiessen.network;

import java.io.Serializable;

public record BroadcastResponse(BroadcastResponseStatus status, Object responsePayload)
    implements Serializable {}
