/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.lobby;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.extern.java.Log;

/**
 * Wrapper for the serverless API.
 *
 * @author Harry Stoltz
 */
@Log
public class LobbyAPI {

    private static final String API_URL = "https://dragonskulle.vercel.app/api/hosts";
    private static URL url;

    static {
        try {
            url = new URL(API_URL);
        } catch (MalformedURLException e) {
            log.warning("Invalid URL for API. Won't be able to join or host public lobbies");
            url = null;
        }
    }

    public interface IAsyncCallback {
        void call(String response);
    }

    /**
     * Class that runs a request to the API in a thread before calling the AsyncCallback supplied.
     *
     * @author Harry Stoltz
     *     <p>If the request fails for any reason, the callback is never called.
     */
    private static class AsyncRequest extends Thread {
        private final URL mUrl;
        private final String mMethod;
        private final IAsyncCallback mCallback;
        private String mContentType;
        private String mContent;

        public AsyncRequest(URL url, String method, IAsyncCallback callback) {
            mUrl = url;
            mMethod = method;
            mCallback = callback;
        }

        public AsyncRequest(
                URL url,
                String method,
                IAsyncCallback callback,
                String contentType,
                String content) {
            this(url, method, callback);
            mContentType = contentType;
            mContent = content;
        }

        @Override
        public void run() {
            try {
                HttpURLConnection con = (HttpURLConnection) mUrl.openConnection();
                con.setRequestMethod(mMethod);
                con.setConnectTimeout(5000);

                if (mMethod.equals("POST")) {
                    con.setDoOutput(true);
                    con.setRequestProperty("content-type", mContentType);
                    OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
                    writer.write(mContent);
                    writer.flush();
                    writer.close();
                }

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();

                if (mCallback != null) {
                    mCallback.call(builder.toString());
                }
            } catch (IOException e) {
                log.warning(String.format("%s request to %s failed", mMethod, mUrl.toString()));
            }
        }
    }

    /**
     * Attempt to get all of the currently up hosts via the API.
     *
     * @param callback Method to call if the request is successful.
     */
    public static void getAllHosts(IAsyncCallback callback) {
        if (url == null) {
            return;
        }
        AsyncRequest request = new AsyncRequest(url, "GET", callback);
        request.start();
    }

    public static void addNewHost(String ip, int port, IAsyncCallback callback) {
        if (url == null) {
            return;
        }
        final String contentType = "application/json";
        // {"address":"255.255.255.255","port":1337}
        final String content = String.format("{\"address\":\"%s\",\"port\":%d}", ip, port);

        AsyncRequest request = new AsyncRequest(url, "POST", callback, contentType, content);
        request.start();
    }
}
