// Scheduled cleanup starter. Deploy with Firebase Cloud Functions (2nd gen).
// Requires Node.js 20+, firebase-functions, and firebase-admin.
// This function expires un-checked-in reservations after the check-in deadline
// and releases seats only when the seat still points to that reservation.
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");

initializeApp();
const db = getFirestore();

exports.expireLibraryReservations = onSchedule("every 5 minutes", async () => {
  const now = Timestamp.now();
  const expired = await db.collection("reservations")
    .where("status", "==", "RESERVED")
    .where("checkInDeadline", "<=", now)
    .get();

  for (const doc of expired.docs) {
    await db.runTransaction(async (tx) => {
      const bookingRef = doc.ref;
      const fresh = await tx.get(bookingRef);
      if (!fresh.exists || fresh.get("status") !== "RESERVED") return;

      const deadline = fresh.get("checkInDeadline");
      if (!deadline || deadline.toMillis() > Date.now()) return;

      const seatId = fresh.get("seatId");
      const seatRef = db.collection("seats").doc(seatId);
      const seat = await tx.get(seatRef);

      tx.update(bookingRef, { status: "EXPIRED" });
      if (seat.exists && seat.get("activeReservationId") === bookingRef.id) {
        tx.update(seatRef, {
          status: "AVAILABLE",
          activeReservationId: null
        });
      }
    });
  }

  // Release seats for checked-in reservations whose session end time passed.
  const ended = await db.collection("reservations")
    .where("status", "==", "CHECKED_IN")
    .where("endTime", "<=", now)
    .get();

  for (const doc of ended.docs) {
    await db.runTransaction(async (tx) => {
      const fresh = await tx.get(doc.ref);
      if (!fresh.exists || fresh.get("status") !== "CHECKED_IN") return;
      const seatId = fresh.get("seatId");
      const seatRef = db.collection("seats").doc(seatId);
      const seat = await tx.get(seatRef);
      tx.update(doc.ref, { status: "COMPLETED" });
      if (seat.exists && seat.get("activeReservationId") === doc.id) {
        tx.update(seatRef, {
          status: "AVAILABLE",
          activeReservationId: null
        });
      }
    });
  }
});
