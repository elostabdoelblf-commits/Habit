package com.habitmentality.app;

import android.content.Context;
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
        HabitWidget.refreshAll(c);
        call.resolve();
    }
}
