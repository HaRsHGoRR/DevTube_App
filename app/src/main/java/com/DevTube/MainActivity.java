package com.DevTube;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    private TextView loadingMessage;
    private ImageView imageView;

    private static final long TIMEOUT_DURATION = 2 * 60 * 1000; // 2 minutes timeout
    private static final int RC_SIGN_IN = 123; // Request code for Google sign-in

    private Handler timeoutHandler;
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);
        loadingMessage = findViewById(R.id.loadingMessage);
        imageView = findViewById(R.id.imageView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript
        webSettings.setDomStorageEnabled(true); // Enable DOM Storage for better web app support
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); // Allow JavaScript to open windows automatically
        webSettings.setSupportMultipleWindows(true); // Support multiple windows
        String userAgent = "Mozilla/5.0 (Linux; Android 10; Pixel 4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.101 Mobile Safari/537.36";
        webSettings.setUserAgentString(userAgent); // Set custom user agent

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                // Show ProgressBar and Loading Message when page starts loading
                progressBar.setVisibility(View.VISIBLE);
                loadingMessage.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);

                // Start timeout mechanism
                startTimeout();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Hide ProgressBar and Loading Message when page finishes loading
                progressBar.setVisibility(View.GONE);
                loadingMessage.setVisibility(View.GONE);
                imageView.setVisibility(View.GONE);

                // Cancel timeout mechanism
                cancelTimeout();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Handle errors and retry loading the website
                Toast.makeText(MainActivity.this, "Network Error. Retrying...", Toast.LENGTH_SHORT).show();
                webView.loadUrl(failingUrl);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Check if the URL matches the redirect URL pattern
                if (isRedirectUrl(url)) {
                    // Open the redirect URL in the browser
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true; // Indicate that the URL has been handled
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                final WebView newWebView = new WebView(MainActivity.this);
                WebSettings webSettings = newWebView.getSettings();
                webSettings.setJavaScriptEnabled(true); // Enable JavaScript

                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(newWebView);
                dialog.setCancelable(true);
                dialog.show();

                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        // Load all URLs inside WebView
                        view.loadUrl(url);
                        return true;
                    }
                });

                ((WebView.WebViewTransport) resultMsg.obj).setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                super.onCloseWindow(window);
            }
        });

        webView.loadUrl("https://devtube-zeta.vercel.app"); // Load the initial URL
    }

    private void signInWithGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient signInClient = GoogleSignIn.getClient(this, gso);
        Intent signInIntent = signInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String idToken = account.getIdToken();
                    webView.loadUrl("javascript:handleGoogleSignIn('" + idToken + "')");
                } else {
                    // Handle null account
                    Log.e("MainActivity", "GoogleSignInAccount is null");
                }
            } catch (ApiException e) {
                // Handle ApiException
                Log.e("MainActivity", "signInResult:failed code=" + e.getStatusCode());
            } catch (Exception e) {
                // Handle other exceptions
                Log.e("MainActivity", "Exception: " + e.getMessage());
            }
        }
    }

    private void startTimeout() {
        timeoutHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                // Handle timeout, reload the WebView
                webView.reload();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DURATION);
    }

    private void cancelTimeout() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private boolean isRedirectUrl(String url) {
        // Check if the URL matches the redirect URL pattern
        return url.startsWith("https://devtube-beec1.firebaseapp.com/__/auth/handler");
    }
}

