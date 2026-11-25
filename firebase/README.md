# Firebase setup notes

The included `firestore.rules` deliberately denies client writes to seats/reservations.
That is the safe production direction, but the Android starter currently writes directly
to those collections. To run the starter for a classroom demo, you must either:
1. implement callable Cloud Functions for reserve/check-in/cancel and call them from Android (recommended), or
2. temporarily create development-only rules and never ship them to production.

Do not deploy permissive rules to a real project. The next development step should be moving
booking operations to trusted Cloud Functions so security rules can stay restrictive.
