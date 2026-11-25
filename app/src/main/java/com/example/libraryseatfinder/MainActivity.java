package com.example.libraryseatfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText emailInput, passwordInput;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        Button login = findViewById(R.id.loginButton);
        Button register = findViewById(R.id.registerButton);

        login.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            if (email.isEmpty() || password.isEmpty()) {
                toast("Enter your email and password.");
                return;
            }
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startActivity(new Intent(this, DashboardActivity.class));
                    finish();
                } else toast("Login failed: " + task.getException().getMessage());
            });
        });
        register.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
