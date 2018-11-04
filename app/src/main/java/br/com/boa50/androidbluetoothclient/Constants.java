package br.com.boa50.androidbluetoothclient;

public interface Constants {
    int MESSAGE_READ = 0;
    int MESSAGE_WRITE = 1;
    int MESSAGE_TOAST = 2;

    int STATE_NONE = 0;
    int STATE_CONNECTING = 1;
    int STATE_CONNECTED = 2;

    String TOAST = "toast";
    String EXTRA_DEVICE_ADDRESS = "device_address";
}
