package com.example.webrtctest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback;
import org.webrtc.voiceengine.WebRtcAudioEffects;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SignalingClient.Callback {
    MediaConstraints audioConstraints;
    AudioSource audioSource;
    AudioTrack localAudioTrack;


    EglBase.Context eglBaseContext;
    PeerConnectionFactory peerConnectionFactory;

    MediaStream mediaStream;
    List<PeerConnection.IceServer> iceServers;

    HashMap<String, PeerConnection> peerConnectionMap;
    SurfaceViewRenderer[] remoteViews;
    SurfaceViewRenderer localView;
//    SurfaceViewRenderer remoteView;
    int remoteViewsIndex = 0;
    String roomName;
    private String TAG = "MAIN_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG ,"ONCREATE");
//        roomName = "roomtest";

        Intent getintent = getIntent();
        roomName =getintent.getStringExtra("roomName");

        peerConnectionMap = new HashMap<>();
        iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        eglBaseContext = EglBase.create().getEglBaseContext();
        // create PeerConnectionFactory
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                .builder(this)
                .createInitializationOptions());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        DefaultVideoEncoderFactory defaultVideoEncoderFactory =
                new DefaultVideoEncoderFactory(eglBaseContext, true, true);

        DefaultVideoDecoderFactory defaultVideoDecoderFactory =
                new DefaultVideoDecoderFactory(eglBaseContext);



        //오디오 모듈을 집어넣는다 .
        AudioDeviceModule audioDeviceModule = JavaAudioDeviceModule.builder ( getApplicationContext() )
                .setUseHardwareAcousticEchoCanceler ( false )
                .setUseHardwareNoiseSuppressor ( false )
                .createAudioDeviceModule ();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();


        //비디오 트랙 채널과 소스
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext);
        VideoCapturer videoCapturer = createCameraCapturer(true);
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturer.startCapture(480, 640, 30);
        VideoTrack videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);


        localView = findViewById(R.id.localView);
        localView.setMirror(true);
        localView.init(eglBaseContext, null);


        //오디오 트랙 채널과 소스
        audioConstraints = new MediaConstraints();
        //        audioConstraints.mandatory.add(
//                new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
//        audioConstraints.mandatory.add(
//                new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
//        audioConstraints.mandatory.add(
//                new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
//        audioConstraints.mandatory.add(
//                new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);
        localAudioTrack.setVolume(10);

        //로컬뷰
        videoTrack.addSink(localView);

        //리모트뷰 
//        remoteViews = new SurfaceViewRenderer[]{
//                findViewById(R.id.remoteView),
//                findViewById(R.id.remoteView2),
//                findViewById(R.id.remoteView3),
//        };
//        for (SurfaceViewRenderer remoteView : remoteViews) {
//            remoteView.setMirror(false);
//            remoteView.init(eglBaseContext, null);
//        }


        mediaStream = peerConnectionFactory.createLocalMediaStream("mediaStream");
        //미디어 스트림에 비디오트랙 넣기
        mediaStream.addTrack(videoTrack);
        //미디어 스트림에 오디오 트랙에 넣기
        mediaStream.addTrack(localAudioTrack);



//        Log.d("onAddStreamRemote", ""+ mediaStream.videoTracks.get(0).toString());
        AudioManager am;

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setSpeakerphoneOn(true);

        Log.d("PeerHashMap" ," "+peerConnectionMap);

        SignalingClient.get().init(this,roomName);
    }


    private synchronized PeerConnection getOrCreatePeerConnection(String socketId) {

        Log.d(TAG ,"getOrCreatePeerConnection");
        PeerConnection peerConnection = peerConnectionMap.get(socketId);
        if (peerConnection != null) {
            return peerConnection;
        }
        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, new PeerConnectionAdapter("PC:" + socketId) {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                SignalingClient.get().sendIceCandidate(iceCandidate, socketId);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);

                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                runOnUiThread(() -> {
//                    remoteVideoTrack.addSink(remoteViews[remoteViewsIndex++]) ;

                });
            }
        });
        peerConnection.addStream(mediaStream);
        peerConnectionMap.put(socketId, peerConnection);
        return peerConnection;
    }

    @Override
    public void onCreateRoom() {
        Log.d(TAG ,"onCreateRoom");
    }

    @Override
    public void onPeerJoined(String socketId) {
        Log.d(TAG ,"onPeerJoined");
        PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
        peerConnection.createOffer(new SdpAdapter("createOfferSdp:" + socketId) {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(new SdpAdapter("setLocalSdp:" + socketId), sessionDescription);
                SignalingClient.get().sendSessionDescription(sessionDescription, socketId);
            }
        }, new MediaConstraints());
    }

    @Override
    public void onSelfJoined() {
        Log.d(TAG ,"onSelfJoined");
    }

    @Override
    public void onPeerLeave(String msg) {
        Log.d(TAG ,"onPeerLeave");
        Log.d(TAG ,"msg");

    }

    @Override
    public void onOfferReceived(JSONObject data) {

        Log.d(TAG ,"onOfferReceived"+data.toString());
        runOnUiThread(() -> {
            final String socketId = data.optString("from");
            PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
            peerConnection.setRemoteDescription(new SdpAdapter("setRemoteSdp:" + socketId),
                    new SessionDescription(SessionDescription.Type.OFFER, data.optString("sdp")));
            peerConnection.createAnswer(new SdpAdapter("localAnswerSdp") {
                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                    super.onCreateSuccess(sdp);
                    peerConnectionMap.get(socketId).setLocalDescription(new SdpAdapter("setLocalSdp:" + socketId), sdp);
                    SignalingClient.get().sendSessionDescription(sdp, socketId);
                }
            }, new MediaConstraints());

        });
    }

    @Override
    public void onAnswerReceived(JSONObject data) {

        Log.d(TAG ,"onAnswerReceived" + data.toString());
        String socketId = data.optString("from");
        PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
        peerConnection.setRemoteDescription(new SdpAdapter("setRemoteSdp:" + socketId),
                new SessionDescription(SessionDescription.Type.ANSWER, data.optString("sdp")));
    }

    @Override
    public void onIceCandidateReceived(JSONObject data) {
        Log.d(TAG ,"onIceCandidateReceived"+data.toString());
        String socketId = data.optString("from");
        PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
        peerConnection.addIceCandidate(new IceCandidate(
                data.optString("id"),
                data.optInt("label"),
                data.optString("candidate")
        ));
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG ,"onDestroy");
        super.onDestroy();
        Log.d("PeerHashMap" ," "+peerConnectionMap);
        SignalingClient.get().destroy();
    }


    private VideoCapturer createCameraCapturer(boolean isFront) {
        Log.d(TAG ,"createCameraCapturer");
        Camera1Enumerator enumerator = new Camera1Enumerator(false);
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (isFront ? enumerator.isFrontFacing(deviceName) : enumerator.isBackFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }
}