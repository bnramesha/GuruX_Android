//
// --------------------------------------------------------------------------
//  Gurux Ltd
//
//
//
// Filename:        $HeadURL$
//
// Version:         $Revision$,
//                  $Date$
//                  $Author$
//
// Copyright (c) Gurux Ltd
//
//---------------------------------------------------------------------------
//
//  DESCRIPTION
//
// This file is a part of Gurux Device Framework.
//
// Gurux Device Framework is Open Source software; you can redistribute it
// and/or modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; version 2 of the License.
// Gurux Device Framework is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the GNU General Public License for more details.
//
// More information of Gurux products: http://www.gurux.org
//
// This code is licensed under the GNU General Public License v2.
// Full text may be retrieved at http://www.gnu.org/licenses/gpl-2.0.txt
//---------------------------------------------------------------------------
package gurux.serial;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gurux.common.GXCommon;
import gurux.common.IGXMediaListener;
import gurux.common.MediaStateEventArgs;
import gurux.common.PropertyChangedEventArgs;
import gurux.common.ReceiveEventArgs;
import gurux.common.TraceEventArgs;
import gurux.common.enums.MediaState;
import gurux.io.BaudRate;
import gurux.io.Parity;
import gurux.io.StopBits;
import gurux.serial.dlms.DlmsJavaClient;

public class MainActivity extends AppCompatActivity implements IGXMediaListener {
    /**
     * List of available serial ports.
     */
    private Spinner portList;
    /**
     * Used baud rate.
     */
    private Spinner baudRate;
    /**
     * Used data bits.
     */
    private Spinner dataBits;
    /**
     * Used parity.
     */
    private Spinner parity;
    /**
     * Used stop bits.
     */
    private Spinner stopBits;

    private Button openBtn;
    private Button sendBtn;
    private GXSerial serial;
    private TextView receivedData,tvLog;
    private EditText sendData;
    private CheckBox hex;

    private DlmsJavaClient client;

    private static final String TAG = "MainActivity";
    /**
     * Read last used settings.
     */
    private void readSettings() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int br = sharedPref.getInt(getString(R.string.baudrate), 9600);
        int pos = ((ArrayAdapter) baudRate.getAdapter()).getPosition(br);
        baudRate.setSelection(pos);
        int db = sharedPref.getInt(getString(R.string.dataBits), 8);
        pos = ((ArrayAdapter) dataBits.getAdapter()).getPosition(db);
        dataBits.setSelection(pos);

        pos = sharedPref.getInt(getString(R.string.parity), 0);
        parity.setSelection(pos);

        pos = sharedPref.getInt(getString(R.string.stopBits), 0);
        stopBits.setSelection(pos);
        hex.setChecked(sharedPref.getBoolean(getString(R.string.Hex), true));
        sendData.setText(sharedPref.getString(getString(R.string.sendData), ""));
    }

    /**
     * Save last used settings.
     */
    private void saveSettings() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.baudrate), ((Integer) baudRate.getSelectedItem()));
        editor.putInt(getString(R.string.dataBits), (Integer) dataBits.getSelectedItem());
        editor.putInt(getString(R.string.parity), ((Parity) parity.getSelectedItem()).ordinal());
        editor.putInt(getString(R.string.stopBits), ((StopBits) stopBits.getSelectedItem()).ordinal());
        editor.putBoolean(getString(R.string.Hex), hex.isChecked());
        editor.putString(getString(R.string.sendData), sendData.getText().toString());
        editor.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        portList = (Spinner) findViewById(R.id.portList);
        baudRate = (Spinner) findViewById(R.id.baudRate);
        dataBits = (Spinner) findViewById(R.id.dataBits);
        parity = (Spinner) findViewById(R.id.parity);
        stopBits = (Spinner) findViewById(R.id.stopBits);
        openBtn = (Button) findViewById(R.id.openBtn);
        sendBtn = (Button) findViewById(R.id.sendBtn);
        receivedData = (TextView) findViewById(R.id.receivedData);
        tvLog = (TextView) findViewById(R.id.tvLog);
        sendData = (EditText) findViewById(R.id.sendData);
        hex = (CheckBox) findViewById(R.id.hex);
        try {
            //Add baud rates.
            List<Integer> rates = new ArrayList<Integer>();
            for (int it : GXSerial.getAvailableBaudRates(null)) {
                rates.add(it);
            }
            ArrayAdapter<Integer> ratesAdapter = new ArrayAdapter<Integer>(this,android.R.layout.simple_spinner_item, rates);
            ratesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            baudRate.setAdapter(ratesAdapter);

            //Add data bits.
            List<Integer> dataBitsList = new ArrayList<Integer>();
            dataBitsList.add(7);
            dataBitsList.add(8);
            ArrayAdapter<Integer> dataBitsAdapter = new ArrayAdapter<Integer>(this,
                    android.R.layout.simple_spinner_item, dataBitsList);
            dataBitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dataBits.setAdapter(dataBitsAdapter);

            //Add Parity.
            List<Parity> parityList = new ArrayList<Parity>();
            parityList.add(Parity.NONE);
            parityList.add(Parity.ODD);
            parityList.add(Parity.EVEN);
            parityList.add(Parity.MARK);
            parityList.add(Parity.SPACE);
            ArrayAdapter<Parity> parityAdapter = new ArrayAdapter<Parity>(this,
                    android.R.layout.simple_spinner_item, parityList);
            parityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            parity.setAdapter(parityAdapter);


            //Add stop bits.
            List<StopBits> stopBitsList = new ArrayList<StopBits>();
            stopBitsList.add(StopBits.ONE);
            stopBitsList.add(StopBits.ONE_POINT_FIVE);
            stopBitsList.add(StopBits.TWO);
            ArrayAdapter<StopBits> stopBitsAdapter = new ArrayAdapter<StopBits>(this,android.R.layout.simple_spinner_item, stopBitsList);
            stopBitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            stopBits.setAdapter(stopBitsAdapter);


            //Add serial ports.
            serial = new GXSerial(this);
            serial.addListener(this);
            client = DlmsJavaClient.newInstance(serial, getApplicationContext());
//            GXSerial cl = new GXSerial(this);
//            cl.setPortNames(gurux.serial.GXSerial.getPortNames(this)[0]);
//            cl.setBaudRate(BaudRate.BAUD_RATE_9600);
//            cl.setDataBits(8);
//            cl.setParity(Parity.ODD);
//            cl.setStopBits(StopBits.ONE);
//            cl.open();


            List<GXPort> ports = new ArrayList<GXPort>(Arrays.asList(serial.getPorts()));

            if (ports.size() == 0) {
                throw new Exception("No serial ports available.");
            }
            ArrayAdapter<GXPort> portsAdapter = new ArrayAdapter<GXPort>(this,android.R.layout.simple_spinner_item, ports);
            portsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            portList.setAdapter(portsAdapter);
            try {
                readSettings();
            } catch (Exception ex) {
                //Select 9600 as default baud rate value.
                baudRate.setSelection(5);
                //Select 8 as default data bit value.
                dataBits.setSelection(1);
                //Select NONE as default parity value.
                parity.setSelection(0);
                //Select ONE as default value.
                stopBits.setSelection(0);
                hex.setChecked(true);
            }

        } catch (Exception ex) {
            openBtn.setEnabled(false);
            showError(ex);
        }
        StringBuilder builder = new StringBuilder("");
        for (byte[] array : client.getData()) {
            builder.append(new String(array));
        }
        sendData.setText(builder.toString());
    }

    private void showError(Exception ex) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(ex.getMessage())
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Open selected serial port.
     */
    public void openSerialPort(View view) {
        try {
            String open = getResources().getString(R.string.open);
            String close = getResources().getString(R.string.close);
            if (openBtn.getText() == open) {
                serial.setPort(((GXPort) portList.getSelectedItem()));
                serial.setBaudRate(BaudRate.forValue((Integer) baudRate.getSelectedItem()));
                serial.setDataBits(Integer.parseInt(dataBits.getSelectedItem().toString()));
                serial.setParity((Parity) parity.getSelectedItem());
                serial.setStopBits((StopBits) stopBits.getSelectedItem());
                serial.setReadTimeout(10000);
                Toast.makeText(getApplicationContext(), "Baud rate selected : " + serial.getBaudRate().getValue(), Toast.LENGTH_LONG).show();
                serial.open();
                try {
                    client.doHandshake();
                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), ex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                serial.close();
            }
        } catch (Exception ex) {
            serial.close();
            showError(ex);
        }
    }

    /**
     * Send data to the serial port.
     */
    public void sendData(View view) {
        try {
            String str = sendData.getText().toString();
            if (hex.isChecked()) {
                serial.send(GXCommon.hexToBytes(str));
            } else {
                serial.send(str);
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /**
     * Send data to the serial port.
     */
    public void findPorts(View view) {
        try {
            List<GXPort> list = new ArrayList<GXPort>();
            for (GXPort it : serial.getPorts()) {
                list.add(it);
            }
            ArrayAdapter<GXPort> dataAdapter = new ArrayAdapter<GXPort>(this,
                    android.R.layout.simple_spinner_item, list);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            portList.setAdapter(dataAdapter);
            if (list.size() == 0) {
                throw new Exception("No serial ports available.");
            }
            openBtn.setEnabled(true);
        } catch (Exception ex) {
            openBtn.setEnabled(false);
            showError(ex);
        }
    }

    /*
     * Show serial port info.
     */
    public void showInfo(View view) {
        try {
            GXPort port = (GXPort) portList.getSelectedItem();
            String info = "";
            if (port != null) {
                info = port.getInfo();
            }
            new AlertDialog.Builder(this)
                    .setTitle("Info")
                    .setMessage(info)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Do nothing.
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        } catch (Exception ex) {
            openBtn.setEnabled(false);
            showError(ex);
        }
    }

    /**
     * Clear received data.
     */
    public void clearData(View view) {
        try {
            receivedData.setText("");
        } catch (Exception ex) {
            openBtn.setEnabled(false);
            showError(ex);
        }
    }

    /**
     * Show occurred exception.
     *
     * @param sender The source of the event.
     * @param ex     Occurred exception.
     */
    @Override
    public void onError(final Object sender, final RuntimeException ex) {
        Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
        tvLog.setText("ERROE");
        showError(ex);
    }

    /**
     * Show received data.
     *
     * @param sender The source of the event.
     * @param e      Received data.
     */
    @Override
    public void onReceived(final Object sender, final ReceiveEventArgs e) {
        Toast.makeText(this, "RECEDIVED", Toast.LENGTH_LONG).show();
        tvLog.setText("RECEIVED");
        if (hex.isChecked()) {
            receivedData.setText(receivedData.getText() + GXCommon.bytesToHex((byte[]) e.getData()));
        } else {
            receivedData.setText(receivedData.getText() + new String((byte[]) e.getData()));
        }
    }

    private void enableUI(boolean open) {
        sendBtn.setEnabled(open);
        portList.setEnabled(!open);
        baudRate.setEnabled(!open);
        dataBits.setEnabled(!open);
        parity.setEnabled(!open);
        stopBits.setEnabled(!open);
    }

    /**
     * Update UI when media state changes.
     *
     * @param sender The source of the event.
     * @param e      Media state event arguments.
     */
    @Override
    public void onMediaStateChange(final Object sender, final MediaStateEventArgs e) {
        Toast.makeText(this, "MEDIA CHANGED", Toast.LENGTH_SHORT).show();
        if (e.getState() == MediaState.OPEN) {
            enableUI(true);
            openBtn.setText(R.string.close);
        } else if (e.getState() == MediaState.CLOSED) {
            enableUI(false);
            openBtn.setText(R.string.open);
        }
    }

    @Override
    public void onTrace(final Object sender, final TraceEventArgs e) {
        Toast.makeText(this, "TRACE", Toast.LENGTH_SHORT).show();
        tvLog.setText("TRACE");
    }

    @Override
    public void onPropertyChanged(Object sender, PropertyChangedEventArgs e) {

        Log.i(TAG, "onPropertyChanged: "+sender);
        Toast.makeText(this, "PROPERTY", Toast.LENGTH_SHORT).show();
        tvLog.setText("PROPERTY");
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        saveSettings();
        if (serial != null) {
            serial.close();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serial != null) {
            serial.close();
            serial = null;
        }
        super.onStop();
    }
}
