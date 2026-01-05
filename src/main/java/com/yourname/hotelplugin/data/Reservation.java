package com.yourname.hotelplugin.data;

import java.util.UUID;

public class Reservation {
    private String roomName;
    private UUID guestUUID;
    private long expirationTime;

    public Reservation() {
    }

    public Reservation(String roomName, UUID guestUUID, long expirationTime) {
        this.roomName = roomName;
        this.guestUUID = guestUUID;
        this.expirationTime = expirationTime;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public UUID getGuestUUID() {
        return this.guestUUID;
    }

    public long getExpirationTime() {
        return this.expirationTime;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setGuestUUID(UUID guestUUID) {
        this.guestUUID = guestUUID;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }
}
