package com.example.webrtctest;

import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;

public class PeerConnectionAdapter implements PeerConnection.Observer {
    private String TAG = "MAIN_ACTIVITY_PeerConnectionAdapter";
    private String tag;

    public PeerConnectionAdapter(String tag) {
        this.tag = "chao " + tag;
    }

    private void log(String s) {
        Log.d(tag, s);
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.d(TAG ,"onSignalingChange");
        log("onSignalingChange " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d(TAG ,"onIceConnectionChange");
        log("onIceConnectionChange " + iceConnectionState);
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.d(TAG ,"onIceConnectionReceivingChange");
        log("onIceConnectionReceivingChange " + b);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG ,"onIceGatheringChange");
        log("onIceGatheringChange " + iceGatheringState);
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG ,"onIceCandidate");
        log("onIceCandidate " + iceCandidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        Log.d(TAG ,"onIceCandidatesRemoved");
        log("onIceCandidatesRemoved " + iceCandidates);
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d(TAG ,"onAddStream");
        log("onAddStream " + mediaStream);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(TAG ,"onRemoveStream");
        log("onRemoveStream " + mediaStream);
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.d(TAG ,"onDataChannel");
        log("onDataChannel " + dataChannel);
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG ,"onRenegotiationNeeded");
        log("onRenegotiationNeeded ");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.d(TAG ,"onAddTrack");
        log("onAddTrack " + mediaStreams);
    }
}