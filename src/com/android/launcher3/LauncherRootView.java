package com.android.launcher3;

import static com.android.launcher3.util.SystemUiController.FLAG_DARK_NAV;
import static com.android.launcher3.util.SystemUiController.UI_STATE_ROOT_VIEW;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;

import com.android.launcher3.util.Themes;

public class LauncherRootView extends InsettableFrameLayout {

    private final Launcher mLauncher;

    private final Paint mOpaquePaint;
    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mDrawSideInsetBar;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mLeftInsetBarWidth;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mRightInsetBarWidth;

    private View mAlignedView;
    private WindowStateListener mWindowStateListener;

    public LauncherRootView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mOpaquePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOpaquePaint.setColor(Color.BLACK);
        mOpaquePaint.setStyle(Paint.Style.FILL);

        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    protected void onFinishInflate() {
        if (getChildCount() > 0) {
            // LauncherRootView contains only one child, which should be aligned
            // based on the horizontal insets.
            mAlignedView = getChildAt(0);
        }
        super.onFinishInflate();
    }

    @TargetApi(23)
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        mDrawSideInsetBar = (insets.right > 0 || insets.left > 0) &&
                (!Utilities.ATLEAST_MARSHMALLOW ||
                        getContext().getSystemService(ActivityManager.class).isLowRamDevice());
        if (mDrawSideInsetBar) {
            mLeftInsetBarWidth = insets.left;
            mRightInsetBarWidth = insets.right;
            insets = new Rect(0, insets.top, 0, insets.bottom);
        } else {
            mLeftInsetBarWidth = mRightInsetBarWidth = 0;
        }
        mLauncher.getSystemUiController().updateUiState(
                UI_STATE_ROOT_VIEW, mDrawSideInsetBar ? FLAG_DARK_NAV : 0);

        // Update device profile before notifying th children.
        mLauncher.getDeviceProfile().updateInsets(insets);
        boolean resetState = !insets.equals(mInsets);
        setInsets(insets);

        if (mAlignedView != null) {
            // Apply margins on aligned view to handle left/right insets.
            MarginLayoutParams lp = (MarginLayoutParams) mAlignedView.getLayoutParams();
            if (lp.leftMargin != mLeftInsetBarWidth || lp.rightMargin != mRightInsetBarWidth) {
                lp.leftMargin = mLeftInsetBarWidth;
                lp.rightMargin = mRightInsetBarWidth;
                mAlignedView.setLayoutParams(lp);
            }
        }
        if (resetState) {
            mLauncher.getStateManager().reapplyState(true /* cancelCurrentAnimation */);
        }

        return true; // I'll take it from here
    }

    @Override
    public void setInsets(Rect insets) {
        // If the insets haven't changed, this is a no-op. Avoid unnecessary layout caused by
        // modifying child layout params.
        if (!insets.equals(mInsets)) {
            super.setInsets(insets);
        }
        setBackground(insets.top == 0 ? null
                : Themes.getAttrDrawable(getContext(), R.attr.workspaceStatusBarScrim));
    }

    public void dispatchInsets() {
        mLauncher.getDeviceProfile().updateInsets(mInsets);
        super.setInsets(mInsets);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        // If the right inset is opaque, draw a black rectangle to ensure that is stays opaque.
        if (mDrawSideInsetBar) {
            if (mRightInsetBarWidth > 0) {
                int width = getWidth();
                canvas.drawRect(width - mRightInsetBarWidth, 0, width, getHeight(), mOpaquePaint);
            }
            if (mLeftInsetBarWidth > 0) {
                canvas.drawRect(0, 0, mLeftInsetBarWidth, getHeight(), mOpaquePaint);
            }
        }
    }

    public void setWindowStateListener(WindowStateListener listener) {
        mWindowStateListener = listener;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (mWindowStateListener != null) {
            mWindowStateListener.onWindowFocusChanged(hasWindowFocus);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (mWindowStateListener != null) {
            mWindowStateListener.onWindowVisibilityChanged(visibility);
        }
    }

    public interface WindowStateListener {

        void onWindowFocusChanged(boolean hasFocus);

        void onWindowVisibilityChanged(int visibility);
    }
}