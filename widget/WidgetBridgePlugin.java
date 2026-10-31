package com.habitmentality.app;

import android.content.Context;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "WidgetBridge")
public class WidgetBridgePlugin extends Plugin {

    @PluginMethod
    public void update(PluginCall call) {
        String json = call.getString("json", "");
        Context c = getContext().getApplicationContext();
        c.getSharedPreferences("hm_widget", Context.MODE_PRIVATE)
                .edit().putString("data", json).apply();
        HmWidgets.refreshAll(c);
        call.resolve();
    }

    @PluginMethod
    public void pin(PluginCall call) {
        String type = call.getString("type", "today");
        Context c = getContext().getApplicationContext();
        boolean ok = HmWidgets.pin(c, type);
        JSObject r = new JSObject();
        r.put("supported", ok);
        call.resolve(r);
    }
}
