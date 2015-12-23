/*
 * Copyright (C) 2015 Doug Melton
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

package com.dgmltn.upnpbrowser;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends ActionBarActivity {

	private static final String TAG = MainActivity.class.getSimpleName();

	UPnPDeviceAdapter mAdapter;

	@InjectView(R.id.recycler)
	protected RecyclerView vRecycler;

	@InjectView(R.id.spinner)
	protected View vSpinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.inject(this);

		mAdapter = new UPnPDeviceAdapter(this);
		vRecycler.setAdapter(mAdapter);
		vRecycler.setLayoutManager(new LinearLayoutManager(this));
		vRecycler.setVisibility(View.INVISIBLE);
		vSpinner.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onStart() {
		super.onStart();

		new UPnPDeviceFinder().observe()
			.filter(new Func1<UPnPDevice, Boolean>() {
				@Override
				public Boolean call(UPnPDevice device) {
					try {
						device.downloadSpecs();
					}
					catch (Exception e) {
						// Ignore errors
						Log.w(TAG, "Error: " + e);
					}
					return true;
				}
			})
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(new Action1<UPnPDevice>() {
				@Override
				public void call(UPnPDevice device) {
					// This is the first device found.
					if (mAdapter.getItemCount() == 0) {
						vSpinner.animate()
							.alpha(0f)
							.setDuration(1000)
							.setInterpolator(new AccelerateInterpolator())
							.start();

						vRecycler.setAlpha(0f);
						vRecycler.setVisibility(View.VISIBLE);
						vRecycler.animate()
							.alpha(1f)
							.setDuration(1000)
							.setStartDelay(1000)
							.setInterpolator(new DecelerateInterpolator())
							.start();
					}

					mAdapter.add(device);
				}
			});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_info:
			startActivity(AboutActivity.createIntent(this));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
