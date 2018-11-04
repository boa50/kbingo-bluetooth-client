package br.com.boa50.kbingobluetoothclient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private EditText etCartelaNumero;
    private Button btSend;
    private Button btConnect;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private StringBuffer mOutStringBuffer;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        etCartelaNumero = findViewById(R.id.et_cartela_numero);
        btSend = findViewById(R.id.bt_send);
        btConnect = findViewById(R.id.bt_connect);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth indisponível",
                    Toast.LENGTH_LONG).show();
            this.finish();
        }

        if (mHandler == null) {
            mHandler = new ServiceHandler(getApplicationContext());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mBluetoothService == null) {
            setupService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == Constants.STATE_NONE) {
                mBluetoothService.start();
            }
        }
    }

    private void setupService() {
        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!"".equals(etCartelaNumero.getText().toString())){
                    sendMessage(etCartelaNumero.getText().toString());
                    etCartelaNumero.setText("");

                    InputMethodManager inputMethodManager =
                            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

                    assert inputMethodManager != null;
                    assert getCurrentFocus() != null;
                    inputMethodManager.hideSoftInputFromWindow(
                            getCurrentFocus().getWindowToken(), 0);
                }
            }
        });

        btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
        });

        mBluetoothService = new BluetoothService(mHandler);
        mOutStringBuffer = new StringBuffer();
    }

    private void sendMessage(String message) {
        if (mBluetoothService.getState() != Constants.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(), "Não está conectado",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
            mOutStringBuffer.setLength(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    assert data != null;
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupService();
                } else {
                    Log.d(TAG, "Bluetooth not enabled");
                    Toast.makeText(getApplicationContext(), "Bluetooth não ativado",
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
        }
    }

    private void connectDevice(Intent data) {
        assert data.getExtras() != null;
        String address = data.getExtras().getString(Constants.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mBluetoothService.connect(device);
    }
}
