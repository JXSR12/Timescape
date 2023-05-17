package edu.bluejack22_2.timescape;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    private final long YEAR_IN_SECOND = 31556952;

    private static final int RC_SIGN_IN = 9001;

    EditText regPhone, regEmail, regName, regPassword, regConfPassword;
    Button registerBtn, googleBtn;
    TextView goToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        regPhone = findViewById(R.id.editTextRegPhone);
        regEmail = findViewById(R.id.editTextRegEmail);
        regName = findViewById(R.id.editTextRegName);
        regPassword = findViewById(R.id.editTextRegPassword);
        regConfPassword = findViewById(R.id.editTextRegConfPassword);
        registerBtn = findViewById(R.id.cirRegisterButton);
        googleBtn = findViewById(R.id.cirGoogleButton);
        goToLogin = findViewById(R.id.registerGoToLoginText);

        registerBtn.setOnClickListener(x -> {
            if (validateInputs()) {
                registerWithEmailPassword();
            } else {
                Toast.makeText(getApplicationContext(), "Please fill all fields correctly, tap (?) icon for more info", Toast.LENGTH_SHORT).show();
            }
        });

        googleBtn.setOnClickListener(x -> {
            signOutAndSignInWithGoogle();
        });

        goToLogin.setOnClickListener(x -> {
            loadLogin();
        });
    }

    public boolean isValidEmail(String email) {
        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        return email.matches(regexPattern);
    }

    private void registerWithEmailPassword() {
        String email = regEmail.getText().toString().trim();
        String password = regPassword.getText().toString().trim();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String displayName = regName.getText().toString().trim();
                        String phoneNumber = regPhone.getText().toString().trim();
                        updateUserProfile(user, displayName, phoneNumber, false);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserProfile(FirebaseUser user, String displayName, String phoneNumber, boolean isGoogleSignIn) {
        if (user != null) {
            // Save user data to Firestore
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", user.getUid());
            userData.put("displayName", displayName);
            userData.put("email", user.getEmail());
            userData.put("phoneNumber", phoneNumber);

            db.collection("users")
                    .document(user.getUid())
                    .set(userData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                            if (isGoogleSignIn) {
                                loadMainActivity();
                            } else {
                                loadLogin();
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "Error saving user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void signOutAndSignInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            signInWithGoogle();
        });
    }
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(RegisterActivity.this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        String displayName = user.getDisplayName();
                        String phoneNumber = user.getPhoneNumber();
                        updateUserProfile(user, displayName, phoneNumber, true);
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(RegisterActivity.this, R.string.authentication_failed + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public boolean validateInputs(){
        if(regName.getText().toString().length() < 4){
            Toast.makeText(this, R.string.name_must_be_3_chars, Toast.LENGTH_SHORT).show();
        }
        if(regPassword.getText().toString().length() < 6){
            Toast.makeText(this, R.string.password_must_be_5_chars, Toast.LENGTH_SHORT).show();
        }
        if(regPhone.getText().toString().length() < 7){
            Toast.makeText(this, R.string.phone_must_be_6_chars, Toast.LENGTH_SHORT).show();
        }
        if(!isValidEmail(regEmail.getText().toString())){
            Toast.makeText(this, R.string.email_must_be_valid, Toast.LENGTH_SHORT).show();
        }

        return !(regPhone.getText().toString().length() < 7)
                && isValidEmail(regEmail.getText().toString())
                && !(regName.getText().toString().length() < 4)
                && !(regPassword.getText().toString().length() < 6)
                && regPassword.getText().toString().equals(regConfPassword.getText().toString())
                ;
    }

    public void loadMainActivity() {
        Intent mainActivity = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(mainActivity);
        finish();
    }

    public void loadLogin(){
        Intent login = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(login);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}