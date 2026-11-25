package com.example.libraryseatfinder;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import java.util.HashMap;
import java.util.Map;

public class BookingService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    public interface Result { void done(String message); }

    public void reserveSeat(String seatId, Result callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { callback.done("Please log in again."); return; }
        DocumentReference seatRef = db.collection("seats").document(seatId);
        DocumentReference bookingRef = db.collection("reservations").document();
        long now = System.currentTimeMillis();
        Timestamp created = new Timestamp(now / 1000, (int)((now % 1000) * 1_000_000));
        Timestamp checkDeadline = new Timestamp((now + 10 * 60_000L) / 1000,
            (int)(((now + 10 * 60_000L) % 1000) * 1_000_000));
        Timestamp end = new Timestamp((now + 60 * 60_000L) / 1000,
            (int)(((now + 60 * 60_000L) % 1000) * 1_000_000));

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot seat = transaction.get(seatRef);
            if (!seat.exists()) throw new IllegalStateException("Seat does not exist.");
            String status = seat.getString("status");
            String activeId = seat.getString("activeReservationId");
            if ("RESERVED".equals(status) || "OCCUPIED".equals(status)) {
                throw new IllegalStateException("This seat is already reserved.");
            }
            Map<String, Object> booking = new HashMap<>();
            booking.put("seatId", seatId);
            booking.put("userId", uid);
            booking.put("status", "RESERVED");
            booking.put("createdAt", created);
            booking.put("checkInDeadline", checkDeadline);
            booking.put("endTime", end);
            booking.put("checkedInAt", null);
            transaction.set(bookingRef, booking);
            Map<String, Object> seatUpdate = new HashMap<>();
            seatUpdate.put("status", "RESERVED");
            seatUpdate.put("activeReservationId", bookingRef.getId());
            transaction.set(seatRef, seatUpdate, SetOptions.merge());
            return null;
        }).addOnSuccessListener(unused -> callback.done("Seat reserved! Check in within 10 minutes."))
          .addOnFailureListener(e -> callback.done("Booking failed: " + e.getMessage()));
    }

    public void checkIn(String reservationId, Result callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        DocumentReference bookingRef = db.collection("reservations").document(reservationId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot booking = transaction.get(bookingRef);
            if (!booking.exists()) throw new IllegalStateException("Booking not found.");
            if (!uid.equals(booking.getString("userId"))) throw new IllegalStateException("This is not your booking.");
            if (!"RESERVED".equals(booking.getString("status"))) throw new IllegalStateException("Booking is not awaiting check-in.");
            Timestamp deadline = booking.getTimestamp("checkInDeadline");
            if (deadline == null || System.currentTimeMillis() > deadline.toDate().getTime())
                throw new IllegalStateException("Check-in deadline passed. Refresh seats and try again.");
            String seatId = booking.getString("seatId");
            DocumentReference seatRef = db.collection("seats").document(seatId);
            DocumentSnapshot seat = transaction.get(seatRef);
            if (!reservationId.equals(seat.getString("activeReservationId")))
                throw new IllegalStateException("Reservation is no longer active.");
            transaction.update(bookingRef, "status", "CHECKED_IN");
            transaction.update(bookingRef, "checkedInAt", Timestamp.now());
            transaction.update(seatRef, "status", "OCCUPIED");
            return null;
        }).addOnSuccessListener(unused -> callback.done("Check-in successful!"))
          .addOnFailureListener(e -> callback.done("Check-in failed: " + e.getMessage()));
    }

    public void cancel(String reservationId, Result callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        DocumentReference bookingRef = db.collection("reservations").document(reservationId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot booking = transaction.get(bookingRef);
            if (!booking.exists()) throw new IllegalStateException("Booking not found.");
            if (!uid.equals(booking.getString("userId"))) throw new IllegalStateException("Not your booking.");
            String status = booking.getString("status");
            if (!"RESERVED".equals(status) && !"CHECKED_IN".equals(status))
                throw new IllegalStateException("Booking is no longer active.");
            String seatId = booking.getString("seatId");
            DocumentReference seatRef = db.collection("seats").document(seatId);
            DocumentSnapshot seat = transaction.get(seatRef);
            transaction.update(bookingRef, "status", "CANCELLED");
            if (reservationId.equals(seat.getString("activeReservationId"))) {
                transaction.update(seatRef, "status", "AVAILABLE");
                transaction.update(seatRef, "activeReservationId", null);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.done("Booking cancelled."))
          .addOnFailureListener(e -> callback.done("Cancellation failed: " + e.getMessage()));
    }
}
