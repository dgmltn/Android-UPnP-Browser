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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;

public class UPnPDeviceAdapter extends RecyclerView.Adapter<UPnPDeviceAdapter.ViewHolder> {

	public interface ItemClickListener {
		public void onClick(UPnPDevice item, int position);
	}

	private Comparator<UPnPDevice> mComparator = new UPnPDeviceComparator();

	private LayoutInflater inflater;
	private Picasso picasso;
	private ArrayList<UPnPDevice> mItems;
	private ItemClickListener mListener;

	public UPnPDeviceAdapter(Context context) {
		super();
		inflater = LayoutInflater.from(context);
		picasso = Picasso.with(context);
		picasso.setIndicatorsEnabled(false);
		mItems = new ArrayList<>();
		setHasStableIds(false);
	}

	public void setItemClickListener(ItemClickListener listener) {
		mListener = listener;
	}

	@Override
	public int getItemCount() {
		return mItems.size();
	}

	public UPnPDevice getItem(int position) {
		return mItems.get(position);
	}

	public void clear() {
		int count = mItems.size();
		mItems.clear();
		notifyItemRangeRemoved(0, count);
	}

	public void add(UPnPDevice item) {
		int index = Collections.binarySearch(mItems, item, mComparator);
		if (index < 0) {
			int position = -index - 1;
			mItems.add(position, item);
			notifyItemInserted(position);
		}
		else {
			mItems.set(index, item);
			notifyItemChanged(index);
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
		return new ViewHolder(inflater.inflate(R.layout.row_upnp_device, parent, false));
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		UPnPDevice item = getItem(position);
		if (holder.friendlyName != null) {
			String friendlyName = item.getScrubbedFriendlyName();
			if (TextUtils.isEmpty(friendlyName)) {
				friendlyName = "[unnamed]";
			}
			holder.friendlyName.setText(friendlyName);
		}
		if (holder.location != null) {
			String loc = item.getLocation().toExternalForm()
				// Uncomment to obscure actual ip addresses for screenshots
				// .replaceAll("[0-9]+\\.[0-9]+\\.[0-9]+", "192.258.1")
				;
			linkify(holder.location, null, loc);
		}
		if (holder.icon != null) {
			if (!TextUtils.isEmpty(item.getIconUrl())) {
				int iconSize = (int) holder.icon.getContext().getResources().getDimension(R.dimen.icon_size);
				picasso.load(item.getIconUrl())
					.error(R.drawable.ic_server_network)
					.resize(iconSize, iconSize)
					.centerInside()
					.into(holder.icon);
			}
			else {
				holder.icon.setImageResource(R.drawable.ic_server_network);
			}
		}
	}

	private void linkify(TextView view, CharSequence str, String url) {
		if (TextUtils.isEmpty(str) && TextUtils.isEmpty(url)) {
			view.setVisibility(View.GONE);
			return;
		}

		view.setVisibility(View.VISIBLE);
		if (TextUtils.isEmpty(url)) {
			view.setText(str);
			return;
		}

		if (TextUtils.isEmpty(str)) {
			str = url;
		}

		SpannableBuilder builder = new SpannableBuilder(view.getContext());
		builder.append(str, new URLSpan(url));

		view.setText(builder.build());
		view.setMovementMethod(LinkMovementMethod.getInstance());
	}

	class ViewHolder extends RecyclerView.ViewHolder {
		@InjectView(R.id.icon)
		@Optional
		ImageView icon;

		@InjectView(R.id.friendly_name)
		@Optional
		TextView friendlyName;

		@InjectView(R.id.location)
		@Optional
		TextView location;

		public ViewHolder(View view) {
			super(view);
			ButterKnife.inject(this, view);
		}

		@OnClick(R.id.root)
		public void click(View view) {
			int position = getAdapterPosition();
			if (mListener != null) {
				mListener.onClick(mItems.get(position), position);
				notifyItemChanged(position);
			}
		}
	}
}
