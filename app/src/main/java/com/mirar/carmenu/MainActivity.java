package com.mirar.carmenu;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Typeface;
import android.text.method.LinkMovementMethod;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Phone-side settings UI. One EditText for the server URL, one for the
 * device id, a save button. That's the whole phone UI surface.
 *
 * <p>Programmatic layout (no res/layout XML needed for something this
 * small — saves a file and makes the source self-contained).
 *
 * <p>On Save: persist prefs, then if {@code ACCESS_FINE_LOCATION} isn't
 * granted, ask. The AA screen still works without permission (server gets
 * told {@code location_permission:"denied"}), but the user almost certainly
 * wants location enabled — that's the point of the app.
 */
public class MainActivity extends AppCompatActivity {

    private static final String PRIVACY_URL =
            "https://mirarkitty.github.io/android-auto-car-menu/privacy.html";
    private static final String REPO_URL =
            "https://github.com/Mirarkitty/android-auto-car-menu";

    private EditText serverUrlInput;
    private EditText deviceIdInput;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    Toast.makeText(this,
                            granted
                                ? "Location granted"
                                : "Location denied — CarMenu still works but won't send GPS",
                            Toast.LENGTH_LONG).show();
                    // Whether granted or denied, the AA screen needs to know.
                    RefreshBus.get().signal();
                });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(R.string.settings_title);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, pad);
        root.addView(title);

        TextView welcome = new TextView(this);
        welcome.setText(R.string.welcome);
        welcome.setPadding(0, 0, 0, pad);
        root.addView(welcome);

        TextView urlLbl = new TextView(this);
        urlLbl.setText(R.string.server_url_label);
        root.addView(urlLbl);

        serverUrlInput = new EditText(this);
        serverUrlInput.setHint(R.string.server_url_hint);
        serverUrlInput.setText(Prefs.getServerUrl(this));
        root.addView(serverUrlInput);

        TextView idLbl = new TextView(this);
        idLbl.setText(R.string.device_id_label);
        idLbl.setPadding(0, pad, 0, 0);
        root.addView(idLbl);

        deviceIdInput = new EditText(this);
        deviceIdInput.setText(Prefs.getDeviceId(this));
        root.addView(deviceIdInput);

        Button save = new Button(this);
        save.setText(R.string.save);
        save.setOnClickListener(v -> onSaveClicked());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = pad;
        root.addView(save, lp);

        // ── Privacy block — explicit about what CarMenu does and does not
        // send. Spelled out in the UI so users (and Play reviewers) don't
        // need to dig into the policy URL to understand the scope.
        TextView privacyHeader = new TextView(this);
        privacyHeader.setText("Privacy");
        privacyHeader.setTextSize(16);
        privacyHeader.setTypeface(Typeface.DEFAULT_BOLD);
        privacyHeader.setPadding(0, pad * 2, 0, pad / 2);
        root.addView(privacyHeader);

        TextView privacyBody = new TextView(this);
        privacyBody.setText(
                "CarMenu only talks to the server URL you set above. "
              + "It sends your location, device id, and a timestamp — nothing else. "
              + "No analytics. No crash reporters. No third-party services. "
              + "The server is yours; the data path stops there.");
        privacyBody.setTextSize(13);
        root.addView(privacyBody);

        TextView privacyLink = new TextView(this);
        SpannableString linkText = new SpannableString("View privacy policy");
        linkText.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(PRIVACY_URL)));
                } catch (Throwable t) {
                    Toast.makeText(MainActivity.this,
                            "Couldn't open browser: " + PRIVACY_URL,
                            Toast.LENGTH_LONG).show();
                }
            }
        }, 0, linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        privacyLink.setText(linkText);
        privacyLink.setMovementMethod(LinkMovementMethod.getInstance());
        privacyLink.setTextSize(13);
        privacyLink.setPadding(0, pad / 2, 0, 0);
        root.addView(privacyLink);

        // ── Server protocol link — opens the bundled spec viewer so
        // technically-curious users (and Play reviewers) can see exactly
        // what gets sent and rendered, no network round-trip.
        TextView protocolHeader = new TextView(this);
        protocolHeader.setText("Server protocol");
        protocolHeader.setTextSize(16);
        protocolHeader.setTypeface(Typeface.DEFAULT_BOLD);
        protocolHeader.setPadding(0, pad * 2, 0, pad / 2);
        root.addView(protocolHeader);

        TextView protocolBody = new TextView(this);
        protocolBody.setText(
                "Full HTTP/JSON spec for what CarMenu sends to your server "
              + "and what it expects back. Bundled with the app — viewable "
              + "and downloadable offline.");
        protocolBody.setTextSize(13);
        root.addView(protocolBody);

        TextView protocolLink = new TextView(this);
        SpannableString protocolLinkText =
                new SpannableString("View / save protocol spec");
        protocolLinkText.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(MainActivity.this,
                        ProtocolActivity.class));
            }
        }, 0, protocolLinkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        protocolLink.setText(protocolLinkText);
        protocolLink.setMovementMethod(LinkMovementMethod.getInstance());
        protocolLink.setTextSize(13);
        protocolLink.setPadding(0, pad / 2, 0, 0);
        root.addView(protocolLink);

        // ── Source link — open-source repo with full app + reference
        // server source.  Useful for testers who want to audit the code,
        // contribute, or file an issue.
        TextView sourceHeader = new TextView(this);
        sourceHeader.setText("Open source");
        sourceHeader.setTextSize(16);
        sourceHeader.setTypeface(Typeface.DEFAULT_BOLD);
        sourceHeader.setPadding(0, pad * 2, 0, pad / 2);
        root.addView(sourceHeader);

        TextView sourceBody = new TextView(this);
        sourceBody.setText(
                "CarMenu is MIT-licensed open source. Read or audit the app "
              + "source, the privacy policy, the server protocol, and a "
              + "reference server implementation in one place.");
        sourceBody.setTextSize(13);
        root.addView(sourceBody);

        TextView sourceLink = new TextView(this);
        SpannableString sourceLinkText = new SpannableString("View on GitHub");
        sourceLinkText.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(REPO_URL)));
                } catch (Throwable t) {
                    Toast.makeText(MainActivity.this,
                            "Couldn't open browser: " + REPO_URL,
                            Toast.LENGTH_LONG).show();
                }
            }
        }, 0, sourceLinkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sourceLink.setText(sourceLinkText);
        sourceLink.setMovementMethod(LinkMovementMethod.getInstance());
        sourceLink.setTextSize(13);
        sourceLink.setPadding(0, pad / 2, 0, 0);
        root.addView(sourceLink);

        TextView footer = new TextView(this);
        footer.setText("Version: " + BuildConfig.VERSION_CODE
                + " (" + BuildConfig.VERSION_NAME + ")  built "
                + BuildConfig.BUILD_TIME);
        footer.setTextSize(12);
        footer.setPadding(0, pad * 2, 0, 0);
        root.addView(footer);

        setContentView(root);
    }

    private void onSaveClicked() {
        Prefs.setServerUrl(this, serverUrlInput.getText().toString());
        Prefs.setDeviceId(this, deviceIdInput.getText().toString());
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        // If AA is currently active, make it re-query with the new URL.
        RefreshBus.get().signal();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // Not granted. Show a rationale first when the OS says we should
        // (i.e. the user denied once); otherwise go straight to the system
        // prompt.
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Location needed")
                    .setMessage("CarMenu sends your location to your server so it can suggest "
                            + "relevant destinations. Without it the menu still loads but the "
                            + "server can't tailor suggestions.")
                    .setPositiveButton("Allow", (d, w) ->
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                    .setNegativeButton("Not now", null)
                    .show();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }
}
