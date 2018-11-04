package br.com.boa50.kbingobluetoothclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static br.com.boa50.kbingobluetoothclient.Constants.STATE_CONNECTED;
import static br.com.boa50.kbingobluetoothclient.Constants.STATE_CONNECTING;
import static br.com.boa50.kbingobluetoothclient.Constants.STATE_NONE;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private final UUID APP_UUID = UUID.fromString("dcf36f3b-df12-4b9f-8d7a-a0a0409febb7");

    private final Handler handler;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;

    BluetoothService(Handler handler) {
        state = STATE_NONE;
        this.handler = handler;
    }

    public synchronized void start() {
        cancelThreads();
    }

    synchronized void connect(BluetoothDevice device) {
        cancelThreads();
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        cancelThreads();
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        sendToastMessage("Connected to: " + device.getName());
    }

    void write(byte[] output) {
        ConnectedThread connectedThread;

        synchronized (this) {
            if (state != STATE_CONNECTED) {
                connectionLost();
                return;
            }
            connectedThread = this.connectedThread;
        }
        connectedThread.write(output);
    }

    synchronized void stop() {
        cancelThreads();
        state = STATE_NONE;
    }

    private void connectionFailed() {
        sendToastMessage("Unable to connect device");
        restartService();
    }

    private void connectionLost() {
        sendToastMessage("Device connection was lost");
        restartService();
    }

    private void cancelThreads() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    private void restartService() {
        state = STATE_NONE;
        BluetoothService.this.start();
    }

    private void sendToastMessage(String message) {
        Message msg = handler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_TOAST, message);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    synchronized int getState() {
        return state;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmpSocket = null;

            try {
                tmpSocket = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            socket = tmpSocket;
            state = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "Running connectThread Socket");

            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (BluetoothService.this) {
                connectThread = null;
            }
            connected(socket, device);
        }

        void cancel() {
            closeBluetoothSocket(socket);
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpInputStream = null;
            OutputStream tmpOutputStream = null;

            try {
                tmpInputStream = socket.getInputStream();
                tmpOutputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Temp sockets streams not created", e);
            }
            inputStream = tmpInputStream;
            outputStream = tmpOutputStream;
            state = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "Running connectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (state == STATE_CONNECTED) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "connectedThread disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        void cancel() {
            closeBluetoothSocket(socket);
        }
    }

    private void closeBluetoothSocket(BluetoothSocket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of socket failed", e);
        }
    }
}
