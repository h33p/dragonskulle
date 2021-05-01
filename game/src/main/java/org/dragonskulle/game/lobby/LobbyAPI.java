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
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0";
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
        void call(String response, boolean success);
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

        /**
         * Create a new AsyncRequest.
         *
         * @param url URL to make a request to
         * @param method Method of the request (GET, POST, DELETE etc)
         * @param callback Method to call after request execution
         * @throws MalformedURLException If the url passed is invalid
         */
        public AsyncRequest(String url, String method, IAsyncCallback callback)
                throws MalformedURLException {
            mUrl = new URL(url);
            mMethod = method;
            mCallback = callback;
        }

        /**
         * Create a new AsyncRequest.
         *
         * @param url URL to make a request to
         * @param method Method of the request (GET, POST, DELETE etc)
         * @param callback Method to call after request execution
         */
        public AsyncRequest(URL url, String method, IAsyncCallback callback) {
            mUrl = url;
            mMethod = method;
            mCallback = callback;
        }

        /**
         * Create a new AsyncRequest.
         *
         * @param url URL to make a request to
         * @param method Method of the request (GET, POST, DELETE etc)
         * @param callback Method to call after request execution
         * @param contentType Value for request property "content-type"
         * @param content Data to send in the request
         */
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
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);

                con.setRequestProperty("User-Agent", USER_AGENT);

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
                    boolean success = con.getResponseCode() == HttpURLConnection.HTTP_OK;
                    mCallback.call(builder.toString(), success);
                }
            } catch (IOException e) {
                log.warning(String.format("%s request to %s failed", mMethod, mUrl.toString()));
            }
        }
    }

    /**
     * Attempt to get all of the currently up hosts asynchronously via the API.
     *
     * @param callback Method to call after the request is completed.
     */
    public static void getAllHostsAsync(IAsyncCallback callback) {
        if (url == null) {
            return;
        }
        AsyncRequest request = new AsyncRequest(url, "GET", callback);
        request.start();
    }

    /**
     * Add a new host to the server list asynchronously via the API.
     *
     * @param ip IP address of the host
     * @param port Port that the server is hosted on
     * @param callback Method to call after the request is completed.
     */
    public static void addNewHostAsync(String ip, int port, IAsyncCallback callback) {
        if (url == null) {
            return;
        }
        String contentType = "application/json";
        String content = String.format("{\"address\":\"%s\",\"port\":%d}", ip, port);

        AsyncRequest request = new AsyncRequest(url, "POST", callback, contentType, content);
        request.start();
    }

    /**
     * Delete an existing host from the server list asynchronously.
     *
     * @param id ID of the entry to be deleted
     * @param callback Method to call after the request is completed.
     */
    public static void deleteHostAsync(String id, IAsyncCallback callback) {
        if (url == null) {
            return;
        }

        try {
            AsyncRequest request = new AsyncRequest(API_URL + "/code/" + id, "DELETE", callback);
            request.start();
        } catch (MalformedURLException e) {
            log.warning("Invalid url for delete request");
        }
    }

    /**
     * Get an existing host from the server list by id. This is done synchronously.
     *
     * @param id ID of the entry to find
     * @param callback Method to call after the request is completed
     */
    public static void getHostById(String id, IAsyncCallback callback) {
        if (url == null) {
            return;
        }

        try {
            AsyncRequest request = new AsyncRequest(API_URL + "/code/" + id, "GET", callback);
            request.start();
            request.join();
        } catch (MalformedURLException e) {
            log.warning("Invalid url for delete request");
        } catch (InterruptedException e) {
            log.warning("Thread interrupted when making request to API");
        }
    }
}
