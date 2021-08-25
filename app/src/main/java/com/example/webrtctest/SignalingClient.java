package com.example.webrtctest;


import android.telecom.Call;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
public class SignalingClient {
    private String TAG = "MAIN_ACTIVITY_SIGNALING";
        private static SignalingClient instance;
        private String mRoomName;

        private SignalingClient(){


        }



    public static SignalingClient get( ) {
            if(instance == null) {
                synchronized (SignalingClient.class) {
                    if(instance == null) {
                        instance = new SignalingClient();
                    }
                }
            }
            return instance;
        }

        private Socket socket;
//        private String room = "OldPlace";
        private Callback callback;

        private final TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        public void init(Callback callback , String roomName) {
            Log.d(TAG ,"init");
            mRoomName = roomName;
            Log.d(TAG ,"mRoomName: "+mRoomName);
            this.callback = callback;
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAll, null);
                IO.setDefaultHostnameVerifier((hostname, session) -> true);
                IO.setDefaultSSLContext(sslContext);

                socket = IO.socket("https://3.35.236.251:8080");
                socket.connect();

                socket.emit("create or join", roomName);
                Log.d(TAG ,"create or join");

                socket.on("created", args -> {
                    Log.d(TAG ,"created");
                    Log.e("chao", "room created:" + socket.id());
                    callback.onCreateRoom();
                });
                socket.on("full", args -> {
                    Log.d(TAG ,"full");
                    Log.e("chao", "room full");
                });
                socket.on("join", args -> {
                    Log.d(TAG ,"join");
                    Log.e("chao", "peer joined " + Arrays.toString(args));
                    callback.onPeerJoined(String.valueOf(args[1]));
                });
                socket.on("joined", args -> {
                    Log.d(TAG ,"joined");
                    Log.e("chao", "self joined:" + socket.id());
                    callback.onSelfJoined();
                });
                socket.on("log", args -> {
                    Log.d(TAG ,"log");
                    Log.e("chao", "log call " + Arrays.toString(args));
                });
                socket.on("bye", args -> {
                    Log.d(TAG ,"bye");
                    Log.e("chao", "bye " + args[0]);
//                    callback.onPeerLeave((String) args[0]);
                    destroy();
                });
                socket.on("message", args -> {
                    Log.d(TAG ,"message");
                    Log.e("chao", "message " + Arrays.toString(args));
                    Object arg = args[0];
                    if(arg instanceof String) {

                    } else if(arg instanceof JSONObject) {
                        JSONObject data = (JSONObject) arg;
                        String type = data.optString("type");
                        Log.d(TAG ,"message"+type);
                        if("offer".equals(type)) {
                            callback.onOfferReceived(data);
                        } else if("answer".equals(type)) {
                            callback.onAnswerReceived(data);
                        } else if("candidate".equals(type)) {
                            callback.onIceCandidateReceived(data);
                        }
//                        else if ("bye".equals(type)){
//                            callback.onPeerLeave((String) args[0]);
//                        }
                    }
                });
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        public void destroy() {
            Log.d(TAG ,"destroy");

            socket.emit("bye", socket.id());
            socket.disconnect();
            socket.close();
            instance = null;
        }



        public void sendIceCandidate(IceCandidate iceCandidate, String to) {
            Log.d(TAG ,"sendIceCandidate" +to.toString());
//            Log.d(TAG ,"sendSessionDescription" +iceCandidate.toString());
            JSONObject jo = new JSONObject();

            try {
                jo.put("type", "candidate");
                jo.put("label", iceCandidate.sdpMLineIndex);
                jo.put("id", iceCandidate.sdpMid);
                jo.put("candidate", iceCandidate.sdp);
                jo.put("from", socket.id());
                jo.put("room", mRoomName);
                jo.put("to", to);

                socket.emit("message", jo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void sendSessionDescription(SessionDescription sdp, String to) {
            Log.d(TAG ,"sendSessionDescription" +to.toString());
//            Log.d(TAG ,"sendSessionDescription" +sdp.toString());
            JSONObject jo = new JSONObject();
            try {
                jo.put("type", sdp.type.canonicalForm());
                jo.put("sdp", sdp.description);
                jo.put("from", socket.id());
                jo.put("to", to);
                socket.emit("message", jo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

public interface Callback {
    void onCreateRoom();
    void onPeerJoined(String socketId);
    void onSelfJoined();
    void onPeerLeave(String msg);

    void onOfferReceived(JSONObject data);
    void onAnswerReceived(JSONObject data);
    void onIceCandidateReceived(JSONObject data);
}

}