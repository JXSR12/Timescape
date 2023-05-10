package edu.bluejack22_2.timescape;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {
    EditText inputEmail, inputPassword, inputPhone;
    TextInputLayout tilEmail, tilPassword, tilPhone;
    RadioGroup selectLoginMethod;
    RadioButton selectPhone, selectEmail;
    TextView goToRegister;
    Button loginBtn, googleBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            mAuth.signOut();
        }

        inputEmail = findViewById(R.id.editTextEmail);
        inputPhone = findViewById(R.id.editTextPhone);
        inputPassword = findViewById(R.id.editTextPassword);

        tilEmail = findViewById(R.id.textInputEmail);
        tilPhone = findViewById(R.id.textInputPhone);
        tilPassword = findViewById(R.id.textInputPassword);

        selectLoginMethod = findViewById(R.id.radioGroupLoginMethod);
        selectPhone = findViewById(R.id.radioBtnPhone);
        selectEmail = findViewById(R.id.radioBtnEmail);

        goToRegister = findViewById(R.id.loginGoToRegisterText);
        loginBtn = findViewById(R.id.cirLoginButton);
        googleBtn = findViewById(R.id.cirGoogleButton);

        selectEmail.setChecked(true);
        inputPhone.setVisibility(View.GONE);
        tilPhone.setVisibility(View.GONE);

        selectLoginMethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                View radioButton = selectLoginMethod.findViewById(checkedId);
                int index = selectLoginMethod.indexOfChild(radioButton);
                switch (index) {
                    case 0:
                        selectPhone.setChecked(true);
                        inputPhone.setVisibility(View.VISIBLE);
                        tilPhone.setVisibility(View.VISIBLE);
                        inputEmail.setVisibility(View.GONE);
                        tilEmail.setVisibility(View.GONE);
                        break;
                    case 1:
                        selectEmail.setChecked(true);
                        inputEmail.setVisibility(View.VISIBLE);
                        tilEmail.setVisibility(View.VISIBLE);
                        inputPhone.setVisibility(View.GONE);
                        tilPhone.setVisibility(View.GONE);
                        break;
                }
            }
        });

        goToRegister.setOnClickListener(x->{
            Intent register = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(register);
            finish();
        });

        final boolean[] usingPhone = {false};
        final String[] inputEmailStr = {""};
        final String[] inputPhoneStr = {""};
        final String[] inputPassStr = {""};

        loginBtn.setOnClickListener(x->{
            inputEmailStr[0] = inputEmail.getText().toString();
            inputPassStr[0] = inputPassword.getText().toString();
            inputPhoneStr[0] = inputPhone.getText().toString();

            usingPhone[0] = selectPhone.isChecked();
            if(usingPhone[0]){
                if(inputPhoneStr[0].isEmpty() || inputPassStr[0].isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                }else{
                    loginWithPhone(inputPhoneStr[0], inputPassStr[0]);
                }
            }else{
                if(inputEmailStr[0].isEmpty() || inputPassStr[0].isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                }else {
                    loginWithEmail(inputEmailStr[0], inputPassStr[0]);
                }
            }

        });

        db = FirebaseFirestore.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleBtn.setOnClickListener(v -> signOutAndSignInWithGoogle());
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        loadMain();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginWithPhone(String phoneNumber, String password) {
        db.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String email = document.getString("email");
                            loginWithEmail(email, password);
                        } else {
                            Toast.makeText(LoginActivity.this, "Phone number not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error retrieving user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(LoginActivity.this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

                        // Check if the user exists in Firestore
                        db.collection("users")
                                .document(user.getUid())
                                .get()
                                .addOnCompleteListener(fetchTask -> {
                                    if (fetchTask.isSuccessful()) {
                                        DocumentSnapshot document = fetchTask.getResult();
                                        if (document.exists()) {
                                            // User exists, load main activity
                                            loadMain();
                                        } else {
                                            // User does not exist, add user data to Firestore
                                            String displayName = user.getDisplayName();
                                            String phoneNumber = user.getPhoneNumber();
                                            updateUserProfile(user, displayName, phoneNumber, true);
                                        }
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Error checking user data: " + fetchTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(LoginActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                            loadMain();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error saving user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public void loadMain(){
        Intent main = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(main);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}