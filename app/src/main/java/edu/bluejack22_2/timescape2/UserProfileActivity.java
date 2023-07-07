package edu.bluejack22_2.timescape2;

import androidx.appcompat.app.AlertDialog;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;

import com.amulyakhare.textdrawable.TextDrawable;

import java.util.Random;

import edu.bluejack22_2.timescape2.model.User;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserProfileActivity extends BaseActivity {

    private UserProfileViewModel userProfileViewModel;
    private TextView userDisplayName, userEmail, userPhoneNumber, googleAccountMessage, versionText;
    private ImageView userAvatar;
    private Button changePasswordButton;
    private Button editDisplayNameButton;
    private Button logOutButton;
    private Button checkUpdatesButton;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Bind views
        userDisplayName = findViewById(R.id.userDisplayName);
        userEmail = findViewById(R.id.userEmail);
        userPhoneNumber = findViewById(R.id.userPhoneNumber);
        userAvatar = findViewById(R.id.userAvatar);
        googleAccountMessage = findViewById(R.id.googleAccountMessage);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        logOutButton = findViewById(R.id.logOutButton);
        editDisplayNameButton = findViewById(R.id.editDisplayNameButton);
        versionText = findViewById(R.id.versionText);
        checkUpdatesButton = findViewById(R.id.checkUpdatesButton);

        editDisplayNameButton.setOnClickListener(v -> showEditDisplayNameDialog());
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        logOutButton.setOnClickListener(v -> {
            signOut();
            finish();
        });

        // Setup action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.profile_and_settings);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Setup FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        // Setup ViewModel
        userProfileViewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        userProfileViewModel.getUserLiveData().observe(this, this::updateUI);
        userProfileViewModel.fetchUserDetails();

        userProfileViewModel.getUpdateMessage().observe(this, message -> {
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
            snackbar.show();
        });

        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        String currentVersionName = pInfo != null ? pInfo.versionName : getString(R.string.unknown_version);
        versionText.setText("Timescape " + currentVersionName);

        checkUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        });

    }

    private void updateUI(User user) {
        // Set user info
        userDisplayName.setText(user.getDisplayName());
        userEmail.setText(user.getEmail());
        userPhoneNumber.setText((user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) ? getString(R.string.no_phone_number_added) : user.getPhoneNumber());

        // Set user avatar
        Drawable avatarDrawable = generateAvatar(user.getUid());
        userAvatar.setImageDrawable(avatarDrawable);

        // Check auth provider
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            String providerId = firebaseUser.getProviderData().get(1).getProviderId();
            if ("password".equals(providerId)) {
                // User is signed in with email/password
                changePasswordButton.setVisibility(View.VISIBLE);
                googleAccountMessage.setVisibility(View.GONE);
            } else if ("google.com".equals(providerId)) {
                // User is signed in with Google
                changePasswordButton.setVisibility(View.GONE);
                googleAccountMessage.setVisibility(View.VISIBLE);
                googleAccountMessage.setText(R.string.this_account_is_linked_to_a_google_account);
            }
        }
    }

    private Drawable generateAvatar(String userId) {
        String content = "\\O/";
        Random random = new Random(userId.hashCode());
        int color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
        return TextDrawable.builder().buildRound(content, color);
    }

    private void showEditDisplayNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_display_name, null);

        EditText editDisplayName = view.findViewById(R.id.editDisplayName);

        builder.setView(view)
                .setTitle(R.string.edit_display_name)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newDisplayName = editDisplayName.getText().toString();
                    userProfileViewModel.updateDisplayName(this, newDisplayName);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.create().show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_change_password, null);

        EditText editOldPassword = view.findViewById(R.id.editOldPassword);
        EditText editNewPassword = view.findViewById(R.id.editNewPassword);
        EditText editConfirmPassword = view.findViewById(R.id.editConfirmPassword);

        builder.setView(view)
                .setTitle(R.string.change_password)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String oldPassword = editOldPassword.getText().toString();
                    String newPassword = editNewPassword.getText().toString();
                    String confirmPassword = editConfirmPassword.getText().toString();

                    if (newPassword.equals(confirmPassword)) {
                        userProfileViewModel.changePassword(this, oldPassword, newPassword);
                    } else {
                        Toast.makeText(UserProfileActivity.this, R.string.new_passwords_do_not_match, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.create().show();
    }


    // This method allows the back button in the ActionBar to behave like the hardware back button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

