package com.example.mobile_memoryexplorer.Auth;

import static android.content.ContentValues.TAG;
import static android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.mobile_memoryexplorer.Database.AppDatabase;
import com.example.mobile_memoryexplorer.Database.Profile;
import com.example.mobile_memoryexplorer.MainActivity;
import com.example.mobile_memoryexplorer.MySharedData;
import com.example.mobile_memoryexplorer.R;
import com.example.mobile_memoryexplorer.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executor;

public class Login extends AppCompatActivity {
  private static final int REQUEST_CODE = 101010;

  private FirebaseAuth auth;
  private ActivityLoginBinding binding;
  MySharedData mySharedData;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityLoginBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
    getSupportActionBar().setCustomView(R.layout.action_bar_layout);

    mySharedData = new MySharedData(this);
    if (MySharedData.getThemePreferences()) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
    if (mySharedData.getRemember().equals("true")) {
      Intent homePage = new Intent(this, MainActivity.class);
      homePage.setFlags(homePage.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
      startActivity(homePage);
    }
    auth = FirebaseAuth.getInstance();
    isLocationPermissionGranted();
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
      BiometricManager biometricManager = BiometricManager.from(this);
      switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
        case BiometricManager.BIOMETRIC_SUCCESS:
          Log.d("MY_APP_TAG", "App can authenticate using biometrics.");
          break;
        case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
          Log.e("MY_APP_TAG", "No biometric features available on this device.");
          break;
        case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
          Log.e("MY_APP_TAG", "Biometric features are currently unavailable.");
          break;
        case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
          // Prompts the user to create credentials that your app accepts.
          final Intent enrollIntent;
          enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
          enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
              BIOMETRIC_STRONG | DEVICE_CREDENTIAL);
          someActivityResultLauncher.launch(enrollIntent);

          break;
        case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
        case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
        case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
          break;
      }

      Executor executor = ContextCompat.getMainExecutor(this);
      BiometricPrompt biometricPrompt = new BiometricPrompt(Login.this,
          executor, new BiometricPrompt.AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int errorCode,
                                          @NonNull CharSequence errString) {
          super.onAuthenticationError(errorCode, errString);
          Toast.makeText(getApplicationContext(),
                  "Authentication error: " + errString, Toast.LENGTH_SHORT)
              .show();
        }

        @Override
        public void onAuthenticationSucceeded(
            @NonNull BiometricPrompt.AuthenticationResult result) {
          super.onAuthenticationSucceeded(result);
          Toast.makeText(getApplicationContext(),
              "Authentication succeeded!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationFailed() {
          super.onAuthenticationFailed();
          Toast.makeText(getApplicationContext(), "Authentication failed",
                  Toast.LENGTH_SHORT)
              .show();
        }
      });

      BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
          .setTitle("Biometric login for my app")
          .setSubtitle("Log in using your biometric credential")
          .setNegativeButtonText("Use account password")
          .build();

      biometricPrompt.authenticate(promptInfo);
    }
    binding.login.setOnClickListener(v ->
    {
      binding.progressBar.setVisibility(RelativeLayout.VISIBLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
          WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

      String email = binding.email.getText().toString();
      String password = binding.passsword.getText().toString();
      auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
        if (task.isSuccessful()) {

          AppDatabase appDb = AppDatabase.getInstance(Login.this);
          if(appDb.profileDao().getProfile(email) == null){
            Profile profile = new Profile(email, task.getResult().getUser().getDisplayName(), task.getResult().getUser().getPhotoUrl().toString());
            appDb.profileDao().insert(profile);
          }
          // Sign in success, update UI with the signed-in user's information
          mySharedData.setSharedpreferences("email", email);
          binding.progressBar.setVisibility(View.GONE);
          getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
          Intent homePage = new Intent(this, MainActivity.class);
          homePage.setFlags(homePage.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
          startActivity(homePage);
        } else {
          // If sign in fails, display a message to the user.
          Log.w(TAG, "signInWithEmail:failure", task.getException());
          binding.progressBar.setVisibility(View.GONE);
          getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
          Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
        }
      });
    });

    binding.register.setOnClickListener(v ->

        startActivity(new Intent(this, Register.class)));
    binding.rememberMe.setOnCheckedChangeListener((buttonView, isChecked) ->

    {
      if (isChecked) {
        mySharedData.setSharedpreferences("remember", "true");
      } else {
        mySharedData.setSharedpreferences("remember", "false");
      }
    });
  }

  ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
      new ActivityResultContracts.StartActivityForResult(),
      result -> {
        if (result.getResultCode() == REQUEST_CODE) {
          //TODO test fingerprint cicle
        }
      });
  private boolean isLocationPermissionGranted() {
    if (ActivityCompat.checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED
    ) {
      int requestcode = 1;
      ActivityCompat.requestPermissions(
          this,
          new String[]{
              android.Manifest.permission.ACCESS_FINE_LOCATION,
              android.Manifest.permission.ACCESS_COARSE_LOCATION
          },
          requestcode
      );
      return false;
    } else {
      return true;
    }
  }
}