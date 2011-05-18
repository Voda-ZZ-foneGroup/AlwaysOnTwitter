/*
 * Copyright (C) 2011 Vodafone Group Duesseldorf
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vodafone.twitter.service;

import java.net.URL;
import android.util.Config;
import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.widget.Toast;

public class OAuthActivity extends Activity {

  private static final String LOGTAG = "OAuthActivity";
  private Context context = null;
  private WebView myWebView = null;
  private TwitterService mService = null;
  private volatile String pin = null;
  private boolean pinSent = false;

  private class MyWebViewClient extends WebViewClient {
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String  failingUrl) {
      if(Config.LOGD) Log.i(LOGTAG+" MyWebViewClient", "onReceivedError() errorCode="+errorCode+" description="+description+" failingUrl="+failingUrl);
    }

    @Override
    public void onLoadResource(WebView view, String  url) {
      if(Config.LOGD) Log.i(LOGTAG+" MyWebViewClient", "onLoadResource() url="+url);
      if(url.endsWith("/oauth/authorize"))
        Toast.makeText(context, "Pin detection...", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPageFinished(WebView view, String  url) {
      if(Config.LOGD) Log.i(LOGTAG+" MyWebViewClient", "onPageFinished() url="+url);

      if(mService!=null && url!=null) {
        if(url.endsWith("/oauth/authorize")) {
          Toast.makeText(context, "Please wait...", 200).show();
          // parse the Twitter confirm document using dynamic javascript to find the "oauth_pin" element
          // push the pin to MyWebChromeClient.onJsAlert() per alert(string)
          if(Config.LOGD) Log.i(LOGTAG+" MyWebViewClient", "onPageFinished() oauth alert for pin");
            myWebView.loadUrl("javascript:(function() { " +
              "alert('oauth-pin='+document.getElementById('oauth_pin').innerHTML); " +
            "})()");
          if(Config.LOGD) Log.i(LOGTAG+" MyWebViewClient", "onPageFinished() oauth alert for pin done");
        }
      }
    }
  }

  private class MyWebChromeClient extends WebChromeClient {
    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
      Log.i(LOGTAG+" MyWebChromeClient", "onJsAlert() message="+ message);
      message = message.trim();
      if(message.startsWith("oauth-pin=")) {
        pin = message.substring(10).trim();
        if(Config.LOGD) Log.i(LOGTAG+" MyWebChromeClient", "onJsAlert() mService.twitterLogin("+pin+")");
        //Toast.makeText(context, "Pin = "+pin+" ...", 200).show();
        (new Thread() { public void run() {
          if(mService!=null)
            mService.twitterLogin(pin);
        }}).start();
        pinSent = true;
        if(Config.LOGD) Log.i(LOGTAG+" MyWebChromeClient", "onJsAlert() finish()");
        finish(); // we're done - now close this activity
        return true; // and do not show the alert dialog
      }
      pin = null;
      return false;
    }
  }

  private ServiceConnection serviceConnection = new ServiceConnection() { 
    public void onServiceConnected(ComponentName className, IBinder binder) { 
      // activated by bindService() in onConnect()
      if(Config.LOGD) Log.i(LOGTAG, "onServiceConnected");
      mService = ((TwitterService.LocalBinder)binder).getService();
    }
    public void onServiceDisconnected(ComponentName className) { 
      // result of unbindService()
      if(Config.LOGD) Log.i(LOGTAG, "onServiceDisconnected");
      mService = null; 
    } 
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if(Config.LOGD) Log.i(LOGTAG, "onCreate");
    if(context==null)
      context = getApplicationContext();

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    // prepare connection to TwitterService for post processing: call to twitterLogin(pin) (OAuth pin handover)
    Intent serviceIntent = new Intent(this, TwitterService.class);
    bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE); 

    if(myWebView==null) {
      myWebView = new WebView(this);
      if(myWebView!=null) {
        WebSettings webSettings = myWebView.getSettings();
        if(webSettings!=null) {
          webSettings.setJavaScriptEnabled(true);
          webSettings.setSavePassword(false);
          webSettings.setSaveFormData(false);
        }

        myWebView.setWebViewClient(new MyWebViewClient()); // onPageFinished(), etc.
        myWebView.setWebChromeClient(new MyWebChromeClient()); // onJsAlert(), etc.
        setContentView(myWebView);

        // fetch the OAuth web document..
        String webpath = getIntent().getStringExtra("argAuthorizationURL");
        if(Config.LOGD) Log.i(LOGTAG, "loadUrl("+webpath+") ...");
        myWebView.loadUrl(webpath);
        if(Config.LOGD) Log.i(LOGTAG, "done initializing myWebView, mService="+mService);
        // --> OAuth-pin post-processing -> MyWebViewClient.onPageFinished() and MyWebChromeClient.onJsAlert()
      }
    }

    if(Config.LOGD) Log.i(LOGTAG, "done onCreate()");
  }

  @Override
  protected void onStop() {
    if(Config.LOGD) Log.i(LOGTAG, "onStop");
    if(mService!=null && !pinSent) {
      // let the service know that login has been aborted
      if(Config.LOGD) Log.i(LOGTAG, "onStop mService.twitterLogin(null) ...");
      mService.twitterLogin(null);
    }
    if(serviceConnection!=null) {
      if(Config.LOGD) Log.i(LOGTAG, "onStop unbindService() ...");
      unbindService(serviceConnection);
      // our service will continue to run, it was running before this activity was started
      serviceConnection = null;
    }
    if(myWebView!=null) {
      if(Config.LOGD) Log.i(LOGTAG, "onStop myWebView.destroy() ...");
      myWebView.destroy();
      myWebView=null;
    }
    mService = null;
    super.onStop();
  }
}


