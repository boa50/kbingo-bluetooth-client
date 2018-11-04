package br.com.boa50.kbingobluetoothclient;

public interface Constants {
    int MESSAGE_READ = 0;
    int MESSAGE_TOAST = 1;

    int STATE_NONE = 0;
    int STATE_CONNECTING = 1;
    int STATE_CONNECTED = 2;

    String EXTRA_TOAST = "toast";
    String EXTRA_DEVICE_ADDRESS = "device_address";
}
