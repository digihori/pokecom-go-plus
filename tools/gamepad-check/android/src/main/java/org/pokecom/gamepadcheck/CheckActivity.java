package org.pokecom.gamepadcheck;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.widget.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.*;

public final class CheckActivity extends Activity implements InputManager.InputDeviceListener {
    private static final int EVENT_LIMIT = 20000;
    private final Map<Integer, InputDevice> devices = new LinkedHashMap<>();
    private final Map<Integer, Map<String, Float>> values = new LinkedHashMap<>();
    private InputManager input;
    private TextView live, status;
    private Button record, marker, share;
    private Spinner model;
    private JSONArray events = new JSONArray();
    private boolean recording, truncated;
    private long started;
    private String recordedModel;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        layout.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom);
            } else {
                view.setPadding(padding + insets.getSystemWindowInsetLeft(), padding + insets.getSystemWindowInsetTop(),
                    padding + insets.getSystemWindowInsetRight(), padding + insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        setContentView(layout);
        TextView title = new TextView(this);
        title.setText("Gamepad Check");
        title.setTextSize(24);
        layout.addView(title);
        model = new Spinner(this);
        model.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
            new String[]{"不明", "初代 Joy-Con", "Joy-Con 2", "その他"}));
        layout.addView(model);
        record = button(layout, "記録開始", this::toggleRecording);
        marker = button(layout, "区切りを記録", () -> append("marker", null, null, null));
        share = button(layout, "診断結果を共有", this::shareLog);
        status = new TextView(this);
        layout.addView(status);
        live = new TextView(this);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(live);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        input = getSystemService(InputManager.class);
        input.registerInputDeviceListener(this, new Handler(Looper.getMainLooper()));
        for (int id : input.getInputDeviceIds()) onInputDeviceAdded(id);
        refresh();
    }

    private Button button(LinearLayout layout, String title, Runnable action) {
        Button button = new Button(this);
        button.setText(title);
        button.setOnClickListener(view -> action.run());
        layout.addView(button);
        return button;
    }

    @Override protected void onDestroy() {
        input.unregisterInputDeviceListener(this);
        super.onDestroy();
    }

    private static boolean controller(InputDevice device) {
        return device != null && (device.supportsSource(InputDevice.SOURCE_GAMEPAD)
            || device.supportsSource(InputDevice.SOURCE_JOYSTICK));
    }

    @Override public void onInputDeviceAdded(int id) {
        InputDevice device = input.getInputDevice(id);
        if (!controller(device) || devices.containsKey(id)) return;
        devices.put(id, device);
        values.put(id, new LinkedHashMap<>());
        append("connected", device, null, null);
        refresh();
    }

    @Override public void onInputDeviceRemoved(int id) {
        InputDevice device = devices.remove(id);
        values.remove(id);
        if (device != null) append("disconnected", device, null, null);
        refresh();
    }

    @Override public void onInputDeviceChanged(int id) {
        onInputDeviceRemoved(id);
        onInputDeviceAdded(id);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        InputDevice device = event.getDevice();
        if (controller(device) && (KeyEvent.isGamepadButton(event.getKeyCode())
            || event.isFromSource(InputDevice.SOURCE_DPAD)
            || event.isFromSource(InputDevice.SOURCE_GAMEPAD))
            && (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_UP)) {
            update(device, "button", KeyEvent.keyCodeToString(event.getKeyCode()),
                event.getAction() == KeyEvent.ACTION_DOWN ? 1f : 0f);
            refresh();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        InputDevice device = event.getDevice();
        if (!controller(device) || event.getActionMasked() != MotionEvent.ACTION_MOVE
            || !(event.isFromSource(InputDevice.SOURCE_JOYSTICK) || event.isFromSource(InputDevice.SOURCE_GAMEPAD)
                || event.isFromSource(InputDevice.SOURCE_DPAD))) return super.dispatchGenericMotionEvent(event);
        for (int sample = 0; sample <= event.getHistorySize(); sample++) {
            for (InputDevice.MotionRange range : device.getMotionRanges()) {
                if (range.getSource() != event.getSource()) continue;
                int axis = range.getAxis();
                float value = sample == event.getHistorySize()
                    ? event.getAxisValue(axis) : event.getHistoricalAxisValue(axis, sample);
                update(device, "axis", MotionEvent.axisToString(axis), value);
            }
        }
        refresh();
        return true;
    }

    private void update(InputDevice device, String type, String control, float value) {
        onInputDeviceAdded(device.getId());
        Map<String, Float> state = values.get(device.getId());
        if (state == null || !Float.isFinite(value)) return;
        Float previous = state.get(control);
        if (previous != null && (previous == value
            || (value != 0f && Math.abs(value - previous) < 0.01f))) return;
        state.put(control, value);
        append(type, device, control, value);
    }

    private void toggleRecording() {
        if (recording) recording = false;
        else {
            events = new JSONArray();
            truncated = false;
            started = SystemClock.elapsedRealtime();
            recordedModel = model.getSelectedItem().toString();
            recording = true;
            append("marker", null, "start", null);
            for (InputDevice device : devices.values()) {
                append("connected", device, null, null);
                values.get(device.getId()).forEach((control, value) ->
                    append(control.startsWith("AXIS_") ? "axis" : "button", device, control, value));
            }
        }
        refresh();
    }

    private void append(String type, InputDevice device, String control, Float value) {
        if (!recording) return;
        if (events.length() >= EVENT_LIMIT) { truncated = true; return; }
        try {
            JSONObject event = new JSONObject().put("elapsedMs", SystemClock.elapsedRealtime() - started).put("type", type);
            if (device != null) {
                event.put("deviceId", Integer.toString(device.getId())).put("deviceName", device.getName());
                if ("connected".equals(type)) event.put("vendorId", device.getVendorId())
                    .put("productId", device.getProductId()).put("sources", device.getSources());
            }
            if (control != null) event.put("control", control);
            if (value != null) event.put("value", value.doubleValue());
            events.put(event);
        } catch (JSONException error) { throw new IllegalStateException(error); }
    }

    private void refresh() {
        if (live == null) return;
        StringBuilder display = new StringBuilder("接続中 " + devices.size() + " 台\n");
        devices.forEach((id, device) -> {
            display.append("\n#").append(id).append(" ").append(device.getName()).append("\n");
            values.get(id).forEach((control, value) -> display.append(control).append("  ").append(value).append("\n"));
        });
        live.setText(display);
        status.setText((recording ? "記録中  " : "") + events.length() + " 件" + (truncated ? "（上限到達）" : ""));
        record.setText(recording ? "記録停止" : "記録開始");
        marker.setEnabled(recording);
        share.setEnabled(!recording && events.length() > 0);
        model.setEnabled(!recording);
    }

    private void shareLog() {
        try {
            JSONObject report = new JSONObject().put("schemaVersion", 1).put("platform", "android")
                .put("osVersion", Build.VERSION.RELEASE).put("appVersion", getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName)
                .put("controllerModel", recordedModel).put("truncated", truncated).put("events", events);
            File file = new File(getCacheDir(), "gamepad-check-" + System.currentTimeMillis() + ".json");
            Files.write(file.toPath(), report.toString(2).getBytes(StandardCharsets.UTF_8));
            Uri uri = Uri.parse("content://org.pokecom.gamepadcheck.logs/" + file.getName());
            Intent intent = new Intent(Intent.ACTION_SEND).setType("application/json")
                .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri(file.getName(), uri));
            startActivity(Intent.createChooser(intent, "診断結果を共有"));
        } catch (Exception error) {
            Toast.makeText(this, "共有できませんでした: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
