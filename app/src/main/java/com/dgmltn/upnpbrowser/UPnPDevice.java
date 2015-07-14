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

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import android.text.TextUtils;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class UPnPDevice {

	private String mRawUPnP;
	private String mRawXml;
	private URL mLocation;
	private String mServer;

	private HashMap<String, String> mProperties;
	private String mCachedIconUrl;

	private UPnPDevice() {
	}

	public String getHost() {
		return mLocation.getHost();
	}

	public InetAddress getInetAddress() throws UnknownHostException {
		return InetAddress.getByName(getHost());
	}

	public URL getLocation() {
		return mLocation;
	}

	public String getRawUPnP() {
		return mRawUPnP;
	}

	public String getRawXml() {
		return mRawXml;
	}

	public String getServer() {
		return mServer;
	}

	public String getIconUrl() {
		return mCachedIconUrl;
	}

	public String generateIconUrl() {
		String path = mProperties.get("xml_icon_url");
		if (TextUtils.isEmpty(path)) {
			return null;
		}
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		mCachedIconUrl = mLocation.getProtocol() + "://" + mLocation.getHost() + ":" + mLocation.getPort() + "/" + path;
		return mCachedIconUrl;
	}

	public String getFriendlyName() {
		String friendlyName = mProperties.get("xml_friendly_name");
		return friendlyName;
	}

	public String getScrubbedFriendlyName() {
		String friendlyName = mProperties.get("xml_friendly_name");

		// Special case for SONOS: remove the leading ip address from the friendly name
		// "192.168.1.123 - Sonos PLAY:1" => "Sonos PLAY:1"
		if (friendlyName != null && friendlyName.startsWith(getHost() + " - ")) {
			friendlyName = friendlyName.substring(getHost().length() + 3);
		}

		return friendlyName;
	}

	////////////////////////////////////////////////////////////////////////////////
	// UPnP Response Parsing
	////////////////////////////////////////////////////////////////////////////////

	public static UPnPDevice getInstance(String raw) {
		HashMap<String, String> parsed = parseRaw(raw);
		try {
			UPnPDevice device = new UPnPDevice();
			device.mRawUPnP = raw;
			device.mProperties = parsed;
			device.mLocation = new URL(parsed.get("upnp_location"));
			device.mServer = parsed.get("upnp_server");
			return device;
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static HashMap<String, String> parseRaw(String raw) {
		HashMap<String, String> results = new HashMap<>();
		for (String line : raw.split("\r\n")) {
			int colon = line.indexOf(":");
			if (colon != -1) {
				String key = line.substring(0, colon).trim().toLowerCase();
				String value = line.substring(colon + 1).trim();
				results.put("upnp_" + key, value);
			}
		}
		return results;
	}

	////////////////////////////////////////////////////////////////////////////////
	// UPnP Specification Downloading / Parsing
	////////////////////////////////////////////////////////////////////////////////

	private transient final OkHttpClient mClient = new OkHttpClient();

	public void downloadSpecs() throws Exception {
		Request request = new Request.Builder()
			.url(mLocation)
			.build();

		Response response = mClient.newCall(request).execute();
		if (!response.isSuccessful()) {
			throw new IOException("Unexpected code " + response);
		}

		mRawXml = response.body().string();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource source = new InputSource(new StringReader(mRawXml));
		Document doc;
		try {
			doc = db.parse(source);
		}
		catch (SAXParseException e) {
			return;
		}
		XPath xPath = XPathFactory.newInstance().newXPath();

		mProperties.put("xml_icon_url", xPath.compile("//icon/url").evaluate(doc));
		generateIconUrl();
		mProperties.put("xml_friendly_name", xPath.compile("//friendlyName").evaluate(doc));
	}
}
