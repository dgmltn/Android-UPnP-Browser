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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

import android.util.Log;

import rx.Observable;
import rx.Subscriber;

/**
 * Based on:
 * https://github.com/heb-dtc/SSDPDiscovery/blob/master/src/main/java/com/flo/upnpdevicedetector/UPnPDeviceFinder.java
 */
public class UPnPDeviceFinder {

	private static String TAG = UPnPDeviceFinder.class.getName();

	public static final String MULTICAST_ADDRESS = "239.255.255.250";

	public static final int PORT = 1900;

	public static final int MAX_REPLY_TIME = 60;
	public static final int MSG_TIMEOUT = MAX_REPLY_TIME * 1000 + 1000;

	private InetAddress mInetDeviceAdr;

	private UPnPSocket mSock;

	public UPnPDeviceFinder() {
		this(true);
	}

	public UPnPDeviceFinder(boolean IPV4) {
		mInetDeviceAdr = getDeviceLocalIP(IPV4);
		Log.e(TAG, "IP is: " + mInetDeviceAdr);

		try {
			mSock = new UPnPSocket(mInetDeviceAdr);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Observable<UPnPDevice> observe() {
		return Observable.create(new Observable.OnSubscribe<UPnPDevice>() {
			@Override
			public void call(Subscriber<? super UPnPDevice> subscriber) {
				if (mSock == null) {
					subscriber.onError(new Exception("socket is null"));
					return;
				}

				try {
					// Broadcast SSDP search messages
					mSock.sendMulticastMsg();

					// Listen to responses from network until the socket timeout
					while (true) {
						Log.e(TAG, "wait for dev. response");
						DatagramPacket dp = mSock.receiveMulticastMsg();
						String receivedString = new String(dp.getData());
						receivedString = receivedString.substring(0, dp.getLength());
						Log.e(TAG, "found dev: " + receivedString);
						UPnPDevice device = UPnPDevice.getInstance(receivedString);
						if (device != null) {
							subscriber.onNext(device);
						}
					}
				}
				catch (IOException e) {
					//sock timeout will get us out of the loop
					Log.e(TAG, "time out");
					mSock.close();
					subscriber.onCompleted();
				}
			}
		});

	}

	////////////////////////////////////////////////////////////////////////////////
	// UPnPSocket
	////////////////////////////////////////////////////////////////////////////////

	private static class UPnPSocket {
		private static String TAG = UPnPSocket.class.getName();

		private SocketAddress mMulticastGroup;
		private MulticastSocket mMultiSocket;

		UPnPSocket(InetAddress deviceIp) throws IOException {
			Log.e(TAG, "UPnPSocket");

			mMulticastGroup = new InetSocketAddress(MULTICAST_ADDRESS, PORT);
			mMultiSocket = new MulticastSocket(new InetSocketAddress(deviceIp, 0));

			mMultiSocket.setSoTimeout(MSG_TIMEOUT);
		}

		public void sendMulticastMsg() throws IOException {
			String ssdpMsg = buildSSDPSearchString();

			Log.e(TAG, "sendMulticastMsg: " + ssdpMsg);

			DatagramPacket dp = new DatagramPacket(ssdpMsg.getBytes(), ssdpMsg.length(), mMulticastGroup);
			mMultiSocket.send(dp);
		}

		public DatagramPacket receiveMulticastMsg() throws IOException {
			byte[] buf = new byte[2048];
			DatagramPacket dp = new DatagramPacket(buf, buf.length);

			mMultiSocket.receive(dp);

			return dp;
		}

		/**
		 * Closing the Socket.
		 */
		public void close() {
			if (mMultiSocket != null) {
				mMultiSocket.close();
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// Utils
	////////////////////////////////////////////////////////////////////////////////

	public static final String NEWLINE = "\r\n";

	private static String buildSSDPSearchString() {
		StringBuilder content = new StringBuilder();

		content.append("M-SEARCH * HTTP/1.1").append(NEWLINE);
		content.append("Host: " + MULTICAST_ADDRESS + ":" + PORT).append(NEWLINE);
		content.append("Man:\"ssdp:discover\"").append(NEWLINE);
		content.append("MX: " + MAX_REPLY_TIME).append(NEWLINE);
		content.append("ST: upnp:rootdevice").append(NEWLINE);
		content.append(NEWLINE);

		Log.e(TAG, content.toString());

		return content.toString();
	}

	private static InetAddress getDeviceLocalIP(boolean useIPv4) {
		Log.e(TAG, "getDeviceLocalIP");

		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						Log.e(TAG, "IP from inet is: " + addr);
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4) {
							if (isIPv4) {
								Log.e(TAG, "IP v4");
								return addr;
							}
						}
						else {
							if (!isIPv4) {
								Log.e(TAG, "IP v6");
								//int delim = sAddr.indexOf('%'); // drop ip6 port suffix
								//return delim<0 ? sAddr : sAddr.substring(0, delim);
								return addr;
							}
						}
					}
				}
			}
		}
		catch (Exception ex) {
		} // for now eat exceptions
		return null;
	}

}