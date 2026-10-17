package io.github.godsarmy.mlmarkdown.sample;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Window setup for the sample's light canvas, including when the device uses dark mode. */
final class SampleEdgeToEdge {
    private SampleEdgeToEdge() {}

    static void enable(AppCompatActivity activity) {
        EdgeToEdge.enable(
                activity,
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Keep three-button navigation transparent instead of adding a contrast scrim.
            activity.getWindow().setNavigationBarContrastEnforced(false);
        }
    }

    static void applyContentInsets(AppCompatActivity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        applyInsets(content.getChildAt(0));
    }

    static void applyScrollingContentInsets(AppCompatActivity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        View root = content.getChildAt(0);
        // The scroll pages handle their own bottom inset so content can pass behind navigation.
        root.setPadding(root.getPaddingLeft(), root.getPaddingTop(), root.getPaddingRight(), 0);
        applyInsets(root, false);
    }

    static void applyInsets(View root) {
        applyInsets(root, true);
    }

    private static void applyInsets(View root, boolean insetBottom) {
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {
                    Insets safeInsets =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout()
                                            | WindowInsetsCompat.Type.ime());
                    view.setPadding(
                            left + safeInsets.left,
                            top + safeInsets.top,
                            right + safeInsets.right,
                            bottom + (insetBottom ? safeInsets.bottom : 0));
                    return windowInsets;
                });
        ViewCompat.requestApplyInsets(root);
    }
}
