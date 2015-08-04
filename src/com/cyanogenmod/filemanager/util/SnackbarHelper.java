package com.cyanogenmod.filemanager.util;


import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.cyanogenmod.filemanager.R;

public final class SnackbarHelper {

    private SnackbarHelper(){}


    public static void showWithRetry(Context context, View container, String msg,
            final ExceptionUtil.OnRelaunchCommandResult listener) {
        Snackbar.make(container, msg,
                Snackbar.LENGTH_LONG)
                .setAction(context.getString(R.string.snackbar_retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onSuccess();
                        }
                    }
                }).show();
    }

    public static void showWithUpgrade(Context context, View container, String msg,
            final View.OnClickListener upgradeClickListener) {
        Snackbar.make(container, msg,
                Snackbar.LENGTH_LONG)
                .setAction(context.getString(R.string.snackbar_upgrade), upgradeClickListener)
                .setActionTextColor(context.getResources()
                        .getColor(R.color.snackbar_upgrade_color)).show();
    }
}
