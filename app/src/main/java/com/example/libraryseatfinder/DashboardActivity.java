package com.example.libraryseatfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<String> seatIds = new ArrayList<>();
    private final List<String> seatLabels = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ProgressBar loading;
    private ListenerRegistration seatListener;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class)); finish(); return;
        }
        setContentView(R.layout.activity_dashboard);
        ListView seatList = findViewById(R.id.seatList);
        loading = findViewById(R.id.loading);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, seatLabels);
        seatList.setAdapter(adapter);

        findViewById(R.id.bookingsButton).setOnClickListener(v ->
            startActivity(new Intent(this, MyBookingsActivity.class)));
        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class)); finish();
        });
        seatList.setOnItemClickListener((parent, view, position, id) -> {
            String seatId = seatIds.get(position);
            String label = seatLabels.get(position);
            if (!label.contains("AVAILABLE")) {
                Toast.makeText(this, "That seat is not available.", Toast.LENGTH_SHORT).show();
                return;
            }
            new BookingService().reserveSeat(seatId, new BookingService.Result() {
                @Override public void done(String message) {
                    Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
        listenToSeats();
    }

    private void listenToSeats() {
        loading.setVisibility(View.VISIBLE);
        seatListener = db.collection("seats").addSnapshotListener((snapshot, error) -> {
            loading.setVisibility(View.GONE);
            if (error != null) {
                Toast.makeText(this, "Could not load seats: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (snapshot == null) return;
            seatIds.clear(); seatLabels.clear();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String number = doc.getString("seatNumber");
                String status = doc.getString("status");
                if (number == null) number = doc.getId();
                if (status == null) status = "AVAILABLE";
                seatIds.add(doc.getId());
                seatLabels.add(number + " — " + status + (status.equals("AVAILABLE") ? " (tap to reserve)" : ""));
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (seatListener != null) seatListener.remove();
    }
}
