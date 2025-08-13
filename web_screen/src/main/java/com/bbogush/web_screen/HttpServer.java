package com.bbogush.web_screen;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.KeyManagerFactory;

import fi.iki.elonen.NanoWSD;

public class HttpServer extends NanoWSD {
    private static final String TAG = "WebRtcManager";
    private static final String MIME_IMAGE_SVG = "image/svg+xml";
    private static final String MIME_JS = "text/javascript";
    private static final String MIME_TEXT_PLAIN_JS = "text/plain";
    private static final String MIME_TEXT_CSS = "text/css";
    private static final String TYPE_PARAM = "type";
    private static final String TYPE_VALUE_JOIN = "join";
    private static final String TYPE_VALUE_SDP = "sdp";
    private static final String TYPE_VALUE_ICE = "ice";
    private static final String TYPE_VALUE_BYE = "bye";

    private final Context context;
    private Ws webSocket = null;

    private final HttpServer.HttpServerInterface httpServerInterface;

    public HttpServer(int port, Context context, HttpServer.HttpServerInterface httpServerInterface) {
        super(port);
        this.context = context;
        this.httpServerInterface = httpServerInterface;
        configSecurity();
    }

    private void configSecurity() {
        final String keyPassword = "presscott";
        final String certPassword = "presscott";

        try {
            InputStream keyStoreStream = context.getAssets().open("private/keystore.bks");
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(keyStoreStream, keyPassword.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, certPassword.toCharArray());
            makeSecure(makeSSLSocketFactory(keyStore, keyManagerFactory), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class Ws extends WebSocket {
        private static final int PING_INTERVAL = 20000;
        private Timer pingTimer = new Timer();

        public Ws(IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen() {
            Log.d(TAG, "WebSocket open");
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        Ws.this.ping(new byte[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            pingTimer.scheduleAtFixedRate(timerTask, PING_INTERVAL, PING_INTERVAL);
        }

        @Override
        protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            Log.d(TAG, "WebSocket close");
            pingTimer.cancel();
            httpServerInterface.onWebSocketClose();
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            JSONObject json;
            try {
                json = new JSONObject(message.getTextPayload());
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            handleRequest(json);
        }

        @Override
        protected void onPong(WebSocketFrame pong) {
        }

        @Override
        protected void onException(IOException exception) {
            Log.d(TAG, "WebSocket exception");
        }
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        webSocket = new Ws(handshake);
        return webSocket;
    }

    @Override
    protected Response serveHttp(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        return serveRequest(session, uri, method);
    }

    public interface HttpServerInterface {
        void onJoin(HttpServer server);

        void onSdp(JSONObject message);

        void onIceCandidate(JSONObject message);

        void onBye();

        void onWebSocketClose();
    }

    public void send(String message) throws IOException {
        if (webSocket != null) {
            Log.d(TAG, "发送:" + message);
            webSocket.send(message);
        }
    }

    private Response serveRequest(IHTTPSession session, String uri, Method method) {
        if (Method.GET.equals(method))
            return handleGet(session, uri);
        return notFoundResponse();
    }

    private Response notFoundResponse() {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }

    private Response internalErrorResponse() {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error");
    }

    private Response handleGet(IHTTPSession session, String uri) {
        if (uri.contentEquals("/")) {
            return handleRootRequest(session);
        } else if (uri.contains("private")) {
            return notFoundResponse();
        }

        return handleFileRequest(session, uri);
    }

    private Response handleRootRequest(IHTTPSession session) {
        String indexHtml = readFile();
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, indexHtml);
    }

    private void handleRequest(JSONObject json) {
        String type;
        try {
            type = json.getString(TYPE_PARAM);
            Log.d(TAG, "收到:" + type);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        switch (type) {
            case TYPE_VALUE_JOIN:
                httpServerInterface.onJoin(this);
                break;
            case TYPE_VALUE_SDP:
                httpServerInterface.onSdp(json);
                break;
            case TYPE_VALUE_ICE:
                httpServerInterface.onIceCandidate(json);
                break;
            case TYPE_VALUE_BYE:
                httpServerInterface.onBye();
                break;
        }
    }

    private String readFile() {
        InputStream fileStream;
        StringBuilder string = new StringBuilder();
        try {
            fileStream = context.getAssets().open("html/index.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null)
                string.append(line);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return string.toString();
    }

    private Response handleFileRequest(IHTTPSession session, String uri) {
        String relativePath = uri.startsWith("/") ? uri.substring(1) : uri;

        InputStream fileStream;
        try {
            fileStream = context.getAssets().open(relativePath);
        } catch (IOException e) {
            e.printStackTrace();
            return notFoundResponse();
        }

        String mime;
        if (uri.contains(".js"))
            mime = MIME_JS;
        else if (uri.contains(".svg"))
            mime = MIME_IMAGE_SVG;
        else if (uri.contains(".css"))
            mime = MIME_TEXT_CSS;
        else
            mime = MIME_TEXT_PLAIN_JS;

        return newChunkedResponse(Response.Status.OK, mime, fileStream);
    }

    private void notifyAboutNewConnection(IHTTPSession session) {
        // The message is used to trigger screen redraw on new connection
        final String remoteAddress = session.getRemoteIpAddress();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "WebScreen\nNew connection from " + remoteAddress,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
