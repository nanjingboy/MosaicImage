package me.tom.mosaicimage.utils;

import android.content.Context;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class ToastUtils {

    public static void show(Context context, int messageId) {
        show(context, context.getString(messageId));
    }

    public static void show(Context context, String message) {
        Toast toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
        TextView textView = (TextView) toast.getView().findViewById(android.R.id.message);
        if (textView != null) {
            textView.setGravity(Gravity.CENTER);
        }
        toast.setText(message);
        toast.setGravity(Gravity.BOTTOM, 0, 64);
        toast.show();
    }
}
