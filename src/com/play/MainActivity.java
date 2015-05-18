package com.play;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.play.utils.BluetoothUtils;

import android.R.integer;
import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RemoteViewsService;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private final static String TAG = "Play";
	private final static String PASSWORD = "0";
	private final static String DEVICE = "S-35";

	private Button checkBtn;
	private Button searchBtn;
	private Button playBtn;
	private LinearLayout statusContent;
	private Context mContext;
	private BluetoothAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		initView();
	}

	private void initView() {
		checkBtn = (Button) findViewById(R.id.checkDevice);
		searchBtn = (Button) findViewById(R.id.searchDevice);
		playBtn = (Button) findViewById(R.id.playDevice);
		statusContent = (LinearLayout) findViewById(R.id.statusContent);
		mContext = getApplicationContext();
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		checkBtn.setOnClickListener(this);
		searchBtn.setOnClickListener(this);
		playBtn.setOnClickListener(this);
	}

	private void loggerMsg(String msg) {
		TextView textView = new TextView(mContext);
		textView.setText(msg);
		statusContent.addView(textView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		// 检查设备是否支持蓝牙，支持设备检查是否开启
		case R.id.checkDevice:
			if (mAdapter == null) {
				loggerMsg("设备不支持蓝牙!请更换设备!");
			} else {
				loggerMsg("设备支持蓝牙!");
				loggerMsg("开始检查是否开启.....");
				if (!mAdapter.isEnabled()) {
					loggerMsg("蓝牙未开启!");
					loggerMsg("正在尝试打开蓝牙....");
					Intent mIntent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_ENABLE);
					mIntent.putExtra(
							BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 2000);
					mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(mIntent);
					loggerMsg("蓝牙启动成功！");

				} else {
					loggerMsg("蓝牙已开启!");
				}
			}
			break;
		// 搜索蓝牙设备，发现对应的设备连接
		case R.id.searchDevice:
			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothDevice.ACTION_FOUND);
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
			filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
			filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
			filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			loggerMsg("开始搜索附近的设备......");
			mContext.registerReceiver(BlueReceiver, filter);
			mAdapter.startDiscovery();
			break;
		case R.id.playDevice:
			loggerMsg("laishi");
			break;
		default:
			break;
		}

	}

	private BroadcastReceiver BlueReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				loggerMsg("发现蓝牙设备！");
				BluetoothDevice tempDevice = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				loggerMsg("设备名称:" + tempDevice.getName());
				loggerMsg("设备地址:" + tempDevice.getAddress());
				BluetoothSocket socket = null;
				try {
					Method m = tempDevice.getClass().getMethod(
							"createRfcommSocket", new Class[] { int.class });
					socket = (BluetoothSocket) m.invoke(tempDevice, 1);// 这里端口为1
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}

				try {
					socket.connect();

				} catch (IOException e1) {

					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					socket = null;
				}
			}

		}

	};
}
