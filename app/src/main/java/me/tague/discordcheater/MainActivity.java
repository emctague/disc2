package me.tague.discordcheater;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private ValueCallback<Uri[]> uploadCallback = null;
    static int REQUEST_CODE_FILE_CHOOSER = 1;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String encodedJs = "";

        try {
            InputStream stream = getAssets().open("inject.js");
            byte[] buf = new byte[stream.available()];
            stream.read(buf);
            stream.close();

            encodedJs = Base64.encodeToString(buf, Base64.NO_WRAP);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setMessage("Failed to load `inject.js`, app will not function properly!")
                    .setTitle("Error Setting Up App!")
                    .create().show();
        }

        // Find and configure the webview.
        final WebView myWebView = findViewById(R.id.discord_web);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setDomStorageEnabled(true);

        final String finalEncodedJs = encodedJs;
        myWebView.setWebViewClient(new WebViewClient() {
            // Make sure discord can properly redirect between login and app pages.
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                if (req.getUrl().toString().startsWith("https://discordapp.com/")) return false;
                else {
                    startActivity(new Intent(Intent.ACTION_VIEW, req.getUrl()));
                    return true;
                }
            }

            // Inject a script into the page once it's stopped loading.
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.loadUrl("javascript:eval(window.atob('" + finalEncodedJs + "'));");
            }
        });

        // Allow for file selection via other apps.
        myWebView.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // Send a null value to any pre-existing stray callback
                if (uploadCallback != null)
                    uploadCallback.onReceiveValue(null);

                // Store the callback so it can be triggered upon completion
                uploadCallback = filePathCallback;

                // Create an intent for file selection
                Intent i = new Intent(Intent.ACTION_GET_CONTENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*");

                // Start the appropriate activity to choose a file.
                startActivityForResult(Intent.createChooser(i, "File Selection"), REQUEST_CODE_FILE_CHOOSER);

                return true;
            }
        });

        myWebView.loadUrl("https://discordapp.com/login");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Handle the result of a file being selected for use in the web view.
        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            // Trigger, and then remove the value callback.
            Uri[] result = new Uri[]{Uri.parse(intent.getDataString())};
            uploadCallback.onReceiveValue(result);
            uploadCallback = null;
        }
    }

}
