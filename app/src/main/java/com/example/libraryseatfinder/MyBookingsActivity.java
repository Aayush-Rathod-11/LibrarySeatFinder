package com.example.libraryseatfinder;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyBookingsActivity extends AppCompatActivity {
    private final List<String> ids = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);
        ListView list = findViewById(R.id.bookingsList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        list.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { finish(); return; }
        FirebaseFirestore.getInstance().collection("reservations")
            .whereEqualTo("userId", uid).get()
            .addOnSuccessListener(snapshot -> {
                ids.clear(); labels.clear();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    ids.add(doc.getId());
                    String seat = doc.getString("seatId");
                    String status = doc.getString("status");
                    String end = "";
                    if (doc.getTimestamp("endTime") != null)
                        end = "\nEnds: " + DateFormat.getDateTimeInstance().format(doc.getTimestamp("endTime").toDate());
                    labels.add("Seat " + seat + " — " + status + end + "\nTap to check in or cancel");
                }
                adapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());

        list.setOnItemClickListener((parent, view, position, id) -> {
            String bookingId = ids.get(position);
            String label = labels.get(position);
            if (label.contains("RESERVED")) {
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Reservation actions")
                    .setItems(new String[]{"Check in", "Cancel booking"}, (dialog, which) -> {
                        BookingService service = new BookingService();
                        BookingService.Result result = message -> {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                            recreate();
                        };
                        if (which == 0) service.checkIn(bookingId, result);
                        else service.cancel(bookingId, result);
                    }).show();
            } else if (label.contains("CHECKED_IN")) {
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Cancel active booking?")
                    .setMessage("Cancelling will release the seat.")
                    .setPositiveButton("Cancel booking", (dialog, which) ->
                        new BookingService().cancel(bookingId, message -> {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show(); recreate();
                        }))
                    .setNegativeButton("Keep", null).show();
            }
        });
    }
}
