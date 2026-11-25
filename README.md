# Library Seat Finder (Android / Java / XML / Firebase)

Beginner starter project. Package: `com.example.libraryseatfinder`

## Features in this starter
- Email/password registration and login using Firebase Authentication
- Firestore seat list
- Transaction-based seat reservation (two users cannot reserve the same seat simultaneously)
- Check-in before the deadline
- Cancel reservation
- My bookings / reservation status
- Firestore Security Rules starter
- Firebase Cloud Function scheduled cleanup starter

## Important setup
This starter intentionally does not include `google-services.json` because it is unique to your Firebase project.

1. Open Android Studio.
2. Choose **Open** and select the `LibrarySeatFinder` folder.
3. Wait for Gradle sync.
4. In Firebase Console, create a Firebase project.
5. Add an Android app with package name exactly `com.example.libraryseatfinder`.
6. Download `google-services.json` and put it in `app/`.
7. In Firebase Console, enable **Authentication > Sign-in method > Email/Password**.
8. Create a **Cloud Firestore** database.
9. Publish `firestore.rules` from the `firebase/` folder using Firebase Console or Firebase CLI.
10. Run the app on an emulator or Android phone.

## Add starter seats
In Firestore, create collection `seats`. Add documents with these IDs and fields:
- `A01`: `seatNumber` (string) = `A01`, `status` (string) = `AVAILABLE`
- `A02`: `seatNumber` (string) = `A02`, `status` (string) = `AVAILABLE`
- `A03`: `seatNumber` (string) = `A03`, `status` (string) = `AVAILABLE`
- `B01`: `seatNumber` (string) = `B01`, `status` (string) = `AVAILABLE`
- `B02`: `seatNumber` (string) = `B02`, `status` (string) = `AVAILABLE`
- `B03`: `seatNumber` (string) = `B03`, `status` (string) = `AVAILABLE`

## Reservation policy in this starter
- Reservation duration: 60 minutes.
- Check-in deadline: 10 minutes after reservation creation.
- A reservation must be checked in before its deadline.
- A seat's active reservation ID is stored on the seat document.
- Transactions check the seat document before writing, preventing simultaneous double-booking of a seat.
- The client checks expiry during app actions. For dependable automatic release while the app is closed, deploy the scheduled Cloud Function in `functions/` (requires Firebase Blaze billing plan and Cloud Functions setup).

## Deploy scheduled cleanup (optional but recommended)
1. Install Node.js and Firebase CLI.
2. In a terminal: `npm install -g firebase-tools`
3. Run `firebase login`
4. Run `firebase init functions` in your Firebase project directory; choose JavaScript.
5. Copy/merge `functions/index.js` into the generated Functions entry file.
6. Deploy with `firebase deploy --only functions`.
7. Set your Firebase project ID in `.firebaserc` or choose the project with `firebase use --add`.

The scheduled function uses Admin SDK privileges. Keep it server-side and never put service-account credentials in the Android app.

## Build notes
- If Android Studio offers to upgrade the Android Gradle Plugin or Gradle wrapper, use the IDE's recommended compatible versions.
- Do not publish Firebase service credentials or your `google-services.json` in a public repository.
- Before production use, add stronger role management, rate limits, audit logs, and security review.
