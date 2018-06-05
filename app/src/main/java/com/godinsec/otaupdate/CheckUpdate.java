package com.godinsec.otaupdate;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.godinsec.otaupdate.Util.DeviceInfo;
import com.godinsec.otaupdate.Util.UpdateInfo;
import com.godinsec.otaupdate.XmlParserHelper.NetError;

public class CheckUpdate extends Activity {

	private static final String TAG ="CheckUpdate";


	public static String[]  SEARCH_PATH = {"/sdcard/"};
	public static String OTA_PACKAGE_FILE = "update.zip";
	public static String DEST_OTA_PACKAGE_PATH = "/cache/update.zip";
	public static String RKIMAGE_FILE = "update.img";
	public static final int RKUPDATE_MODE = 1;
	public static final int OTAUPDATE_MODE = 2;
	private static volatile boolean mWorkHandleLocked = false;
	private static volatile boolean mIsNeedDeletePackage = false;

	public static final String EXTRA_IMAGE_PATH = "android.rockchip.update.extra.IMAGE_PATH";
	public static final String EXTRA_IMAGE_VERSION = "android.rockchip.update.extra.IMAGE_VERSION";
	public static final String EXTRA_CURRENT_VERSION = "android.rockchip.update.extra.CURRENT_VERSION";
	public static String DATA_ROOT = "/data/media/0";
	public static String FLASH_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
	public static String SDCARD_ROOT = "/mnt/external_sd";
	public static String USB_ROOT = "/mnt/usb_storage";
	public static String CACHE_ROOT = Environment.getDownloadCacheDirectory().getAbsolutePath();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_check_update);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		Button mmCheckButton;
		Activity mmActivity;
		public PlaceholderFragment() {
		}

		@Override
		public void onActivityCreated(Bundle b){
			super.onActivityCreated(b);
			mmActivity= getActivity();
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_check_update,
					container, false);
			mmCheckButton = (Button) rootView
					.findViewById(R.id.bt_check_update);
			mmCheckButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					checkUpdate();
				}
			});
			return rootView;
		}

		private void checkUpdate() {
			final Activity act = mmActivity;
			new AsyncTask<Void, NetError, UpdateInfo>() {
				ProgressDialog dialog;
				boolean failed = false;
				String[] searchResult = null;

				@Override
				protected void onPreExecute() {
					dialog = new ProgressDialog(act);
					dialog.setMessage(act.getString(R.string.checking_update));
					dialog.setCancelable(false);
					dialog.show();
				}

				@Override
				protected UpdateInfo doInBackground(Void... v) {
					NetError  error = new NetError();
//					ConnectivityManager cm =(ConnectivityManager)act.getSystemService(Context.CONNECTIVITY_SERVICE);
//					NetworkInfo ni = cm.getActiveNetworkInfo();
//					if (ni != null) {
//						DeviceInfo device = XmlParserHelper.getDeviceInfo(error);
//						List<UpdateInfo> updates = new ArrayList<UpdateInfo>();
//						if (device != null)
//							updates = XmlParserHelper.getUpdateInfos(device.url, Util.SYS_VERSION, error);
//						Util.logd(TAG, "updates list size is " + updates.size());
//						if (updates.size() > 0) {
//							return updates.get(0);
//						}
//					} else {
//						error.code = -1;
//						error.msg = act.getString(R.string.no_network);
//					}
//					if(error.code != 0)
//						publishProgress(error);

					///TODO: check  /sdcard/update.zip
					DeviceInfo device = XmlParserHelper.getDeviceInfo(error);
					List<UpdateInfo> updates = new ArrayList<UpdateInfo>();
					if (device != null)
					for ( String dir_path : SEARCH_PATH) {
						String filePath = dir_path + OTA_PACKAGE_FILE;
						Log.i(TAG,"getValidFirmwareImageFile() : Target image file path : " + filePath);
						if ((new File(filePath)).exists()) {

							 ///TODO: copy file to /cache/update.zip
							File src = new File(filePath);
							File dst = new File(DEST_OTA_PACKAGE_PATH);
							try{
								Util.copy(src,dst);
							}catch (IOException e) {
								Log.i(TAG, "copy error :" + e.getMessage());
							}

//							final byte[] buf = new byte[1024];
//							try {
//								InputStream inputStream = new FileInputStream(src);
//								OutputStream outputStream = new FileOutputStream(dst);
//								int bufferSize;
//								final int size = inputStream.available();
//								long alreadyCopied = 0;
//								while ((bufferSize = inputStream.read(buf)) > 0 && canRun.get()) {
//									alreadyCopied += bufferSize;
//									outputStream.write(buf, 0, bufferSize);
//									alreadyCopied += bufferSize;
//									publishProgress(1.0d * alreadyCopied / size);
//								}
//							} catch (final IOException e) {
//								e.printStackTrace();
//							} finally {
//								try {
//									outputStream.flush();
//									outputStream.getFD().sync();
//								} catch (IOException e) {
//									e.printStackTrace();
//								}
//							}

							break;
						}
					}
					return null;
				}

				@Override
				protected void onProgressUpdate(NetError... nes){
					failed = true;
					String title = act.getString(R.string.notice),
							ok = act.getString(R.string.ok);
					AlertDialog.Builder b = new AlertDialog.Builder(act);
					b.setTitle(title).setMessage(nes[0].msg + ". (" + nes[0].code + ")")
							.setPositiveButton(ok, null);
					b.show();
				}

				@Override
				protected void onPostExecute(UpdateInfo update){
					dialog.dismiss();
					dialog = null;
					if (!failed) {
						showUpdateInfo(update);

					}
				}
			}.execute();
		}

		void showUpdateInfo(final UpdateInfo ui){
			String title = mmActivity.getString(R.string.app_name);
			String msg = mmActivity.getString(R.string.has_no_update);
			String buttonMsg = mmActivity.getString(R.string.ok);
			if(ui != null){
				msg = ui.toString();
				buttonMsg=mmActivity.getString(R.string.download);
			}
			AlertDialog.Builder b = new AlertDialog.Builder(mmActivity);
			b.setTitle(title).setMessage(msg)
					.setPositiveButton(buttonMsg,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									if (ui != null)
										startDownload(ui);
								}
							}
					);
			b.show();
		}

		void startDownload(UpdateInfo ui) {
//	        DownloadManager dm=((DownloadManager)mmActivity.getSystemService("download"));
//	        Uri uri = Uri.parse(ui.url);
//	        Request dwreq = new DownloadManager.Request(uri);
//	        dwreq.setTitle(getString(R.string.download_title));
//	        dwreq.setDescription(getString(R.string.download_description));
//	        // hide API , only build from android source. not for 3th app.
//	        dwreq.setDestinationToSystemCache();
//	        dwreq.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
//
//	        long id = dm.enqueue(dwreq);
//	        Util.putDownloadInfo(mmActivity, id, ui);
		}



		private String[] getValidFirmwareImageFile(String searchPaths[]) {
			for ( String dir_path : searchPaths) {
				String filePath = dir_path + OTA_PACKAGE_FILE;
				Log.i(TAG,"getValidFirmwareImageFile() : Target image file path : " + filePath);
				if ((new File(filePath)).exists()) {
					return (new String[] {filePath} );
				}
			}
//
//			//find rkimage
//			for ( String dir_path : searchPaths) {
//				String filePath = dir_path + RKIMAGE_FILE;
//				//LOG("getValidFirmwareImageFile() : Target image file path : " + filePath);
//
//				if ( (new File(filePath) ).exists() ) {
//					return (new String[] {filePath} );
//				}
//			}
//
//			if(mIsSupportUsbUpdate) {
//				//find usb device update package
//				File usbRoot = new File(USB_ROOT);
//				if(usbRoot.listFiles() == null) {
//					return null;
//				}
//
//				for(File tmp : usbRoot.listFiles()) {
//					if(tmp.isDirectory()) {
//						File[] files = tmp.listFiles(new FileFilter() {
//
//							@Override
//							public boolean accept(File arg0) {
//								Log.i(TAG,"scan usb files: " + arg0.getAbsolutePath());
//								if(arg0.isDirectory()) {
//									return false;
//								}
//
//								if(arg0.getName().equals(RKIMAGE_FILE) || arg0.getName().equals(OTA_PACKAGE_FILE)){
//									return true;
//								}
//								return false;
//							}
//
//						});
//
//						if(files != null && files.length > 0) {
//							return new String[] {files[0].getAbsolutePath()};
//						}
//					}
//				}
//			}

			return null;
		}
	}

}
