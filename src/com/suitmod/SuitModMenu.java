package com.suitmod;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

public class SuitModMenu implements Application.ActivityLifecycleCallbacks {

    static final String PREFS      = "suitmod";
    static final String KEY_HEAD   = "sel_head";
    static final String KEY_UPPER  = "sel_upper";
    static final String KEY_LOWER  = "sel_lower";
    static final String KEY_SHOES  = "sel_shoes";
    static final String TAG        = "SuitMod";

    static final String[] SLOT_LABELS = { "Голова", "Верх", "Низ", "Кроссовки" };
    static final int      SLOT_COUNT  = 4;
    static final int MAX_SUITS = 40;

    private Activity        mActivity;
    private FrameLayout     mOverlay;
    private LinearLayout    mPanel;
    private Button          mToggleBtn;
    private boolean         mVisible = false;
    private int[] mSelected = new int[SLOT_COUNT];

    public static class Installer implements Application.ActivityLifecycleCallbacks {
        private final WeakHashMap<Activity, SuitModMenu> mInstalled = new WeakHashMap<>();

        @Override
        public void onActivityCreated(Activity a, Bundle b) {
            if (mInstalled.containsKey(a)) return;
            SuitModMenu menu = new SuitModMenu();
            menu.attach(a);
            mInstalled.put(a, menu);
        }

        @Override public void onActivityStarted(Activity a) {}
        @Override public void onActivityResumed(Activity a) {}
        @Override public void onActivityPaused(Activity a) {}
        @Override public void onActivityStopped(Activity a) {}
        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        @Override public void onActivityDestroyed(Activity a) { mInstalled.remove(a); }
    }

    void attach(Activity a) {
        mActivity = a;
        a.getWindow().getDecorView().getViewTreeObserver()
            .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                boolean done = false;
                @Override public void onGlobalLayout() {
                    if (done) return;
                    ViewGroup root = findRoot(a);
                    if (root == null) return;
                    done = true;
                    loadPrefs(a);
                    buildOverlay(a, root);
                    Log.d(TAG, "Overlay attached");
                }
            });
    }

    ViewGroup findRoot(Activity a) {
        ViewGroup dec = (ViewGroup) a.getWindow().getDecorView();
        ViewGroup result = findUnityView(dec);
        return result != null ? result : dec;
    }

    ViewGroup findUnityView(ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View v = vg.getChildAt(i);
            String name = v.getClass().getSimpleName();
            if (name.contains("Unity") || name.contains("GL") || name.contains("Surface")) {
                return vg; 
            }
            if (v instanceof ViewGroup) {
                ViewGroup r = findUnityView((ViewGroup) v);
                if (r != null) return r;
            }
        }
        return null;
    }

    // ИСПРАВЛЕННЫЙ СЛУШАТЕЛЬ: теперь он сам открывает меню при клике
    View.OnTouchListener createDragListener(View targetView, boolean isToggleButton) {
        return new View.OnTouchListener() {
            private float initialX, initialY;
            private int initialLeftMargin, initialTopMargin;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) targetView.getLayoutParams();
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = event.getRawX();
                        initialY = event.getRawY();
                        initialLeftMargin = params.leftMargin;
                        initialTopMargin = params.topMargin;
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialX;
                        float deltaY = event.getRawY() - initialY;
                        
                        // Если сдвинули больше чем на 15 пикселей, включаем перетаскивание
                        if (!isDragging && (Math.abs(deltaX) > 15 || Math.abs(deltaY) > 15)) {
                            isDragging = true;
                        }
                        
                        if (isDragging) {
                            params.leftMargin = initialLeftMargin + (int) deltaX;
                            params.topMargin = initialTopMargin + (int) deltaY;
                            targetView.setLayoutParams(params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Если палец не двигался (это был тап) и это кнопка меню - открываем/закрываем
                        if (!isDragging && isToggleButton) {
                            mVisible = !mVisible;
                            mPanel.setVisibility(mVisible ? View.VISIBLE : View.GONE);
                        }
                        return true;
                }
                return false;
            }
        };
    }

    void buildOverlay(Activity a, ViewGroup root) {
        DisplayMetrics dm = new DisplayMetrics();
        a.getWindowManager().getDefaultDisplay().getMetrics(dm);
        float dp = dm.density;
        int screenWidth = dm.widthPixels;

        // Кнопка 🕷
        mToggleBtn = new Button(a);
        mToggleBtn.setText("🕷");
        mToggleBtn.setTextSize(18);
        style(mToggleBtn, 0xCC1A1A2E, 0xFFE94560);
        mToggleBtn.setAllCaps(false);

        FrameLayout.LayoutParams toggleLP = new FrameLayout.LayoutParams((int)(50*dp), (int)(50*dp));
        toggleLP.gravity = Gravity.TOP | Gravity.START;
        toggleLP.topMargin  = (int)(80*dp);
        toggleLP.leftMargin = screenWidth - (int)(50*dp) - (int)(16*dp);
        mToggleBtn.setLayoutParams(toggleLP);
        
        // Вешаем наш умный слушатель (таскает + открывает)
        mToggleBtn.setOnTouchListener(createDragListener(mToggleBtn, true));

        // Панель
        mPanel = new LinearLayout(a);
        mPanel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(0xEE0D0D1A);
        panelBg.setCornerRadius(12*dp);
        mPanel.setBackground(panelBg);
        mPanel.setPadding((int)(12*dp),(int)(12*dp),(int)(12*dp),(int)(12*dp));

        FrameLayout.LayoutParams panelLP = new FrameLayout.LayoutParams((int)(220*dp), ViewGroup.LayoutParams.WRAP_CONTENT);
        panelLP.gravity = Gravity.TOP | Gravity.START;
        panelLP.topMargin  = (int)(140*dp);
        panelLP.leftMargin = screenWidth - (int)(220*dp) - (int)(16*dp);
        mPanel.setLayoutParams(panelLP);

        // Заголовок (за него тоже можно таскать)
        TextView title = label(a, "🕷 Костюм Конструктор", 14, 0xFFE94560);
        title.setPadding(0, (int)(8*dp), 0, (int)(8*dp));
        title.setOnTouchListener(createDragListener(mPanel, false));
        mPanel.addView(title);
        mPanel.addView(divider(a, dp));

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            mPanel.addView(buildSlotRow(a, slot, dp));
        }

        mPanel.addView(divider(a, dp));

        Button apply = new Button(a);
        apply.setText("✅ Применить");
        apply.setTextSize(12);
        apply.setAllCaps(false);
        style(apply, 0xFF16213E, 0xFF0F3460);
        apply.setOnClickListener(v -> applyCustomSuit(a));

        LinearLayout.LayoutParams applyLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(40*dp));
        applyLP.topMargin = (int)(8*dp);
        apply.setLayoutParams(applyLP);
        mPanel.addView(apply);
        mPanel.setVisibility(View.GONE);

        mOverlay = new FrameLayout(a);
        mOverlay.setTag("suitmod_overlay");
        FrameLayout.LayoutParams overlayLP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mOverlay.setLayoutParams(overlayLP);
        mOverlay.addView(mToggleBtn);
        mOverlay.addView(mPanel);

        // УБРАН mToggleBtn.setOnClickListener, чтобы не было конфликтов
        root.addView(mOverlay);
    }

    View buildSlotRow(Activity a, int slot, float dp) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLP.topMargin = (int)(6*dp);
        row.setLayoutParams(rowLP);

        TextView slotLabel = label(a, SLOT_LABELS[slot], 11, 0xFF8899AA);
        row.addView(slotLabel);

        LinearLayout ctrl = new LinearLayout(a);
        ctrl.setOrientation(LinearLayout.HORIZONTAL);
        ctrl.setGravity(Gravity.CENTER_VERTICAL);

        Button prev = new Button(a);
        prev.setText("◀");
        prev.setTextSize(12);
        prev.setAllCaps(false);
        style(prev, 0xFF16213E, 0xFFE94560);
        LinearLayout.LayoutParams btnLP = new LinearLayout.LayoutParams((int)(36*dp), (int)(32*dp));
        prev.setLayoutParams(btnLP);

        final TextView indexTV = new TextView(a);
        indexTV.setText(String.valueOf(mSelected[slot]));
        indexTV.setTextColor(0xFFFFFFFF);
        indexTV.setTextSize(13);
        indexTV.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams idxLP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        indexTV.setLayoutParams(idxLP);

        Button next = new Button(a);
        next.setText("▶");
        next.setTextSize(12);
        next.setAllCaps(false);
        style(next, 0xFF16213E, 0xFFE94560);
        next.setLayoutParams(btnLP);

        prev.setOnClickListener(v -> {
            mSelected[slot] = Math.max(0, mSelected[slot] - 1);
            indexTV.setText(String.valueOf(mSelected[slot]));
            savePrefs(a);
            previewSlot(slot);
        });

        next.setOnClickListener(v -> {
            mSelected[slot] = Math.min(MAX_SUITS - 1, mSelected[slot] + 1);
            indexTV.setText(String.valueOf(mSelected[slot]));
            savePrefs(a);
            previewSlot(slot);
        });

        ctrl.addView(prev);
        ctrl.addView(indexTV);
        ctrl.addView(next);
        row.addView(ctrl);

        return row;
    }

    void applyCustomSuit(Activity a) {
        try {
            Class<?> up = Class.forName("com.unity3d.player.UnityPlayer");
            Method send = up.getMethod("UnitySendMessage", String.class, String.class, String.class);
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                String msg = slot + ":" + mSelected[slot];
                send.invoke(null, "SuitHolder", "TransferAccessory", msg);
            }
            send.invoke(null, "SuitHolder", "EquipSuitStudio", "");
        } catch (Exception e) {
            Log.e(TAG, "UnitySendMessage failed: " + e.getMessage());
        }
    }

    void previewSlot(int slot) {
        try {
            Class<?> up = Class.forName("com.unity3d.player.UnityPlayer");
            Method send = up.getMethod("UnitySendMessage", String.class, String.class, String.class);
            String msg = slot + ":" + mSelected[slot];
            send.invoke(null, "SuitHolder", "TransferAccessory", msg);
        } catch (Exception ignored) {}
    }

    void loadPrefs(Activity a) {
        SharedPreferences p = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        mSelected[0] = p.getInt(KEY_HEAD,  0);
        mSelected[1] = p.getInt(KEY_UPPER, 0);
        mSelected[2] = p.getInt(KEY_LOWER, 0);
        mSelected[3] = p.getInt(KEY_SHOES, 0);
    }

    void savePrefs(Activity a) {
        SharedPreferences.Editor ed = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        ed.putInt(KEY_HEAD,  mSelected[0]);
        ed.putInt(KEY_UPPER, mSelected[1]);
        ed.putInt(KEY_LOWER, mSelected[2]);
        ed.putInt(KEY_SHOES, mSelected[3]);
        ed.apply();
    }

    void style(Button b, int bg, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg);
        d.setStroke(2, stroke);
        d.setCornerRadius(8);
        b.setBackground(d);
        b.setTextColor(0xFFFFFFFF);
    }

    TextView label(Activity a, String text, int sp, int color) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(sp);
        return tv;
    }

    View divider(Activity a, float dp) {
        View v = new View(a);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.topMargin = lp.bottomMargin = (int)(6*dp);
        v.setLayoutParams(lp);
        v.setBackgroundColor(0x44E94560);
        return v;
    }

    @Override public void onActivityCreated(Activity a, Bundle b) {}
    @Override public void onActivityStarted(Activity a) {}
    @Override public void onActivityResumed(Activity a) {}
    @Override public void onActivityPaused(Activity a) {}
    @Override public void onActivityStopped(Activity a) {}
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
    @Override public void onActivityDestroyed(Activity a) {}
}
