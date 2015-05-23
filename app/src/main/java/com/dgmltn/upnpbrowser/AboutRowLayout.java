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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class AboutRowLayout extends LinearLayout {

	@InjectView(R.id.avatar)
	ImageView vAvatar;

	@InjectView(R.id.project)
	TextView vProject;

	@InjectView(R.id.description)
	TextView vDescription;

	@InjectView(R.id.link1)
	TextView vLink1;

	@InjectView(R.id.link2)
	TextView vLink2;

	public AboutRowLayout(Context context) {
		super(context);
		init(context, null);
	}

	public AboutRowLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public AboutRowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	@TargetApi(21)
	public AboutRowLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		setOrientation(HORIZONTAL);

		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AboutRowLayout);
		Drawable src = a.getDrawable(R.styleable.AboutRowLayout_avatar);
		String projectStr = a.getString(R.styleable.AboutRowLayout_projectStr);
		String projectUrl = a.getString(R.styleable.AboutRowLayout_projectUrl);
		String projectStr2 = a.getString(R.styleable.AboutRowLayout_projectStr2);
		String projectUrl2 = a.getString(R.styleable.AboutRowLayout_projectUrl2);
		String projectStr3 = a.getString(R.styleable.AboutRowLayout_projectStr3);
		String projectUrl3 = a.getString(R.styleable.AboutRowLayout_projectUrl3);
		CharSequence description = a.getText(R.styleable.AboutRowLayout_description);
		String linkStr = a.getString(R.styleable.AboutRowLayout_linkStr);
		String linkUrl = a.getString(R.styleable.AboutRowLayout_linkUrl);
		String linkStr2 = a.getString(R.styleable.AboutRowLayout_linkStr2);
		String linkUrl2 = a.getString(R.styleable.AboutRowLayout_linkUrl2);
		a.recycle();

		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.row_about, this, true);

		ButterKnife.inject(this);

		vAvatar.setVisibility(src == null ? View.GONE : View.VISIBLE);
		vAvatar.setImageDrawable(src);

		SpannableBuilder project = new SpannableBuilder(getContext());
		appendProject(project, projectStr, projectUrl);
		appendProject(project, projectStr2, projectUrl2);
		appendProject(project, projectStr3, projectUrl3);
		if (project.length() == 0) {
			vProject.setVisibility(View.GONE);
		}
		else {
			vProject.setVisibility(View.VISIBLE);
			vProject.setText(project.build());
			vProject.setMovementMethod(LinkMovementMethod.getInstance());
		}

		linkify(vDescription, description, null);
		linkify(vLink1, linkStr, linkUrl);
		linkify(vLink2, linkStr2, linkUrl2);
	}

	private void appendProject(SpannableBuilder builder, CharSequence str, String url) {
		if (TextUtils.isEmpty(str) && TextUtils.isEmpty(url)) {
			return;
		}

		if (builder.length() != 0) {
			builder.append(", ");
		}

		if (TextUtils.isEmpty(url)) {
			builder.append(str, new StyleSpan(Typeface.BOLD));
			return;
		}

		if (TextUtils.isEmpty(str)) {
			str = url;
		}

		builder.append(str, new PlainURLSpan(url), new StyleSpan(Typeface.BOLD));
		builder.append(R.drawable.ic_external_link_arrow);
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

		SpannableBuilder builder = new SpannableBuilder(getContext());
		builder.append(str, new URLSpan(url));

		view.setText(builder.build());
		view.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private static class PlainURLSpan extends URLSpan {
		public PlainURLSpan(String url) {
			super(url);
		}

		@Override
		public void updateDrawState(TextPaint ds) {
			// intentionally do nothing here
		}
	}

}
