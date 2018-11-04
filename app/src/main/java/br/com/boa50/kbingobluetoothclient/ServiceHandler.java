package br.com.boa50.kbingobluetoothclient;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class ServiceHandler extends Handler {
    private WeakReference<Context> mContext;

    ServiceHandler(Context context) {
        mContext = new WeakReference<>(context);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Constants.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMsg = new String(readBuf, 0, msg.arg1);
                Toast.makeText(mContext.get(), "Read: " + readMsg,
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.MESSAGE_TOAST:
                Toast.makeText(mContext.get(), msg.getData().getString(Constants.EXTRA_TOAST),
                        Toast.LENGTH_LONG).show();
                break;
        }
    }
}
