/*
 * Copyright (C) 2022-2024 Paranoid Android
 * (C) 2023 ArrowOS
 * (C) 2023 The LibreMobileOS Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.app.ActivityTaskManager;
import android.app.ActivityThread;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Binder;
import android.os.Environment;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.R;
import com.android.internal.util.neoteric.KeyProviderManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @hide
 */
public class PropImitationHooks {

    private static final String TAG = "PropImitationHooks";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final String DATA_FILE = "gms_certified_props.json";

    // --- START OF FIX ---
    // These static variables will hold the latest known setting values for the current process.
    // They act as a safe cache for methods called during early boot.
    private static boolean sSpoofPlayIntegrity = true;
    private static boolean sSpoofPhotos = true;
    private static boolean sSpoofGames = true;
    private static boolean sDisableStrongIntegrity = false;
    // --- END OF FIX ---

    private static final String PACKAGE_ARCORE = "com.google.ar.core";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_GMS_UNSTABLE = PACKAGE_GMS + ".unstable";
    private static final String PACKAGE_NETFLIX = "com.netflix.mediaclient";
    private static final String PACKAGE_GPHOTOS = "com.google.android.apps.photos";

    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity");

    // All device prop maps and package sets remain the same...
    private static final Map<String, Object> propsToChangePixelXL;
    private static final Map<String, Object> propsToChangeROG6;
    private static final Map<String, Object> propsToChangeS24U;
    private static final Map<String, Object> propsToChangeLenovoY700;
    private static final Map<String, Object> propsToChangeOP8P;
    private static final Map<String, Object> propsToChangeOP9P;
    private static final Map<String, Object> propsToChangeMI11TP;
    private static final Map<String, Object> propsToChangeMI13P;
    private static final Map<String, Object> propsToChangeF5;
    private static final Map<String, Object> propsToChangeBS4;
    private static final Set<String> packagesToChangePixelXL;
    private static final Set<String> packagesToChangeROG6;
    private static final Set<String> packagesToChangeS24U;
    private static final Set<String> packagesToChangeLenovoY700;
    private static final Set<String> packagesToChangeOP8P;
    private static final Set<String> packagesToChangeOP9P;
    private static final Set<String> packagesToChangeMI11TP;
    private static final Set<String> packagesToChangeMI13P;
    private static final Set<String> packagesToChangeF5;
    private static final Set<String> packagesToChangeBS4;
    private static final Set<String> sNexusFeatures;
    private static final Set<String> sPixelFeatures;
    private static final Set<String> sTensorFeatures;
    // Static initializer blocks for the above remain the same...
    static {
        propsToChangePixelXL = new HashMap<>();
        propsToChangePixelXL.put("BRAND", "google");
        propsToChangePixelXL.put("MANUFACTURER", "Google");
        propsToChangePixelXL.put("DEVICE", "marlin");
        propsToChangePixelXL.put("PRODUCT", "marlin");
        propsToChangePixelXL.put("HARDWARE", "marlin");
        propsToChangePixelXL.put("MODEL", "Pixel XL");
        propsToChangePixelXL.put("ID", "QP1A.191005.007.A3");
        propsToChangePixelXL.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
        propsToChangeROG6 = new HashMap<>();
        propsToChangeROG6.put("BRAND", "asus");
        propsToChangeROG6.put("MANUFACTURER", "asus");
        propsToChangeROG6.put("DEVICE", "AI2201");
        propsToChangeROG6.put("MODEL", "ASUS_AI2201");
        propsToChangeS24U = new HashMap<>();
        propsToChangeS24U.put("BRAND", "samsung");
        propsToChangeS24U.put("DEVICE", "e3q");
        propsToChangeS24U.put("MODEL", "SM-S928B");
        propsToChangeS24U.put("MANUFACTURER", "samsung");
        propsToChangeLenovoY700 = new HashMap<>();
        propsToChangeLenovoY700.put("MODEL", "Lenovo TB-9707F");
        propsToChangeLenovoY700.put("MANUFACTURER", "lenovo");
        propsToChangeOP8P = new HashMap<>();
        propsToChangeOP8P.put("MODEL", "IN2020");
        propsToChangeOP8P.put("MANUFACTURER", "OnePlus");
        propsToChangeOP9P = new HashMap<>();
        propsToChangeOP9P.put("MODEL", "LE2123");
        propsToChangeOP9P.put("MANUFACTURER", "OnePlus");
        propsToChangeMI11TP = new HashMap<>();
        propsToChangeMI11TP.put("MODEL", "2107113SI");
        propsToChangeMI11TP.put("MANUFACTURER", "Xiaomi");
        propsToChangeMI13P = new HashMap<>();
        propsToChangeMI13P.put("BRAND", "Xiaomi");
        propsToChangeMI13P.put("MANUFACTURER", "Xiaomi");
        propsToChangeMI13P.put("MODEL", "2210132C");
        propsToChangeF5 = new HashMap<>();
        propsToChangeF5.put("MODEL", "23049PCD8G");
        propsToChangeF5.put("MANUFACTURER", "Xiaomi");
        propsToChangeBS4 = new HashMap<>();
        propsToChangeBS4.put("MODEL", "2SM-X706B");
        propsToChangeBS4.put("MANUFACTURER", "blackshark");
        packagesToChangePixelXL = Set.of("com.google.android.apps.photos");
        packagesToChangeROG6 = Set.of("com.ea.gp.fifamobile", "com.gameloft.android.ANMP.GloftA9HM", "com.madfingergames.legends", "com.pearlabyss.blackdesertm", "com.pearlabyss.blackdesertm.gl");
        packagesToChangeS24U = Set.of("com.pubg.imobile", "com.pubg.krmobile", "com.rekoo.pubgm", "com.tencent.ig", "com.kurogame.wutheringwaves.global", "com.vng.pubgmobile", "com.proxima.dfm");
        packagesToChangeLenovoY700 = Set.of("com.activision.callofduty.warzone", "com.activision.callofduty.shooter", "com.garena.game.codm", "com.tencent.tmgp.kr.codm", "com.vng.codmvn");
        packagesToChangeOP8P = Set.of("com.netease.lztgglobal", "com.riotgames.league.wildrift", "com.riotgames.league.wildrifttw", "com.riotgames.league.wildriftvn", "com.riotgames.league.teamfighttactics", "com.riotgames.league.teamfighttacticstw", "com.riotgames.league.teamfighttacticsvn");
        packagesToChangeOP9P = Set.of("com.epicgames.fortnite", "com.epicgames.portal", "com.tencent.lolm");
        packagesToChangeMI11TP = Set.of("com.ea.gp.apexlegendsmobilefps", "com.levelinfinite.hotta.gp", "com.supercell.clashofclans", "com.vng.mlbbvn");
        packagesToChangeMI13P = Set.of("com.levelinfinite.sgameGlobal", "com.tencent.tmgp.sgame");
        packagesToChangeF5 = Set.of("com.dts.freefiremax", "com.dts.freefireth", "com.mobile.legends");
        packagesToChangeBS4 = Set.of("com.proximabeta.mf.uamo");
        sNexusFeatures = Set.of("NEXUS_PRELOAD", "nexus_preload", "GOOGLE_BUILD", "GOOGLE_EXPERIENCE", "PIXEL_EXPERIENCE");
        sPixelFeatures = Set.of("PIXEL_2017_PRELOAD", "PIXEL_2018_PRELOAD", "PIXEL_2019_MIDYEAR_PRELOAD", "PIXEL_2019_PRELOAD", "PIXEL_2020_EXPERIENCE", "PIXEL_2020_MIDYEAR_EXPERIENCE");
        sTensorFeatures = Set.of("PIXEL_2021_EXPERIENCE", "PIXEL_2022_EXPERIENCE", "PIXEL_2022_MIDYEAR_EXPERIENCE", "PIXEL_2023_EXPERIENCE", "PIXEL_2023_MIDYEAR_EXPERIENCE", "PIXEL_2024_EXPERIENCE", "PIXEL_2024_MIDYEAR_EXPERIENCE");
    }


    private static volatile List<String> sCertifiedProps = new ArrayList<>();
    private static volatile String sStockFp, sNetflixModel;

    private static volatile String sProcessName;
    private static volatile boolean sIsPixelDevice, sIsGms, sIsFinsky, sIsPhotos;

    public static void setProps(Context context) {
        final String packageName = context.getPackageName();
        final String processName = Application.getProcessName();

        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(processName)) {
            Log.e(TAG, "Null package or process name");
            return;
        }

        // --- START OF FIX ---
        // Remove the aggressive caching. Read the settings every time a process context is available.
        // This is safe and ensures long-running processes get the latest settings.
        try {
            sSpoofPlayIntegrity = Settings.Secure.getInt(context.getContentResolver(), "spoof_play_integrity", 1) == 1;
            sDisableStrongIntegrity = Settings.Secure.getInt(context.getContentResolver(), "gms_cert_chain", 0) == 1;
            sSpoofPhotos = Settings.Secure.getInt(context.getContentResolver(), "spoof_photos", 1) == 1;
            sSpoofGames = Settings.Secure.getInt(context.getContentResolver(), "spoof_games", 1) == 1;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read settings in setProps, using cached values.", e);
        }
        // --- END OF FIX ---

        final Resources res = context.getResources();
        if (res == null) {
            Log.e(TAG, "Null resources");
            return;
        }

        sStockFp = res.getString(R.string.config_stockFingerprint);
        sNetflixModel = res.getString(R.string.config_netflixSpoofModel);

        sProcessName = processName;
        sIsPixelDevice = Build.MANUFACTURER.equals("Google") && Build.MODEL.contains("Pixel");
        sIsGms = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_UNSTABLE);
        sIsFinsky = packageName.equals(PACKAGE_FINSKY);
        sIsPhotos = packageName.equals(PACKAGE_GPHOTOS);

        if (sIsGms || sIsFinsky) {
            if (!android.os.Process.isIsolated()) {
                setPlayIntegrityProps(context);
            } else {
                dlog("Not setting Play Integrity props in isolated process");
            }
        } else if (!sStockFp.isEmpty() && packageName.equals(PACKAGE_ARCORE)) {
            dlog("Setting stock fingerprint for: " + packageName);
            setPropValue("FINGERPRINT", sStockFp);
        } else if (!sNetflixModel.isEmpty() && packageName.equals(PACKAGE_NETFLIX)) {
            dlog("Setting model to " + sNetflixModel + " for Netflix");
            setPropValue("MODEL", sNetflixModel);
        }

        Map<String, Object> propsToChange = new HashMap<>();

        if (sSpoofPhotos && packagesToChangePixelXL.contains(packageName)) {
            propsToChange.putAll(propsToChangePixelXL);
        }

        if (sSpoofGames) {
            // All game spoofing logic remains here
            if (packagesToChangeROG6.contains(packageName)) {
                propsToChange.putAll(propsToChangeROG6);
            } else if (packagesToChangeS24U.contains(packageName)) {
                propsToChange.putAll(propsToChangeS24U);
            } else if (packagesToChangeLenovoY700.contains(packageName)) {
                propsToChange.putAll(propsToChangeLenovoY700);
            } else if (packagesToChangeOP8P.contains(packageName)) {
                propsToChange.putAll(propsToChangeOP8P);
            } else if (packagesToChangeOP9P.contains(packageName)) {
                propsToChange.putAll(propsToChangeOP9P);
            } else if (packagesToChangeMI11TP.contains(packageName)) {
                propsToChange.putAll(propsToChangeMI11TP);
            } else if (packagesToChangeMI13P.contains(packageName)) {
                propsToChange.putAll(propsToChangeMI13P);
            } else if (packagesToChangeF5.contains(packageName)) {
                propsToChange.putAll(propsToChangeF5);
            } else if (packagesToChangeBS4.contains(packageName)) {
                propsToChange.putAll(propsToChangeBS4);
            }
        }

        if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
        for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
            String key = prop.getKey();
            Object value = prop.getValue();
            if (DEBUG) Log.d(TAG, "Defining " + key + " prop for: " + packageName);
            setPropValue(key, value);
        }
    }
    
    // All other methods from your original file...
    private static void setPropValue(String key, Object value) {
        setPropValue(key, value.toString());
    }

    private static void setPropValue(String key, String value) {
        try {
            dlog("Setting prop " + key + " to " + value.toString());
            Class clazz = Build.class;
            if (key.startsWith("VERSION.")) {
                clazz = Build.VERSION.class;
                key = key.substring(8);
            }
            Field field = clazz.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, field.getType().equals(Integer.TYPE) ? Integer.parseInt(value) : value);
            field.setAccessible(false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void setPlayIntegrityProps(Context context) {
        if (!sSpoofPlayIntegrity) {
            dlog("Play Integrity spoofing is disabled by master toggle.");
            return;
        }

        if (android.os.Process.isIsolated()) {
            dlog("Skipping setPlayIntegrityProps in isolated process");
            return;
        }
        File dataFile = new File(Environment.getDataSystemDirectory(), DATA_FILE);
        String savedProps = readFromFile(dataFile);

        if (TextUtils.isEmpty(savedProps)) {
            Log.d(TAG, "Parsing props locally - data file unavailable");
            sCertifiedProps = Arrays.asList(context.getResources().getStringArray(R.array.config_certifiedBuildProperties));
        } else {
            Log.d(TAG, "Parsing props fetched by attestation service");
            try {
                JSONObject parsedProps = new JSONObject(savedProps);
                Iterator<String> keys = parsedProps.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = parsedProps.getString(key);
                    sCertifiedProps.add(key + ":" + value);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON data", e);
                Log.d(TAG, "Parsing props locally as fallback");
                sCertifiedProps = Arrays.asList(context.getResources().getStringArray(R.array.config_certifiedBuildProperties));
            }
        }

        if (sCertifiedProps.isEmpty()) {
            dlog("Certified props are not set");
            return;
        }

        final boolean was = isGmsAddAccountActivityOnTop();
        final TaskStackListener taskStackListener = new TaskStackListener() {
            @Override
            public void onTaskStackChanged() {
                final boolean is = isGmsAddAccountActivityOnTop();
                if (is ^ was) {
                    dlog("GmsAddAccountActivityOnTop is:" + is + " was:" + was +
                            ", killing myself!"); // process will restart automatically later
                    Process.killProcess(Process.myPid());
                }
            }
        };

        if (!was) {
            dlog("Spoofing build for GMS / Finsky");
            setCertifiedProps();
        } else {
            dlog("Skip spoofing build for GMS / Finsky, because GmsAddAccountActivityOnTop");
        }

        try {
            ActivityTaskManager.getService().registerTaskStackListener(taskStackListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register task stack listener!", e);
        }
    }

    private static void setCertifiedProps() {
        for (String entry : sCertifiedProps) {
            final String[] fieldAndProp = entry.split(":", 2);
            if (fieldAndProp.length != 2) {
                Log.e(TAG, "Invalid entry in certified props: " + entry);
                continue;
            }
            setPropValue(fieldAndProp[0], fieldAndProp[1]);
        }
    }

    private static boolean isGmsAddAccountActivityOnTop() {
        try {
            final ActivityTaskManager.RootTaskInfo focusedTask =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();

            return focusedTask != null && focusedTask.topActivity != null
                    && focusedTask.topActivity.equals(GMS_ADD_ACCOUNT_ACTIVITY);
        } catch (Exception e) {
            Log.e(TAG, "Unable to get top activity!", e);
        }
        return false;
    }

    public static boolean shouldBypassTaskPermission(Context context) {
        final int callingUid = Binder.getCallingUid();
        try {
            int gmsUid = context.getPackageManager()
                    .getApplicationInfo(PACKAGE_GMS, 0).uid;
            int finskyUid = context.getPackageManager()
                    .getApplicationInfo(PACKAGE_FINSKY, 0).uid;
            dlog("shouldBypassTaskPermission: gmsUid:" + gmsUid +
                    " finskyUid:" + finskyUid +
                    " callingUid:" + callingUid);
            return (callingUid == gmsUid || callingUid == finskyUid);
        } catch (Exception e) {
            Log.e(TAG, "shouldBypassTaskPermission: unable to get gms/finsky uid", e);
            return false;
        }
    }

    private static String readFromFile(File file) {
        StringBuilder content = new StringBuilder();
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading from file", e);
            }
        }
        return content.toString();
    }

    private static boolean isCallerPlayIntegrity() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .map(StackTraceElement::getClassName)
                .anyMatch(name -> name.toLowerCase(Locale.US).contains("droidguard"));
    }

    public static void onEngineGetCertificateChain() {
        if (!sSpoofPlayIntegrity) {
            return;
        }
        if (sDisableStrongIntegrity && KeyProviderManager.isKeyboxAvailable()) {
            dlog("Allowing gms / finsky to get cert chain (strong integrity disabled)");
            return;
        }
        if (isCallerPlayIntegrity()) {
            dlog("Blocked key attestation for play integrity");
            throw new UnsupportedOperationException();
        }
    }

    public static boolean hasSystemFeature(String name, boolean has) {
        if (sSpoofPhotos && sIsPhotos) {
            if (has && !sIsPixelDevice && (sPixelFeatures.stream().anyMatch(name::contains)
                    || sTensorFeatures.stream().anyMatch(name::contains))) {
                dlog("Blocked system feature " + name + " for Google Photos");
                has = false;
            } else if (!has && !sIsPixelDevice && sNexusFeatures.stream().anyMatch(name::contains)) {
                dlog("Enabled system feature " + name + " for Google Photos");
                has = true;
            }
        }
        return has;
    }

    public static boolean isPlayIntegritySpoofingEnabled() {
        return sSpoofPlayIntegrity;
    }

    public static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, "[" + sProcessName + "] " + msg);
    }
}