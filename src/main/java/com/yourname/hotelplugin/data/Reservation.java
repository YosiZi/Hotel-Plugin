package com.yourname.hotelplugin.data;

import java.util.UUID;

/**
 * Represents a reservation for a hotel room.
 * This class stores the room name, the UUID of the player who booked it,
 * and the expiration time of the reservation.
 */
public class Reservation {

    private String roomName;
    private UUID guestUUID;
    private long expirationTime;

    // A no-argument constructor is often useful for serialization/deserialization
    // with libraries like Gson or Jackson.
    public Reservation() {
    }

    public Reservation(String roomName, UUID guestUUID, long expirationTime) {
        this.roomName = roomName;
        this.guestUUID = guestUUID;
        this.expirationTime = expirationTime;
    }

    // Getters
    public String getRoomName() {
        return roomName;
    }

    public UUID getGuestUUID() {
        return guestUUID;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    // Setters (if needed, but for an immutable data class they might not be)
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