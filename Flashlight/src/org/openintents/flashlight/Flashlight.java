/* 
 * Copyright (C) 2008-2012 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.flashlight;

import org.openintents.distribution.AboutDialog;
import org.openintents.distribution.DistributionLibraryActivity;
import org.openintents.distribution.DownloadAppDialog;
import org.openintents.intents.FlashlightIntents;
import org.openintents.util.IntentUtils;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v2.os.Build;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class Flashlight extends DistributionLibraryActivity {

	private static final String TAG = "Flashlight";
	private static final boolean debug = true;

	private static final int MENU_COLOR = Menu.FIRST + 1;
	private static final int MENU_SETTINGS = Menu.FIRST + 4;
	private static final int MENU_DISTRIBUTION_START = Menu.FIRST + 100; // MUST
																			// BE
																			// LAST

	private static final int REQUEST_CODE_PICK_COLOR = 1;

	private static final int DIALOG_ABOUT = 1;
	private static final int DIALOG_COLORPICKER_DOWNLOAD = 2;
	private static final int DIALOG_DISTRIBUTION_START = 100; // MUST BE LAST

	private LinearLayout mBackground;
	private View mIcon;
	private TextView mText;
	private boolean mWait = false;
	private boolean incR = true, incB = true, incG = true;
	private String mfreq;
	private int intFreq;
	private int i = 1, fRed, fBlue, fGreen;

	private PowerManager.WakeLock mWakeLock;
	private boolean mWakeLockLocked = false;

	private int mColor;
	private boolean mUseCameraFlash = false;

	private static final int HIDE_ICON = 1;

	private static int mTimeout = 5000;

	private Brightness mBrightness;
	private SeekBar mSeekBar;

	private static boolean mOldClassAvailable;
	private static boolean mNewClassAvailable;

	/* establish whether the "new" class is available to us */
	static {
		try {
			BrightnessOld.checkAvailable();
			mOldClassAvailable = true;
		} catch (Throwable t) {
			mOldClassAvailable = false;
		}
		try {
			BrightnessNew.checkAvailable();
			mNewClassAvailable = true;
		} catch (Throwable t) {
			mNewClassAvailable = false;
		}
	}

	private static boolean mCameraFlashAvailable;
	private CameraFlash mCameraFlash;

	/* establish whether the "new" class is available to us */
	static {
		try {
			CameraFlash.checkAvailable();
			mCameraFlashAvailable = true;
		} catch (Throwable t) {
			mCameraFlashAvailable = false;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "onCreate Called");

		mDistribution.setFirst(MENU_DISTRIBUTION_START,
				DIALOG_DISTRIBUTION_START);

		// Check whether EULA has been accepted
		// or information about new version can be presented.
		if (mDistribution.showEulaOrNewVersion()) {
			return;
		}

		// Turn off the title bar
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			// Remove title in Android versions without ActionBar.
			// (Starting from HONEYCOMB, keep the title so that
			// the action bar remains accessible.)
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}

		setContentView(R.layout.main);

		mBackground = (LinearLayout) findViewById(R.id.background);
		mIcon = (View) findViewById(R.id.icon);
		mText = (TextView) findViewById(R.id.text);
		mSeekBar = (SeekBar) findViewById(R.id.seekBar1);
		mSeekBar.setMax(65);

		mBackground.setOnTouchListener(new View.OnTouchListener() {

			public boolean onTouch(View arg0, MotionEvent arg1) {
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(Flashlight.this);
				if (!(prefs.getString(FlashlightPrefs.PREFKEY_COLOR_OPTIONS,
						FlashlightPrefs.DEFAULT_COLOR_OPTIONS)).equals("1")) {

					if (mUseCameraFlash) {
						lightsOnOff();
					} else {
						if (mIcon.getVisibility() == View.VISIBLE) {
							hideIcon();
						} else {
							showIconForAWhile();
						}
					}
				} else {
					mIcon.setVisibility(View.VISIBLE);
					mText.setVisibility(View.VISIBLE);
				}
				return false;
			}
		});

		mBackground.setOnLongClickListener(new OnLongClickListener() {

			public boolean onLongClick(View v) {
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(Flashlight.this);
				if ((prefs.getString(FlashlightPrefs.PREFKEY_COLOR_OPTIONS,
						FlashlightPrefs.DEFAULT_COLOR_OPTIONS)).equals("1")) {
					if (mWait != true) {
						mHandler.removeCallbacks(mUpdateBackground);
						mWait = true;
					} else {
						mHandler.removeCallbacks(mUpdateBackground);
						mHandler.postDelayed(mUpdateBackground, 1000);
						mWait = false;
					}
				}

				return false;
			}
		});

		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mBrightness.setBrightness((float) (progress * 0.015));
				 Toast.makeText(Flashlight.this, ""+progress,
				 Toast.LENGTH_SHORT).show();
			}
		});

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		// mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE, "Flashlight");

		if (mNewClassAvailable) {
			Log.d(TAG, "Using SDK 1.5 brightness adjustment");
			mBrightness = new BrightnessNew(this);
		} else if (mOldClassAvailable) {
			Log.d(TAG, "Using SDK 1.1 brightness adjustment");
			mBrightness = new BrightnessOld(this);
		} else {
			Log.d(TAG, "No way to change brightness");
			mBrightness = new Brightness();
		}

		final FlashlightState state = (FlashlightState) getLastNonConfigurationInstance();
		if (state != null) {
			mColor = state.mColor;
			hideIcon();
		} else {
			resetMainScreen();
		}
		mBackground.setBackgroundColor(mColor);
		if (mCameraFlashAvailable) {
			mCameraFlash = new CameraFlash();
		}
	}

	private void resetMainScreen() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (prefs.getBoolean(FlashlightPrefs.PREFKEY_USE_CAMERA_FLASH,
				FlashlightPrefs.DEFAULT_USE_CAMERA_FLASH)) {
			mColor = Color.BLACK;
			mUseCameraFlash = true;
			Log.v(TAG, "Use Camera!");
		} else {
			mUseCameraFlash = false;
			Log.v(TAG, "Don't use camera!");
			String pref = prefs.getString(
					FlashlightPrefs.PREFKEY_COLOR_OPTIONS,
					FlashlightPrefs.DEFAULT_COLOR_OPTIONS);
			if (pref.equals("0")) {
				mColor = prefs.getInt(FlashlightPrefs.PREFKEY_SAVED_COLOR,
											Color.WHITE);
			} else if (pref.equals("1")) {
				mfreq = prefs.getString(FlashlightPrefs.PREF_COLOR_FREQ,
						FlashlightPrefs.DEFAULT_PREF_COLOR_FREQ);
				if (!(mfreq.equals("")))
					intFreq = Integer.parseInt(mfreq);
				else
					intFreq = 200;

				mHandler.removeCallbacks(mUpdateBackground);
				mHandler.postDelayed(mUpdateBackground, 1000);
			} else if (pref.equals("2")) {
				mColor = Color.WHITE;
			}
		}
		if (mUseCameraFlash) {
			mText.setText(R.string.cameralight_info);
			showIcon();
		} else {
			mText.setText(R.string.backlight_info);
			showIconForAWhile();
		}
		mBackground.setBackgroundColor(mColor);

	}

	private Runnable mUpdateBackground = new Runnable() {
		public void run() {
			int color;
			Random rnd = new Random();
			// Changes the value slowly by deciding whether to increase or
			// decrease
			if (fRed < 0) {
				incR = true;
			} else if (fRed > 255)
				incR = false;

			if (incR)
				fRed = (fRed + rnd.nextInt(10));
			else
				fRed = (fRed - rnd.nextInt(10));

			if (fBlue < 0)
				incB = true;
			else if (fBlue > 255)
				incB = false;

			if (incB)
				fBlue = (fBlue + rnd.nextInt(5));
			else
				fBlue = (fBlue - rnd.nextInt(5));

			if (fGreen > 255)
				incG = false;
			else if (fGreen < 0) {
				incG = true;
			}

			if (incG)
				fGreen = (fGreen + rnd.nextInt(8));
			else
				fGreen = (fGreen - rnd.nextInt(8));

			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(Flashlight.this);
			mfreq = sp.getString(FlashlightPrefs.PREF_COLOR_FREQ,
					FlashlightPrefs.DEFAULT_PREF_COLOR_FREQ);
			if (!(mfreq.equals("")))
				intFreq = Integer.parseInt(mfreq);
			else
				intFreq = 200;

			// TransitionDrawable transition = (TransitionDrawable)
			// mBackground.getBackground();
			// transition.startTransition(5000);
			// if(i == 1)
			color = Color.argb(255, fRed, fGreen, fBlue);
			mBackground.setBackgroundColor(color);
			mHandler.postDelayed(this, intFreq);
		}
	};

	private void lightsOnOff() {
		if (mCameraFlash != null) {
			if (mCameraFlash.isOff()) {
				lightsOn();
			} else {
				lightsOff();
			}
		}
		showIcon();
	}

	private void lightsOn() {
		if (mCameraFlash != null) {
			mCameraFlash.lightsOn();
		}
		Log.v(TAG, "Activating Camera Light!");
		mText.setText(R.string.cameralight_stop_info);
	}

	private void lightsOff() {
		if (mCameraFlash != null) {
			mCameraFlash.lightsOff();
		}
		Log.v(TAG, "Deactivating Camera Light!");
		mText.setText(R.string.cameralight_info);
	}

	class FlashlightState {
		int mColor;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		FlashlightState state = new FlashlightState();
		state.mColor = this.mColor;
		return state;
	}

	@Override
	protected void onResume() {
		super.onResume();
		resetMainScreen();
		wakeLock();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mHandler.removeCallbacks(mUpdateBackground);
		wakeUnlock();
	}

	@Override
	protected void onStart() {
		super.onStart();

		IntentFilter i = new IntentFilter(
				FlashlightIntents.ACTION_SET_FLASHLIGHT);
		registerReceiver(mReceiver, i);
	}

	@Override
	protected void onStop() {
		super.onStop();

		unregisterReceiver(mReceiver);
	}

	// ///////////////////////////////////////////////////
	// Menu

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_COLOR, 0, R.string.color)
				.setIcon(android.R.drawable.ic_menu_manage)
				.setShortcut('3', 'c');

		menu.add(0, MENU_SETTINGS, 0, R.string.preferences)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setShortcut('4', 'p');

		// Add distribution menu items last.
		mDistribution.onCreateOptionsMenu(menu);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
			MenuInflater mi = getMenuInflater();
			mi.inflate(R.menu.menu, menu);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_COLOR:
			pickColor();
			return true;

		case MENU_SETTINGS:
			showSettingsMenu();
			return true;

		case R.id.item1:
			showSettingsMenu();
			return true;
		}
		return super.onOptionsItemSelected(item);

	}

	@Override
	protected Dialog onCreateDialog(int id) {

		Log.d(TAG, "Called onCreateDialog()");

		switch (id) {
		case DIALOG_ABOUT:
			return new AboutDialog(this);
		case DIALOG_COLORPICKER_DOWNLOAD:
			return new DownloadAppDialog(this,
					R.string.color_picker_modularization_explanation,
					R.string.color_picker, R.string.color_picker_package,
					R.string.color_picker_website);
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);

		Log.d(TAG, "Called onPrepareDialog()");

		switch (id) {
		case DIALOG_COLORPICKER_DOWNLOAD: {
			DownloadAppDialog.onPrepareDialog(this, dialog);
			break;
		}
		default:
			break;
		}
	}

	// ///////////////////////////////////////////////////
	// Other functions

	private void wakeLock() {
		if (!mWakeLockLocked) {
			Log.d(TAG, "WakeLock: locking");
			mWakeLock.acquire();
			mWakeLockLocked = true;

			// android 1.5 magic number meaning "full brightness"
			mBrightness.setBrightness(1f);
			// Log.d(TAG,"brightness>"+Settings.System.getInt(getContentResolver(),
			// Settings.System.SCREEN_BRIGHTNESS, NOT_VALID));
		}
	}

	private void wakeUnlock() {
		if (mWakeLockLocked) {
			lightsOff();
			Log.d(TAG, "WakeLock: unlocking");
			mWakeLock.release();
			mWakeLockLocked = false;

			// android 1.5 magic number meaning "default/user brightness"
			mBrightness.setBrightness(-1f);
		}
	}

	private void showSettingsMenu() {
		startActivity(new Intent(this, FlashlightPrefs.class));
	}

	private void pickColor() {
		Intent i = new Intent();
		i.setAction(FlashlightIntents.ACTION_PICK_COLOR);
		i.putExtra(FlashlightIntents.EXTRA_COLOR, mColor);

		if (IntentUtils.isIntentAvailable(this, i)) {
			startActivityForResult(i, REQUEST_CODE_PICK_COLOR);

		} else {
			showDialog(DIALOG_COLORPICKER_DOWNLOAD);
		}
		// IntentUtils.intentLaunchWithMarketFallback(this, i,
		// REQUEST_CODE_PICK_COLOR,
		// FlashlightIntents.PACKAGE_NAME_COLOR_PICKER);
	}

	/**
	 * Broadcast receiver for ACTION_SET_FLASHLIGHT
	 */
	BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				if (intent.hasExtra(FlashlightIntents.EXTRA_COLOR)) {
					mColor = intent.getIntExtra(FlashlightIntents.EXTRA_COLOR,
							mColor);
					mBackground.setBackgroundColor(mColor);
					if (debug)
						Log.d(TAG, "Receive color " + mColor);
				}
				if (intent.hasExtra(FlashlightIntents.EXTRA_BRIGHTNESS)) {
					float brightness = intent.getFloatExtra(
							FlashlightIntents.EXTRA_BRIGHTNESS, 1f);
					mBrightness.setBrightness(brightness);

					if (debug)
						Log.d(TAG, "Receive brightness " + mBrightness);
				}
			}
		}

	};

	// ///////////////////////////////////////////////////
	// Color changed listener:

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_CODE_PICK_COLOR:
			if (resultCode == RESULT_OK) {
				mColor = data
						.getIntExtra(FlashlightIntents.EXTRA_COLOR, mColor);

				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(this);
				Editor edit = prefs.edit();
				edit.putString("color_options", "0");
				edit.apply();
				String pref = prefs.getString(
						FlashlightPrefs.PREFKEY_COLOR_OPTIONS,
						FlashlightPrefs.DEFAULT_COLOR_OPTIONS);

				if (pref.equals("0")) {
					prefs.edit()
							.putInt(FlashlightPrefs.PREFKEY_SAVED_COLOR, mColor)
							.commit();
				}
				mBackground.setBackgroundColor(mColor);
			}
			break;
		}
	}

	// //////////////////
	// Handler

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == HIDE_ICON) {
				hideIcon();
			}
		}
	};

	/**
	 * Hides icon and notification bar.
	 */
	private void hideIcon() {
		mIcon.setVisibility(View.GONE);
		mText.setVisibility(View.GONE);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	private void showIcon() {
		mIcon.setVisibility(View.VISIBLE);
		mText.setVisibility(View.VISIBLE);

		getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mHandler.removeMessages(HIDE_ICON);
	}

	/**
	 * Shows icon and notification bar, and set timeout for hiding it.
	 */
	private void showIconForAWhile() {
		showIcon();
		mHandler.sendMessageDelayed(mHandler.obtainMessage(HIDE_ICON), mTimeout);
	}

}