package com.example.libraryseatfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button create = findViewById(R.id.createButton);

        create.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            if (email.isEmpty() || password.length() < 6) {
                Toast.makeText(this, "Enter an email and password of at least 6 characters.", Toast.LENGTH_LONG).show();
                return;
            }
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Map<String, Object> user = new HashMap<>();
                    user.put("email", email);
                    user.put("role", "STUDENT");
                    user.put("createdAt", FieldValue.serverTimestamp());
                    FirebaseFirestore.getInstance().collection("users").document(uid).set(user)
                        .addOnSuccessListener(unused -> {
                            startActivity(new Intent(this, DashboardActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                });
        });
    }
}
