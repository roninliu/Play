package com.play;

import java.lang.reflect.Method;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.IBluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private final static String DEVICE = "S-35";

	private Button checkBtn;
	private Button searchBtn;
	private Button playBtn;
	private Button stopBtn;
	private LinearLayout statusContent;
	private Context mContext;
	private BluetoothAdapter mAdapter;
	private RecordThread rec;
	private boolean isEnd = false;

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
		stopBtn = (Button) findViewById(R.id.stopDevice);
 		statusContent = (LinearLayout) findViewById(R.id.statusContent);
		mContext = getApplicationContext();
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		checkBtn.setOnClickListener(this);
		searchBtn.setOnClickListener(this);
		playBtn.setOnClickListener(this);
		stopBtn.setOnClickListener(this);
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
			loggerMsg("声音测试");
			rec = new RecordThread();
			rec.start();
			break;
		case R.id.stopDevice:
			isEnd = true;
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
				BluetoothDevice tempDevice = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (tempDevice.getName().equalsIgnoreCase(DEVICE)) {
					mAdapter.cancelDiscovery();
					loggerMsg("发现匹配的设备！");
					loggerMsg("设备名称:" + tempDevice.getName());
					loggerMsg("设备地址:" + tempDevice.getAddress());
					int connectState = tempDevice.getBondState();
					switch (connectState) {
					// 未配对
					case BluetoothDevice.BOND_NONE:
						// 尝试配对
						try {
							loggerMsg("开始配对......");
							Method createBondMethod = BluetoothDevice.class
									.getMethod("createBond");
							createBondMethod.invoke(tempDevice);
						} catch (Exception e) {
							loggerMsg("配对失败,请重新尝试....");
							e.printStackTrace();
						}
						break;
					// 已配对
					case BluetoothDevice.BOND_BONDED:
						try {
							loggerMsg("已经配对,开始尝试连接......");
							conntectA2dp(mContext, tempDevice);
						} catch (Exception e) {
							loggerMsg("连接失败,请重试......");
							e.printStackTrace();
						}
						break;

					default:
						break;
					}
				}

			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED
					.equalsIgnoreCase(action)) {

				BluetoothDevice tempDevice = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (tempDevice.getName().equalsIgnoreCase(DEVICE)) {
					loggerMsg("发现匹配的设备！");
					loggerMsg("设备名称:" + tempDevice.getName());
					loggerMsg("设备地址:" + tempDevice.getAddress());
					int connectState = tempDevice.getBondState();
					switch (connectState) {
					case BluetoothDevice.BOND_NONE:
						break;
					case BluetoothDevice.BOND_BONDING:
						break;
					case BluetoothDevice.BOND_BONDED:
						try {
							loggerMsg("已经配对,开始尝试连接......");
							conntectA2dp(mContext, tempDevice);
						} catch (Exception e) {
							loggerMsg("连接失败,请重试......");
							e.printStackTrace();
						}
						break;
					default:
						break;
					}
				}

			}

		}

	};

	class RecordThread extends Thread {
		static final int frequency = 44100;
		static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

		@Override
		public void run() {
			// TODO Auto-generated method stub
			int recBufSize = AudioRecord.getMinBufferSize(frequency,
					channelConfiguration, audioEncoding) * 2;
			int plyBufSize = AudioTrack.getMinBufferSize(frequency,
					channelConfiguration, audioEncoding) * 2;

			AudioRecord audioRecord = new AudioRecord(
					MediaRecorder.AudioSource.MIC, frequency,
					channelConfiguration, audioEncoding, recBufSize);

			AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					frequency, channelConfiguration, audioEncoding, plyBufSize,
					AudioTrack.MODE_STREAM);
			byte[] recBuf = new byte[recBufSize];
			audioRecord.startRecording();
			audioTrack.play();
			while (!isEnd) {
				int readLen = audioRecord.read(recBuf, 0, recBufSize);
				audioTrack.write(recBuf, 0, readLen);
			}
			//audioTrack.stop();
			//audioRecord.stop();

		}
		
	}

	private void conntectA2dp(Context context,
			final BluetoothDevice deviceToConnect) {
		try {
			Class<?> c2 = Class.forName("android.os.ServiceManager");
			Method m2 = c2.getDeclaredMethod("getService", String.class);
			IBinder b = (IBinder) m2.invoke(c2.newInstance(), "bluetooth_a2dp");
			if (b == null) {
				// For Android 4.2 Above Devices
				BluetoothAdapter.getDefaultAdapter().getProfileProxy(context,
						new ServiceListener() {

							@Override
							public void onServiceDisconnected(int profile) {

							}

							@Override
							public void onServiceConnected(int profile,
									BluetoothProfile proxy) {
								BluetoothA2dp a2dp = (BluetoothA2dp) proxy;
								try {
									a2dp.getClass()
											.getMethod("connect",
													BluetoothDevice.class)
											.invoke(a2dp, deviceToConnect);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}, BluetoothProfile.A2DP);
			} else {
				Class<?> c3 = Class.forName("android.bluetooth.IBluetoothA2dp");
				Class<?>[] s2 = c3.getDeclaredClasses();
				Class<?> c = s2[0];
				Method m = c.getDeclaredMethod("asInterface", IBinder.class);
				m.setAccessible(true);
				IBluetoothA2dp a2dp = (IBluetoothA2dp) m.invoke(null, b);
				a2dp.connect(deviceToConnect);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
