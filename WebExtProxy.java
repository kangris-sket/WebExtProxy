package com.kangris.WebExtProxy;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.WebViewer;

@DesignerComponent(category = ComponentCategory.EXTENSION, 
                  description = "An extension that extends the features of the existing Web Viewer component 
                  iconName = "http://4.bp.blogspot.com/-um9fpZrF5hA/U4SCkUT1EuI/AAAAAAAAA08/cy9elvop6XY/s1600/Proxy_Logo3.gif", 
                  nonVisible = true, 
                  version = 1)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET,android.permission.ACCESS_NETWORK_STATE,android.permission.WRITE_EXTERNAL_STORAGE")

public class WebExtProxy extends AndroidNonvisibleComponent implements Component {
    public final String LOG_TAG = "WebExtProxy";
    private final Activity activity;
    private ComponentContainer container;
    private Context context;
    private String desc = "Downloading file";
    private BroadcastReceiver downloadReceiver = new C00023();
    private boolean showNotification = true;
    public boolean suppressToast;
    public String userAgentString = "";
    public WebView webView;
    public WebViewer webViewer;
    
    private static boolean setProxyKKPlus(WebView webView, String host, int port, String exclusion, String applicationClassName) {
    LOG.warn("try to setProxyKKPlus");
    Context appContext = webView.getContext().getApplicationContext();
    System.setProperty("http.proxyHost", host);
    System.setProperty("http.proxyPort", port + "");
    System.setProperty("http.nonProxyHosts", exclusion);
    System.setProperty("https.proxyHost", host);
    System.setProperty("https.proxyPort", port + "");
    System.setProperty("https.nonProxyHosts", exclusion);
    try {
      Class applictionCls = Class.forName(applicationClassName);
      Field loadedApkField = applictionCls.getField("mLoadedApk");
      loadedApkField.setAccessible(true);
      Object loadedApk = loadedApkField.get(appContext);
      Class loadedApkCls = Class.forName("android.app.LoadedApk");
      Field receiversField = loadedApkCls.getDeclaredField("mReceivers");
      receiversField.setAccessible(true);
      ArrayMap receivers = (ArrayMap) receiversField.get(loadedApk);
      for (Object receiverMap : receivers.values()) {
        for (Object rec : ((ArrayMap) receiverMap).keySet()) {
          Class clazz = rec.getClass();
          if (clazz.getName().contains("ProxyChangeListener")) {
            Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", Context.class, Intent.class);
            Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);
            Bundle extras = new Bundle();
            List<String> exclusionsList = new ArrayList<>(1);
            exclusionsList.add(exclusion);
            ProxyInfo proxyInfo = ProxyInfo.buildDirectProxy(host, port, exclusionsList);
            extras.putParcelable("android.intent.extra.PROXY_INFO", proxyInfo);
            intent.putExtras(extras);

            onReceiveMethod.invoke(rec, appContext, intent);
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("setProxyKKPlus - exception : {}", e);
      return false;
    }
    return true;
  }

    public WebExtProxy(ComponentContainer container) {
        super(container.$form());
        this.container = container;
        this.context = container.$context();
        this.activity = container.$context();
        Log.d("ExtendedWebViewer", "ExtendedWebViewer Created");
        this.form.registerReceiver(this.downloadReceiver, new IntentFilter("android.intent.action.DOWNLOAD_COMPLETE"));
    }

    @SimpleFunction(description = "Enable the OAuth requests made from the WebViewer")
    public void EnableOAuth() {
        this.webView.getSettings().setUserAgentString(System.getProperty("http.agent"));
    }
