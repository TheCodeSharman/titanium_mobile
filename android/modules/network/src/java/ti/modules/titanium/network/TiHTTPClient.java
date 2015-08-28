/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.network;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiFileProxy;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.io.TiResourceFile;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiMimeTypeHelper;
import org.appcelerator.titanium.util.TiPlatformHelper;
import org.appcelerator.titanium.util.TiUrl;
import org.json.JSONObject;

import ti.modules.titanium.network.httpurlconnection.ContentBody;
import ti.modules.titanium.network.httpurlconnection.Entity;
import ti.modules.titanium.network.httpurlconnection.FileEntity;
import ti.modules.titanium.network.httpurlconnection.HttpUrlConnectionUtils;
import ti.modules.titanium.network.httpurlconnection.JsonBody;
import ti.modules.titanium.network.httpurlconnection.NameValuePair;
import ti.modules.titanium.network.httpurlconnection.NullHostNameVerifier;
import ti.modules.titanium.network.httpurlconnection.StringBody;
import ti.modules.titanium.network.httpurlconnection.StringEntity;
import ti.modules.titanium.network.httpurlconnection.FileBody;
import ti.modules.titanium.network.httpurlconnection.UrlEncodedFormEntity;
import ti.modules.titanium.xml.DocumentProxy;
import ti.modules.titanium.xml.XMLModule;
import android.net.Uri;
import android.os.Build;
import android.util.Base64OutputStream;

public class TiHTTPClient
{
	private static final String TAG = "TiHTTPClient";
	private static final int DEFAULT_MAX_BUFFER_SIZE = 512 * 1024;
	private static final String PROPERTY_MAX_BUFFER_SIZE = "ti.android.httpclient.maxbuffersize";
	private static final int PROTOCOL_DEFAULT_PORT = -1;
	private static final String TITANIUM_ID_HEADER = "X-Titanium-Id";
	private static final String TITANIUM_USER_AGENT = "Appcelerator Titanium/" + TiApplication.getInstance().getTiBuildVersion()
	                                                  + " ("+ Build.MODEL + "; Android API Level: "
	                                                  + Integer.toString(Build.VERSION.SDK_INT) + "; "
	                                                  + TiPlatformHelper.getInstance().getLocale() +";)";
	private static final String[] FALLBACK_CHARSETS = {"UTF_8", "ISO_8859_1"};
	
	// Regular expressions for detecting charset information in response documents (ex: html, xml).
	private static final String HTML_META_TAG_REGEX = "charset=([^\"\']*)";
	private static final String XML_DECLARATION_TAG_REGEX = "encoding=[\"\']([^\"\']*)[\"\']";

	private static AtomicInteger httpClientThreadCounter;
	private HttpURLConnection client;
	private KrollProxy proxy;
	private int readyState;
	private String responseText;
	private DocumentProxy responseXml;
	private int status;
	private String statusText;
	private boolean connected;
	private String method;
	private TiBlob responseData;
	private OutputStream responseOut;
	private String charset;
	private String contentType;
	private String contentEncoding;
	private long maxBufferSize;
	private Object data;
	private boolean needMultipart;
	private Thread clientThread;
	private boolean aborted;
	private int timeout = -1;
	private boolean autoEncodeUrl = true;
	private boolean autoRedirect = true;
	private Uri uri;
	private String url;
	private URL mURL;
	private String redirectedLocation;
	private ArrayList<File> tmpFiles = new ArrayList<File>();
	private ArrayList<X509TrustManager> trustManagers = new ArrayList<X509TrustManager>();
	private ArrayList<X509KeyManager> keyManagers = new ArrayList<X509KeyManager>();
	protected SecurityManagerProtocol securityManager;
	private int tlsVersion = NetworkModule.TLS_DEFAULT;

	private static CookieManager cookieManager = NetworkModule.getCookieManagerInstance();
	
	protected HashMap<String,String> requestHeaders = new HashMap<String,String>();
	private ArrayList<NameValuePair> nvPairs;
	private HashMap<String, ContentBody> parts;
	
	protected Map<String, List<String>> responseHeaders;
	
	public static final int READY_STATE_UNSENT = 0; // Unsent, open() has not yet been called
	public static final int READY_STATE_OPENED = 1; // Opened, send() has not yet been called
	public static final int READY_STATE_HEADERS_RECEIVED = 2; // Headers received, headers have returned and the status is available
	public static final int READY_STATE_LOADING = 3; // Loading, responseText is being loaded with data
	public static final int READY_STATE_DONE = 4; // Done, all operations have finished

	private TiFile responseFile;

	private void handleResponse(HttpURLConnection connection) throws IOException {
	    connected = true;	

	    long contentLength;

	    if (connection != null) {
	        TiHTTPClient c = this;

	        contentLength = connection.getContentLength();
	        setReadyState(READY_STATE_HEADERS_RECEIVED);

	        setStatus(connection.getResponseCode());
	        setStatusText(connection.getResponseMessage());
	        setReadyState(READY_STATE_LOADING);

	        if (proxy.hasProperty(TiC.PROPERTY_FILE)) {
	            Object f = c.proxy.getProperty(TiC.PROPERTY_FILE);
	            if (f instanceof String) {
	                String fileName = (String) f;
	                TiBaseFile baseFile = TiFileFactory.createTitaniumFile(fileName, false);
	                if (baseFile instanceof TiFile) {
	                    responseFile = (TiFile) baseFile;
	                }
	            }
	            if (responseFile == null && Log.isDebugModeEnabled()) {
	                Log.w(TAG, "Ignore the provided response file because it is not valid / writable.");
	            }
	        }

	        // Check for new url that is redirected
	        URL currentLocation = connection.getURL();
	        if (autoRedirect && !mURL.sameFile(currentLocation)) {
	            redirectedLocation = currentLocation.toString();
	        }

	        // Note on getHeaderFields()
	        // HttpURLConnection include a mapping
	        // for the null key; in HTTP's case, this maps to the HTTP status line and is
	        // treated as being at position 0 when indexing into the header fields.
	        responseHeaders = connection.getHeaderFields();

	        contentEncoding = connection.getContentEncoding();

	        contentType = connection.getContentType();

	        String[] values = contentType.split(";"); //The values.length must be equal to 2...
	        String charset = "";

	        for (String value : values) {
	            value = value.trim();
	            if (value.toLowerCase().startsWith("charset=")) {
	                charset = value.substring("charset=".length());
	            }
	        }
	        // If no charset is defined, default to UTF-8
	        if ("".equals(charset)) {
	            charset = "UTF-8";
	        }
	        responseData = null;

	        int status = connection.getResponseCode();
	        InputStream in;

	        if (status >= 400) {
	            in = connection.getErrorStream();
	        } else {
	            in = connection.getInputStream();
	        }

	        if ("gzip".equalsIgnoreCase(contentEncoding)) {
	            in = new GZIPInputStream(in);
	        }

	        InputStream is = new BufferedInputStream(in);

	        if (is != null) {
	            Log.d(TAG, "Content length: " + contentLength, Log.DEBUG_MODE);
	            int count = 0;
	            long totalSize = 0;
	            byte[] buf = new byte[4096];
	            Log.d(TAG, "Available: " + is.available(), Log.DEBUG_MODE);

	            while((count = is.read(buf)) != -1) {
	                if (aborted) {
	                    break;
	                }
	                totalSize += count;
	                try {
	                    handleEntityData(buf, count, totalSize, contentLength);
	                } catch (IOException e) {
	                    Log.e(TAG, "Error handling entity data", e);
	                }
	            }

	            if (totalSize > 0) {
	                finishedReceivingEntityData(totalSize);
	            }
	        }
	    }
	}
	
	private TiFile createFileResponseData(boolean dumpResponseOut) throws IOException
	{
		TiFile tiFile = null;
		File outFile = null;
		if (responseFile != null) {
			tiFile = responseFile;
			outFile = tiFile.getFile();
			try {
				responseOut = new FileOutputStream(outFile, dumpResponseOut);
				// If the response file is in the temp folder, don't delete it during cleanup.
				TiApplication app = TiApplication.getInstance();
				if (app != null) {
					app.getTempFileHelper().excludeFileOnCleanup(outFile);
				}
			} catch (FileNotFoundException e) {
				responseFile = null;
				tiFile = null;
				if (Log.isDebugModeEnabled()) {
					Log.e(TAG, "Unable to create / write to the response file. Will write the response data to the internal data directory.");
				}
			}
		}

		if (tiFile == null) {
			outFile = TiFileFactory.createDataFile("tihttp", "tmp");
			tiFile = new TiFile(outFile, outFile.getAbsolutePath(), false);
		}

		if (dumpResponseOut) {
			ByteArrayOutputStream byteStream = (ByteArrayOutputStream) responseOut;
			tiFile.write(TiBlob.blobFromData(byteStream.toByteArray()), false);
		}
		responseOut = new FileOutputStream(outFile, dumpResponseOut);
		responseData = TiBlob.blobFromFile(tiFile, contentType);
		return tiFile;
	}
	
	
	private void handleEntityData(byte[] data, int size, long totalSize, long contentLength) throws IOException
	{
		if (responseOut == null) {
			if (responseFile != null) {
				createFileResponseData(false);
			}
			else if (contentLength > maxBufferSize) {
				createFileResponseData(false);
			} else {
				long streamSize = contentLength > 0 ? contentLength : 512;
				responseOut = new ByteArrayOutputStream((int)streamSize);
			}
		}
		if (totalSize > maxBufferSize && responseOut instanceof ByteArrayOutputStream) {
			// Content length may not have been reported, dump the current stream
			// to a file and re-open as a FileOutputStream w/ append
			createFileResponseData(true);
		}
	
		responseOut.write(data, 0, size);
	
		KrollDict callbackData = new KrollDict();
		callbackData.put("totalCount", contentLength);
		callbackData.put("totalSize", totalSize);
		callbackData.put("size", size);
	
		byte[] blobData = new byte[size];
		System.arraycopy(data, 0, blobData, 0, size);
	
		TiBlob blob = TiBlob.blobFromData(blobData, contentType);
		callbackData.put("blob", blob);
		double progress = ((double)totalSize)/((double)contentLength);
		// return progress as -1 if it is outside the valid range
		if (progress > 1 || progress < 0) {
			progress = NetworkModule.PROGRESS_UNKNOWN;
		}
		callbackData.put("progress", progress);
	
		dispatchCallback(TiC.PROPERTY_ONDATASTREAM, callbackData);
	}
	
	private void finishedReceivingEntityData(long contentLength) throws IOException
	{
		if (responseOut instanceof ByteArrayOutputStream) {
			ByteArrayOutputStream byteStream = (ByteArrayOutputStream) responseOut;
			responseData = TiBlob.blobFromData(byteStream.toByteArray(), contentType);
		}
		responseOut.close();
		responseOut = null;
	}
	
	private interface ProgressListener
	{
		public void progress(int progress);
	}
	
	private class ProgressOutputStream extends FilterOutputStream
	{
		private ProgressListener listener;
		private int transferred = 0, lastTransferred = 0;

		public ProgressOutputStream(OutputStream delegate, ProgressListener listener)
		{
			super(delegate);
			this.listener = listener;
		}

		private void fireProgress()
		{
			// filter to 512 bytes of granularity
			if (transferred - lastTransferred >= 512) {
				lastTransferred = transferred;
				listener.progress(transferred);
			}
		}

		@Override
		public void write(int b) throws IOException
		{
			//Donot write if request is aborted
			if (!aborted) {
				super.write(b);
				transferred++;
				fireProgress();
			}
		}
	}
	
	public TiHTTPClient(KrollProxy proxy)
	{
		this.proxy = proxy;
		if (httpClientThreadCounter == null) {
			httpClientThreadCounter = new AtomicInteger();
		}
		readyState = 0;
		responseText = "";
		connected = false;
		this.nvPairs = new ArrayList<NameValuePair>();
		this.parts = new HashMap<String,ContentBody>();
		this.maxBufferSize = TiApplication.getInstance()
				.getAppProperties().getInt(PROPERTY_MAX_BUFFER_SIZE, DEFAULT_MAX_BUFFER_SIZE);
	}
	
	public int getReadyState()
	{
		synchronized(this) {
			this.notify();
		}
		return readyState;
	}
	
	public boolean validatesSecureCertificate()
	{
		if (proxy.hasProperty("validatesSecureCertificate")) {
			return TiConvert.toBoolean(proxy.getProperty("validatesSecureCertificate"));

		} else {
			if (TiApplication.getInstance().getDeployType().equals(
					TiApplication.DEPLOY_TYPE_PRODUCTION)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	public void addAuthFactory(String scheme, AuthSchemeFactory theFactory)
	{
		customAuthenticators.put(scheme, theFactory);
	}
	*/
	
	public void setReadyState(int readyState)
	{
		Log.d(TAG, "Setting ready state to " + readyState, Log.DEBUG_MODE);
		this.readyState = readyState;
		KrollDict data = new KrollDict();
		data.put("readyState", Integer.valueOf(readyState));
		dispatchCallback(TiC.PROPERTY_ONREADYSTATECHANGE, data);

		if (readyState == READY_STATE_DONE) {
			KrollDict data1 = new KrollDict();
			data1.putCodeAndMessage(TiC.ERROR_CODE_NO_ERROR, null);
			dispatchCallback(TiC.PROPERTY_ONLOAD, data1);
		}
	}
	
	private String decodeResponseData(String charsetName) {
		Charset charset;
		try {
			charset = Charset.forName(charsetName);

		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Could not find charset: " + e.getMessage());
			return null;
		}

		CharsetDecoder decoder = charset.newDecoder();
		ByteBuffer in = ByteBuffer.wrap(responseData.getBytes());

		try {
			CharBuffer decodedText = decoder.decode(in);
			return decodedText.toString();

		} catch (CharacterCodingException e) {
			return null;

		} catch (OutOfMemoryError e) {
			Log.e(TAG, "Not enough memory to decode response data.");
			return null;
		}
	}
	
	/**
	 * Attempts to scan the response data to determine the encoding of the text.
	 * Looks for meta information usually found in HTML or XML documents.
	 *
	 * @return The name of the encoding if detected, otherwise null if no encoding could be determined.
	 */
	private String detectResponseDataEncoding() {
		String regex;
		if (contentType == null) {
			Log.w(TAG, "Could not detect charset, no content type specified.", Log.DEBUG_MODE);
			return null;

		} else if (contentType.contains("xml")) {
			regex = XML_DECLARATION_TAG_REGEX;

		} else if (contentType.contains("html")) {
			regex = HTML_META_TAG_REGEX;

		} else {
			Log.w(TAG, "Cannot detect charset, unknown content type: " + contentType, Log.DEBUG_MODE);
			return null;
		}

		CharSequence responseSequence = responseData.toString();
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(responseSequence);
		if (matcher.find()) {
			return matcher.group(1);
		}

		return null;
	}
	
	public String getResponseText()
	{
		if (responseText != null || responseData == null) {
			return responseText;
		}

		// First try decoding the response data using the charset
		// specified in the response content-type header.
		if (charset != null) {
			responseText = decodeResponseData(charset);
			if (responseText != null) {
				return responseText;
			}
		}

		// If the first attempt to decode fails try detecting the correct
		// charset by scanning the response data.
		String detectedCharset = detectResponseDataEncoding();
		if (detectedCharset != null) {
			Log.d(TAG, "detected charset: " + detectedCharset, Log.DEBUG_MODE);
			responseText = decodeResponseData(detectedCharset);
			if (responseText != null) {
				charset = detectedCharset;
				return responseText;
			}
		}

		// As a last resort try our fallback charsets to decode the data.
		for (String charset : FALLBACK_CHARSETS) {
			responseText = decodeResponseData(charset);
			if (responseText != null) {
				return responseText;
			}
		}

		Log.e(TAG, "Could not decode response text.");
		return responseText;
	}
	
	public TiBlob getResponseData()
	{
		return responseData;
	}
	
	public DocumentProxy getResponseXML()
	{
		// avoid eating up tons of memory if we have a large binary data blob
		if (TiMimeTypeHelper.isBinaryMimeType(contentType))
		{
			return null;
		}

		if (responseXml == null && (responseData != null || responseText != null)) {
			try {
				String text = getResponseText();
				if (text == null || text.length() == 0) {
					return null;
				}

				if (charset != null && charset.length() > 0) {
					responseXml = XMLModule.parse(text, charset);

				} else {
					responseXml = XMLModule.parse(text);
				}

			} catch (Exception e) {
				Log.e(TAG, "Error parsing XML", e);
			}
		}

		return responseXml;
	}
	
	public void setResponseText(String responseText)
	{
		this.responseText = responseText;
	}
	
	public int getStatus()
	{
		return status;
	}

	public  void setStatus(int status)
	{
		this.status = status;
	}

	public  String getStatusText()
	{
		return statusText;
	}

	public  void setStatusText(String statusText)
	{
		this.statusText = statusText;
	}

	public void abort()
	{
		if (readyState > READY_STATE_UNSENT && readyState < READY_STATE_DONE) {
			aborted = true;
			if (client != null) {
				client.disconnect();
				client = null;
			}
			// Fire the disposehandle event if the request is aborted.
			// And it will dispose the handle of the httpclient in the JS.
			proxy.fireEvent(TiC.EVENT_DISPOSE_HANDLE, null);
		}
	}
	
	public String getAllResponseHeaders()
	{
		String result = "";
		if(!responseHeaders.isEmpty()){
			StringBuilder sb = new StringBuilder(256);
			Set<Map.Entry<String, List<String>>> entrySet = responseHeaders.entrySet();
			
	        for (Map.Entry<String, List<String>> entry : entrySet) {
	            String headerName = entry.getKey();
	            sb.append(headerName).append(":");
	            List<String> headerValues = entry.getValue();
	            for (String value : headerValues) {
	                sb.append(value).append("\n");
	            }
	        }
	        result = sb.toString();
		}
		return result;
	}
	
	public void clearCookies(String url)
	{
		List<HttpCookie> cookies = new ArrayList<HttpCookie>(cookieManager.getCookieStore().getCookies());
		cookieManager.getCookieStore().removeAll();
		String lower_url = url.toLowerCase();

		for (HttpCookie cookie : cookies) {
			String cookieDomain = cookie.getDomain();
			if (!lower_url.contains(cookieDomain.toLowerCase())) {
				URI uriDomain;
				try {
					uriDomain = new URI(cookieDomain);
				} catch (URISyntaxException e) {
					uriDomain = null;
				}
				cookieManager.getCookieStore().add(uriDomain, cookie);
			}
		}
	}

	public void setRequestHeader(String header, String value)
	{
		if (readyState <= READY_STATE_OPENED) {
			if (value == null) {
				// If value is null, remove header
				requestHeaders.remove(header);
			} else {		
				if (requestHeaders.containsKey(header)){
					// Appends a value to a header
					// If it is a cookie, use ';'. If not, use ','.
					String seperator = ("Cookie".equalsIgnoreCase(header))? "; " : ", ";
					StringBuffer val = new StringBuffer(requestHeaders.get(header));
					val.append(seperator+value);
					requestHeaders.put(header, val.toString());
				} else {
					// Set header for the first time
					requestHeaders.put(header, value);
				}
			}
			
		} else {
			throw new IllegalStateException("setRequestHeader can only be called before invoking send.");
		}
	}
	

	public String getResponseHeader(String getHeaderName)
	{
		String result = "";
		if (!responseHeaders.isEmpty()) {
			boolean firstPass = true;
			StringBuilder sb = new StringBuilder(256);
			Set<Map.Entry<String, List<String>>> entrySet = responseHeaders.entrySet();
	        for (Map.Entry<String, List<String>> entry : entrySet) {	            
	        	String headerName = entry.getKey();	            	            
	        	if (headerName != null && headerName.equalsIgnoreCase(getHeaderName)) {
	            	List<String> headerValues = entry.getValue();		            
	            	for (String value : headerValues) {
		            	if (!firstPass) {
							sb.append(", ");
						}
		            	sb.append(value);
		            	firstPass = false;
		            }
	            }
	        }
	        result = sb.toString();
		}
		
		if (result.length() == 0) {
			Log.w(TAG, "No value for response header: " + getHeaderName, Log.DEBUG_MODE);
		}
		
		return result;
	}
	
	
	public void open(String method, String url)
	{
		Log.d(TAG, "open request method=" + method + " url=" + url, Log.DEBUG_MODE);

		if (url == null)
		{
			Log.e(TAG, "Unable to open a null URL");
			throw new IllegalArgumentException("URL cannot be null");
		}

		// if the url is not prepended with either http or
		// https, then default to http and prepend the protocol
		// to the url
		String lowerCaseUrl = url.toLowerCase();
		if (!lowerCaseUrl.startsWith("http://") && !lowerCaseUrl.startsWith("https://")) {
			url = "http://" + url;
		}

		if (autoEncodeUrl) {
			this.uri = TiUrl.getCleanUri(url);

		} else {
			this.uri = Uri.parse(url);
		}

		// If the original url does not contain any
		// escaped query string (i.e., does not look
		// pre-encoded), go ahead and reset it to the
		// clean uri. Else keep it as is so the user's
		// escaping stays in effect.  The users are on their own
		// at that point.
		if (autoEncodeUrl && !url.matches(".*\\?.*\\%\\d\\d.*$")) {
			this.url = this.uri.toString();

		} else {
			this.url = url;
		}

		redirectedLocation = null;
		this.method = method;
		String hostString = uri.getHost();
		int port = PROTOCOL_DEFAULT_PORT;

		// The Android Uri doesn't seem to handle user ids with at-signs (@) in them
		// properly, even if the @ is escaped.  It will set the host (uri.getHost()) to
		// the part of the user name after the @.  For example, this Uri would get
		// the host set to appcelerator.com when it should be mickey.com:
		// http://testuser@appcelerator.com:password@mickey.com/xx
		// ... even if that first one is escaped to ...
		// http://testuser%40appcelerator.com:password@mickey.com/xx
		// Tests show that Java URL handles it properly, however.  So revert to using Java URL.getHost()
		// if we see that the Uri.getUserInfo has an at-sign in it.
		// Also, uri.getPort() will throw an exception as it will try to parse what it thinks is the port
		// part of the Uri (":password....") as an int.  So in this case we'll get the port number
		// as well from Java URL.  See Lighthouse ticket 2150.
		if (uri.getUserInfo() != null && uri.getUserInfo().contains("@")) {
			URL javaUrl;
			try {
				javaUrl = new URL(uri.toString());
				hostString = javaUrl.getHost();
				port = javaUrl.getPort();

			} catch (MalformedURLException e) {
				Log.e(TAG, "Error attempting to derive Java url from uri: " + e.getMessage(), e);
			}

		} else {
			port = uri.getPort();
		}

		Log.d(
			TAG,
			"Instantiating host with hostString='" + hostString + "', port='" + port + "', scheme='" + uri.getScheme() + "'",
			Log.DEBUG_MODE);

		final String username = ((HTTPClientProxy)proxy).getUsername();
		final String password = ((HTTPClientProxy)proxy).getPassword();
		final String domain = ((HTTPClientProxy)proxy).getDomain();	

		Authenticator.setDefault(new TiAuthenticator(domain, username, password));

		setReadyState(READY_STATE_OPENED);
		setRequestHeader("User-Agent", TITANIUM_USER_AGENT);
		// Causes Auth to Fail with twitter and other size apparently block X- as well
		// Ticket #729, ignore twitter for now
		if (!hostString.contains("twitter.com")) {
			setRequestHeader("X-Requested-With","XMLHttpRequest");

		} else {
			Log.i(TAG, "Twitter: not sending X-Requested-With header", Log.DEBUG_MODE);
		}
	}

	public void setRawData(Object data)
	{
		this.data = data;
	}


	public void addPostData(String name, String value) throws UnsupportedEncodingException
	{
		if (value == null) {
			value = "";
		}
		if (needMultipart) {
			// JGH NOTE: this seems to be a bug in RoR where it would puke if you
			// send a content-type of text/plain for key/value pairs in form-data
			// so we send an empty string by default instead which will cause the
			// StringBody to not include the content-type header. this should be
			// harmless for all other cases
			parts.put(name, new StringBody(value,"",null));
		} else {
			nvPairs.add(new NameValuePair(name, value.toString()));
		}
	}
	
	private void dispatchCallback(String name, KrollDict data) {
		if (data == null) {
			data = new KrollDict();
		}

		data.put("source", proxy);

		proxy.callPropertyAsync(name, new Object[] { data });
	}
	
	private int addTitaniumFileAsPostData(String name, Object value)
	{
		try {
			// TiResourceFile cannot use the FileBody approach directly, because it requires
			// a java File object, which you can't get from packaged resources. So
			// TiResourceFile uses the approach we use for blobs, which is write out the
			// contents to a temp file, then use that for the FileBody.
			if (value instanceof TiBaseFile && !(value instanceof TiResourceFile)) {
				TiBaseFile baseFile = (TiBaseFile) value;
				FileBody body = new FileBody(baseFile.getNativeFile(), TiMimeTypeHelper.getMimeType(baseFile.nativePath()));
				parts.put(name, body);
				return (int)baseFile.getNativeFile().length();

			} else if (value instanceof TiBlob || value instanceof TiResourceFile) {
				TiBlob blob;
				if (value instanceof TiBlob) {
					blob = (TiBlob) value;
				} else {
					blob = ((TiResourceFile) value).read();
				}
				String mimeType = blob.getMimeType();
				File tmpFile = File.createTempFile("tixhr", "." + TiMimeTypeHelper.getFileExtensionFromMimeType(mimeType, "txt"));
				FileOutputStream fos = new FileOutputStream(tmpFile);
				if (blob.getType() == TiBlob.TYPE_STREAM_BASE64) {
					TiBaseFile.copyStream(blob.getInputStream(), new Base64OutputStream(fos, android.util.Base64.DEFAULT));
				} else {
					fos.write(blob.getBytes());
				}
				fos.close();

				tmpFiles.add(tmpFile);

				FileBody body = new FileBody(tmpFile, mimeType);
				parts.put(name, body);
				return (int)tmpFile.length();
			} else if (value instanceof HashMap) {
				// If value is a HashMap, it is actually a JSON
				JSONObject jsonObject = TiConvert.toJSON( (HashMap<String, Object>) value);
				JsonBody jsonBody = new JsonBody(jsonObject, null);
				parts.put(name, jsonBody);
				return (int) jsonBody.getContentLength();
			} else {
				if (value != null) {
					Log.e(TAG, name + " is a " + value.getClass().getSimpleName());

				} else {
					Log.e(TAG, name + " is null");
				}
			}

		} catch (IOException e) {
			Log.e(TAG, "Error adding post data ("+name+"): " + e.getMessage());
		}
		return 0;
	}
	
	private void setUpSSL(boolean validating, HttpsURLConnection securedConnection)
	{
		SSLSocketFactory sslSocketFactory = null;

		if (this.securityManager != null) {
			if (this.securityManager.willHandleURL(this.uri)) {
				TrustManager[] trustManagerArray = this.securityManager.getTrustManagers((HTTPClientProxy)this.proxy);
				KeyManager[] keyManagerArray = this.securityManager.getKeyManagers((HTTPClientProxy)this.proxy);

				try {
					sslSocketFactory = new TiSocketFactory(keyManagerArray, trustManagerArray, tlsVersion);
				} catch(Exception e) {
					Log.e(TAG, "Error creating SSLSocketFactory: " + e.getMessage());
					sslSocketFactory = null;
				}
			}
		}
		if (sslSocketFactory == null) {
			if (trustManagers.size() > 0 || keyManagers.size() > 0) {
				TrustManager[] trustManagerArray = null;
				KeyManager[] keyManagerArray = null;

				if (trustManagers.size() > 0) {
					trustManagerArray = new X509TrustManager[trustManagers.size()];
					trustManagerArray = trustManagers.toArray(trustManagerArray);
				}

				if (keyManagers.size() > 0) {
					keyManagerArray = new X509KeyManager[keyManagers.size()];
					keyManagerArray = keyManagers.toArray(keyManagerArray);
				}

				try {
					sslSocketFactory = new TiSocketFactory(keyManagerArray, trustManagerArray, tlsVersion);
				} catch(Exception e) {
					Log.e(TAG, "Error creating SSLSocketFactory: " + e.getMessage());
					sslSocketFactory = null;
				}
			} else if (!validating) {
				TrustManager trustManagerArray[] = new TrustManager[] { new NonValidatingTrustManager() };
				try {
					sslSocketFactory = new TiSocketFactory(null, trustManagerArray, tlsVersion);
				} catch(Exception e) {
					Log.e(TAG, "Error creating SSLSocketFactory: " + e.getMessage());
					sslSocketFactory = null;
				}
			}
		}
		
		if (sslSocketFactory != null) {
			securedConnection.setSSLSocketFactory(sslSocketFactory);
		} else if (!validating) {
			securedConnection.setSSLSocketFactory(new NonValidatingSSLSocketFactory());
		} 
		
		if (!validating) {
			securedConnection.setHostnameVerifier(new NullHostNameVerifier());
		}
		// Fortunately, HttpsURLConnection supports SNI since Android 2.3.
		// We don't have to handle SNI explicitly 
		// https://developer.android.com/training/articles/security-ssl.html
	}
	
	protected void setUpClient(HttpURLConnection connection)
	{
		connection.setInstanceFollowRedirects(autoRedirect);		
	}

	private Object titaniumFileAsPutData(Object value)
	{
		if (value instanceof TiBaseFile && !(value instanceof TiResourceFile)) {
			TiBaseFile baseFile = (TiBaseFile) value;
			return new FileEntity(baseFile.getNativeFile(), TiMimeTypeHelper.getMimeType(baseFile.nativePath()));
		} else if (value instanceof TiBlob || value instanceof TiResourceFile) {
			try {
				TiBlob blob;
				if (value instanceof TiBlob) {
					blob = (TiBlob) value;
				} else {
					blob = ((TiResourceFile) value).read();
				}
				String mimeType = blob.getMimeType();
				File tmpFile = File.createTempFile("tixhr", "." + TiMimeTypeHelper.getFileExtensionFromMimeType(mimeType, "txt"));
				FileOutputStream fos = new FileOutputStream(tmpFile);
				fos.write(blob.getBytes());
				fos.close();

				tmpFiles.add(tmpFile);
				return new FileEntity(tmpFile, mimeType);
			} catch (IOException e) {
				Log.e(TAG, "Error adding put data: " + e.getMessage());
			}
		}
		return value;
	}
	
	public void send(Object userData) throws UnsupportedEncodingException 
	{
		aborted = false;
		
		// TODO consider using task manager
		int totalLength = 0;
		needMultipart = false;

		if (userData != null)
		{
			if (userData instanceof HashMap) {
				HashMap<String, Object> data = (HashMap) userData;
				boolean isPostOrPutOrPatch = method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
				boolean isGet = !isPostOrPutOrPatch && method.equals("GET");

				// first time through check if we need multipart for POST
				for (String key : data.keySet()) {
					Object value = data.get(key);

					if(value != null) {
						// if the value is a proxy, we need to get the actual file object
						if (value instanceof TiFileProxy) {
							value = ((TiFileProxy) value).getBaseFile();
						}

						if (value instanceof TiBaseFile || value instanceof TiBlob) {
							needMultipart = true;
							break;
						}
					}
				}

				boolean queryStringAltered = false;				
				for (String key : data.keySet()) {
					Object value = data.get(key);
					if (isPostOrPutOrPatch && (value != null)) {
						// if the value is a proxy, we need to get the actual file object
						if (value instanceof TiFileProxy) {
							value = ((TiFileProxy) value).getBaseFile();
						}

						if (value instanceof TiBaseFile || value instanceof TiBlob || value instanceof HashMap) {
							totalLength += addTitaniumFileAsPostData(key, value);

						} else {
							String str = TiConvert.toString(value);
							addPostData(key, str);
							totalLength += str.length();
						}

					} else if (isGet) {
						uri = uri.buildUpon().appendQueryParameter(
							key, TiConvert.toString(value)).build();
						queryStringAltered = true;
					}
				}
				
				if (queryStringAltered) {
					this.url = uri.toString();
				}
			} else if (userData instanceof TiFileProxy || userData instanceof TiBaseFile || userData instanceof TiBlob) {
				Object value = userData;
				if (value instanceof TiFileProxy) {
					value = ((TiFileProxy) value).getBaseFile();
				}
				if (value instanceof TiBaseFile || value instanceof TiBlob) {
					setRawData(titaniumFileAsPutData(value));
				} else {
					setRawData(TiConvert.toString(value));
				}
			} else {
				setRawData(TiConvert.toString(userData));
			}
		}

		Log.d(TAG, "Instantiating http request with method='" + method + "' and this url:", Log.DEBUG_MODE);
		Log.d(TAG, this.url, Log.DEBUG_MODE);
		
		clientThread = new Thread(new ClientRunnable(totalLength), "TiHttpClient-" + httpClientThreadCounter.incrementAndGet());
		clientThread.setPriority(Thread.MIN_PRIORITY);
		clientThread.start();

		Log.d(TAG, "Leaving send()", Log.DEBUG_MODE);
	}
	

	
	private class ClientRunnable implements Runnable
	{
		private final int totalLength;
		private PrintWriter printWriter;
		private OutputStream outputStream;
		private String boundary;
		private static final String LINE_FEED = "\r\n";

		public ClientRunnable(int totalLength)
		{
			this.totalLength = totalLength;
		}

		public void run()
		{
			try {
				Thread.sleep(10);
				Log.d(TAG, "send()", Log.DEBUG_MODE);

				//If there are any custom authentication factories registered with the client add them here
				/*
				Enumeration<String> authSchemes = customAuthenticators.keys();
				while (authSchemes.hasMoreElements()) {
					String scheme = authSchemes.nextElement();
					client.getAuthSchemes().register(scheme, customAuthenticators.get(scheme));
				}
				*/
				
				Log.d(TAG, "Preparing to execute request", Log.DEBUG_MODE);

				String result = null;

				try {
					mURL = new URL(url);
					client = (HttpURLConnection) mURL.openConnection();
					setUpClient(client);
					
					if (client instanceof HttpsURLConnection) {
					    HttpsURLConnection securedConnection = (HttpsURLConnection) client;
					    setUpSSL(validatesSecureCertificate(), securedConnection);
					}					
								
					if (timeout != -1) {
						client.setReadTimeout(timeout);
						client.setConnectTimeout(timeout);
					}
					
					if (aborted) {
						return;
					}		
					
					boolean isPostOrPutOrPatch = method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
					
					client.setUseCaches(true);
					client.setRequestMethod(method);
					client.setDoInput(true);
					if (isPostOrPutOrPatch) {
						client.setDoOutput(true);
					}
					client.setUseCaches(false);
					// This is to set gzip default to disable
					// https://code.google.com/p/android/issues/detail?id=174949
					client.setRequestProperty("Accept-Encoding", "identity");
					client.setRequestProperty(TITANIUM_ID_HEADER, TiApplication.getInstance().getAppGUID());
					if (parts.size() > 0 && needMultipart) {
						boundary = HttpUrlConnectionUtils.generateBoundary();
						client.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
					} else {
						client.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
					}

					for (String header : requestHeaders.keySet()) {
						client.setRequestProperty(header, requestHeaders.get(header));
					}
					
					if (isPostOrPutOrPatch) {			
						outputStream = new ProgressOutputStream(client.getOutputStream(), new ProgressListener() {
							public void progress(int progress) {
								KrollDict data = new KrollDict();
								double currentProgress = ((double) progress/totalLength);
								if (currentProgress > 1) currentProgress = 1;
								data.put("progress", currentProgress);
								dispatchCallback(TiC.PROPERTY_ONSENDSTREAM, data);
							}
						});
						printWriter = new PrintWriter(outputStream, true);
						
						UrlEncodedFormEntity form = null;
						
						if (nvPairs.size() > 0) {
							try {
								form = new UrlEncodedFormEntity(nvPairs, "UTF-8");
	
							} catch (UnsupportedEncodingException e) {
								Log.e(TAG, "Unsupported encoding: ", e);
							}
						}
						
						if (parts.size() > 0 && needMultipart) {
							
							for(String name : parts.keySet()) {
								Log.d(TAG, "adding part " + name + ", part type: " + parts.get(name).getMimeType() + ", len: "
									+ parts.get(name).getContentLength(), Log.DEBUG_MODE);
								addFilePart(name, parts.get(name));
							}
							
							if (form != null) {
								try {
									ByteArrayOutputStream bos = new ByteArrayOutputStream((int) form.getContentLength());
									form.writeTo(bos);
									addFilePart("form", new StringBody(bos.toString(), "application/x-www-form-urlencoded", Charset.forName("UTF-8")));
	
								} catch (UnsupportedEncodingException e) {
									Log.e(TAG, "Unsupported encoding: ", e);
	
								} catch (IOException e) {
									Log.e(TAG, "Error converting form to string: ", e);
								}
							}
							completeSendingMultipart();						
						} else {
							handleURLEncodedData(form);
						}
					}
					handleResponse(client);

				}catch (IOException e) {
					if (!aborted) {
						throw e;
					}
				}  finally {
					client.disconnect();
				} 


				if(result != null) {
					Log.d(TAG, "Have result back from request len=" + result.length(), Log.DEBUG_MODE);
				}
				connected = false;
				setResponseText(result);

				
				if (getStatus() >= 400) {
					throw new IOException(getStatus() + " : " + getStatusText());
				}
				

				if (!aborted) {
					setReadyState(READY_STATE_DONE);
				}

			} catch(Throwable t) {
				if (client != null) {
					Log.d(TAG, "clearing the expired and idle connections", Log.DEBUG_MODE);
					client.disconnect();
				} else {
					Log.d(TAG, "client is not valid, unable to clear expired and idle connections");
				}

				String msg = t.getMessage();
				if (msg == null && t.getCause() != null) {
					msg = t.getCause().getMessage();
				}
				if (msg == null) {
					msg = t.getClass().getName();
				}
				Log.e(TAG, "HTTP Error (" + t.getClass().getName() + "): " + msg, t);

				KrollDict data = new KrollDict();
				data.putCodeAndMessage(TiC.ERROR_CODE_UNKNOWN, msg);
				dispatchCallback(TiC.PROPERTY_ONERROR, data);
			} finally {
				deleteTmpFiles();

				//Clean up client and clientThread
				
				client = null;
				clientThread = null;

				// Fire the disposehandle event if the request is finished successfully or the errors occur.
				// And it will dispose the handle of the httpclient in the JS.
				proxy.fireEvent(TiC.EVENT_DISPOSE_HANDLE, null);
			}

		}
		
	    private void addFilePart(String name, ContentBody contentBody) throws IOException{
	    	String fileName = contentBody.getFilename();

	    	printWriter.append("--" + boundary).append(LINE_FEED);
	    	printWriter.append("Content-Disposition: form-data; name=\"" + name);
	    	if(fileName != null){
	    		printWriter.append("\"; filename=\"" + fileName + "\"");
	    	}
	    	printWriter.append(LINE_FEED);
	    	printWriter.append("Content-Type: " + contentBody.getMimeType());
	    	if(contentBody.getCharset() != null) {
	    		printWriter.append("; charset="+contentBody.getCharset());
	    	}
	    	printWriter.append(LINE_FEED);
	    	printWriter.append("Content-Transfer-Encoding: "+ contentBody.getTransferEncoding()).append(LINE_FEED);
	    	printWriter.append(LINE_FEED);
	    	printWriter.flush();

	    	contentBody.writeTo(outputStream);

	    	printWriter.append(LINE_FEED);
	    	printWriter.flush();    

	    }
	 	    
	    public void completeSendingMultipart() throws IOException {
	        printWriter.append("--" + boundary + "--").append(LINE_FEED);
	        printWriter.close();
	 
	    }
	    
		private void handleURLEncodedData(UrlEncodedFormEntity form) throws IOException
		{
			//If set rawDate is set with a String, need to do this
			Entity entity = null;
			if (data instanceof String) {
				try {
					entity = new StringEntity((String) data, "UTF-8");

				} catch(Exception ex) {
					//FIXME
					Log.e(TAG, "Exception, implement recovery: ", ex);
				}
			} else if (data instanceof Entity) {
				entity = (Entity) data;
			} else {
				entity = form;
			}
			
			//This code sets the content type from the headers
			//Then casts the request so that it can put in the form which is the entity.
			
			if (entity != null) {
		    	entity.writeTo(outputStream);
		    	printWriter.flush();   
			}
			
		}
	}

	private void deleteTmpFiles()
	{
		if (tmpFiles.isEmpty()) {
			return;
		}

		for (File tmpFile : tmpFiles) {
			tmpFile.delete();
		}
		tmpFiles.clear();
	}
	
	public String getLocation()
	{
		if (redirectedLocation != null) {
			return redirectedLocation;
		}
		return url;
	}

	public String getConnectionType()
	{
		return method;
	}

	public boolean isConnected()
	{
		return connected;
	}

	public void setTimeout(int millis)
	{
		timeout = millis;
	}

	protected void setAutoEncodeUrl(boolean value)
	{
		autoEncodeUrl = value;
	}

	protected boolean getAutoEncodeUrl()
	{
		return autoEncodeUrl;
	}

	protected void setAutoRedirect(boolean value)
	{
		autoRedirect = value;
	}

	protected boolean getAutoRedirect()
	{
		return autoRedirect;
	}

	protected void addKeyManager(X509KeyManager manager)
	{
		if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "addKeyManager method is deprecated. Use the securityManager property on the HttpClient to define custom SSL Contexts", Log.DEBUG_MODE);
		}
		keyManagers.add(manager);
	}

	protected void addTrustManager(X509TrustManager manager)
	{
		if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "addTrustManager method is deprecated. Use the securityManager property on the HttpClient to define custom SSL Contexts", Log.DEBUG_MODE);
		}
		trustManagers.add(manager);
	}

	protected void setTlsVersion(int value)
	{
		this.proxy.setProperty(TiC.PROPERTY_TLS_VERSION, value);
		tlsVersion = value;
	}

}
