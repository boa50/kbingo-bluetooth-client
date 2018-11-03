package br.com.boa50.androidbluetoothclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import static br.com.boa50.androidbluetoothclient.Constants.STATE_CONNECTED;
import static br.com.boa50.androidbluetoothclient.Constants.STATE_CONNECTING;
import static br.com.boa50.androidbluetoothclient.Constants.STATE_NONE;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private final UUID APP_UUID = UUID.fromString("a649f9e9-ce70-4c04-9692-7126573ee50b");

    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    BluetoothService(Handler handler) {
        mState = STATE_NONE;
        mHandler = handler;
    }

    public synchronized void start() {
        killAll();
    }

    synchronized void connect(BluetoothDevice device) {
        killAll();
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        killAll();
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        sendToastMessage("Connected to: " + device.getName());
    }

    void write(byte[] out) {
        ConnectedThread r;

        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                connectionLost();
                return;
            }
            r = mConnectedThread;
        }
        r.write(out);
    }

    synchronized void stop() {
        killAll();
        mState = STATE_NONE;
    }

    private void connectionFailed() {
        sendToastMessage("Unable to connect device");
        restartService();
    }

    private void connectionLost() {
        sendToastMessage("Device connection was lost");
        restartService();
    }

    private void killAll() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    private void restartService() {
        mState = STATE_NONE;
        BluetoothService.this.start();
    }

    private void sendToastMessage(String message) {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, message);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    public synchronized int getState() {
        return mState;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread Socket");

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice);
        }

        void cancel() {
            closeBluetoothSocket(mmSocket);
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            OutputStream tmpOut = null;
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        void cancel() {
            closeBluetoothSocket(mmSocket);
        }
    }

    private void closeBluetoothSocket(BluetoothSocket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect socket failed", e);
        }
    }
}
