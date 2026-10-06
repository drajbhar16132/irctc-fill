package com.irctcfill.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    WebView web;
    SharedPreferences sp;
    static final String HOME = "https://www.irctc.co.in/nget/train-search";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("p", MODE_PRIVATE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        CookieManager.getInstance().setAcceptCookie(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setBackgroundColor(Color.parseColor("#1a237e"));
        bar.addView(btn("Passengers", v -> editList()), new LinearLayout.LayoutParams(0, -2, 1f));
        bar.addView(btn("Auto Fill", v -> fill()), new LinearLayout.LayoutParams(0, -2, 1f));
        bar.addView(btn("Home", v -> web.loadUrl(HOME)), new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        web.loadUrl(HOME);
    }

    Button btn(String t, android.view.View.OnClickListener l) {
        Button x = new Button(this);
        x.setText(t); x.setOnClickListener(l);
        return x;
    }

    // Format: ek line = Naam,Umar,M/F/T,Berth(LB/MB/UB/SL/SU ya khali)
    void editList() {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        e.setMinLines(5); e.setGravity(android.view.Gravity.TOP);
        e.setHint("Ramesh Kumar,45,M,LB\nSita Devi,42,F,LB");
        e.setText(sp.getString("list", ""));
        new AlertDialog.Builder(this)
            .setTitle("Passengers (Naam,Age,M/F/T,Berth)")
            .setView(e)
            .setPositiveButton("Save", (d, w) -> sp.edit().putString("list", e.getText().toString()).apply())
            .setNegativeButton("Cancel", null).show();
    }

    void fill() {
        try {
            JSONArray arr = new JSONArray();
            for (String line : sp.getString("list", "").split("\n")) {
                String[] p = line.split(",");
                if (p.length < 3 || p[0].trim().isEmpty()) continue;
                JSONObject o = new JSONObject();
                o.put("n", p[0].trim()); o.put("a", p[1].trim());
                o.put("g", p[2].trim().toUpperCase());
                o.put("b", p.length > 3 ? p[3].trim().toUpperCase() : "");
                arr.put(o);
            }
            if (arr.length() == 0) { Toast.makeText(this, "Pehle Passengers add karo", Toast.LENGTH_SHORT).show(); return; }
            String js = "(async function(L){"
              + "const sleep=ms=>new Promise(r=>setTimeout(r,ms));"
              + "function set(el,v){if(!el)return;const proto=el.tagName=='SELECT'?HTMLSelectElement.prototype:HTMLInputElement.prototype;"
              + "Object.getOwnPropertyDescriptor(proto,'value').set.call(el,v);"
              + "el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));el.dispatchEvent(new Event('blur',{bubbles:true}));}"
              + "for(let i=0;i<L.length;i++){"
              + " let rows=document.querySelectorAll('app-passenger');"
              + " if(rows.length<=i){const add=[...document.querySelectorAll('span,a,button')].find(x=>/\\+\\s*Add Passenger/i.test(x.textContent)&&x.children.length<2);"
              + "  if(add){add.click();await sleep(600);rows=document.querySelectorAll('app-passenger');}}"
              + " const r=rows[i]; if(!r) continue;"
              + " set(r.querySelector('input[formcontrolname=passengerName],input[placeholder=Name]'),L[i].n);"
              + " set(r.querySelector('input[formcontrolname=passengerAge],input[placeholder=Age]'),L[i].a);"
              + " set(r.querySelector('select[formcontrolname=passengerGender]'),L[i].g);"
              + " if(L[i].b) set(r.querySelector('select[formcontrolname=passengerBerthChoice]'),L[i].b);"
              + " await sleep(200);}"
              + "})(" + arr.toString() + ");";
            web.evaluateJavascript(js, null);
            Toast.makeText(this, "Fill ho gaya. Captcha/payment khud karo", Toast.LENGTH_LONG).show();
        } catch (Exception ex) { Toast.makeText(this, "Error: " + ex, Toast.LENGTH_LONG).show(); }
    }

    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
