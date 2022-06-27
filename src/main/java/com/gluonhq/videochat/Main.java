package com.gluonhq.videochat;

import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCAnswerOptions;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCIceGatheringState;
import dev.onvoid.webrtc.RTCOfferOptions;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCPeerConnectionIceErrorEvent;
import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCRtpReceiver;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.RTCSignalingState;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.MediaStream;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.audio.AudioTrackSink;
import dev.onvoid.webrtc.media.audio.AudioTrackSource;
import dev.onvoid.webrtc.media.video.VideoCaptureCapability;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoTrack;
import dev.onvoid.webrtc.media.video.VideoTrackSink;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    MySetSessionDescriptionObserver mySetSessionDescriptionObserver = new MySetSessionDescriptionObserver();

    @Override
    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Label label = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
        Button send = new Button("send");
        send.setOnAction(e -> executorService.submit(() -> connect()));
        Button receive = new Button("receive");
        receive.setOnAction(e -> executorService.submit(() -> receive()));
        Button answer = new Button("gotAnswer");
        answer.setOnAction(e -> executorService.submit(() -> processAnswer()));
        HBox box = new HBox(20, send, answer, receive);
        VBox root = new VBox(30, label, box);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.show();
//        Thread t = new Thread() {
//            @Override
//            public void run() {
//                try {
//                    connect();
//                } catch (Throwable t) {
//                    t.printStackTrace();
//                }
//            }
//        };
//        t.start();
    }
    PeerConnectionFactory pcf;
    RTCPeerConnection peerConnection;

    private void receive() {
        try {
            LOG.info("create pcf");
            pcf = new PeerConnectionFactory();
            MyPeerConnectionObserver mpco = new MyPeerConnectionObserver();
            MySetSessionDescriptionObserver mssdo = new MySetSessionDescriptionObserver();
            RTCConfiguration config = new RTCConfiguration();
            peerConnection = pcf.createPeerConnection(config, mpco);
            String spd = Files.readString(Path.of("/tmp/rcv"));
         //   LOG.info("input = " + spd);
            RTCSessionDescription description = new RTCSessionDescription(RTCSdpType.OFFER, spd);
     //       LOG.info(Thread.currentThread()+"created sessiondesc with spd = "+description.sdp);
            peerConnection.setRemoteDescription(description, mssdo);
            LOG.info("Peer has remote desc");
            RTCAnswerOptions options = new RTCAnswerOptions();
            MyCreateSessionDescriptionObserver sdobs = new MyCreateSessionDescriptionObserver(peerConnection, true);
            peerConnection.createAnswer(options, sdobs);
            RTCSessionDescription localDescription = sdobs.getDescription();
            peerConnection.setLocalDescription(localDescription, mssdo);
            Files.writeString(Path.of("/tmp/answer"), localDescription.sdp);
            LOG.info("Answer is written");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void connect() {
        LOG.info("create pcf");
        pcf = new PeerConnectionFactory();
        MyPeerConnectionObserver mpco = new MyPeerConnectionObserver();
        RTCConfiguration config = new RTCConfiguration();
        peerConnection = pcf.createPeerConnection(config, mpco);
        processAudioDevice();
        processVideoDevice();

        RTCOfferOptions options = new RTCOfferOptions();
        MyCreateSessionDescriptionObserver mcsdo = new MyCreateSessionDescriptionObserver(peerConnection, true);
        peerConnection.createOffer(options, mcsdo);
        RTCSessionDescription localDescription = mcsdo.getDescription();
        peerConnection.setLocalDescription(localDescription, mySetSessionDescriptionObserver);
        //  peerConnection.addTrack(track, streamIds);
        LOG.info("That's pcf");
        try {
            Files.writeString(Path.of("/tmp/rcv"), localDescription.sdp);
//        pcf.
//        RTCPeerConnection rpc;
//        RTCPeerConnectionFactory.
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void processAnswer() {
        try {
            LOG.info("Processing answer");
            String spd = Files.readString(Path.of("/tmp/answer"));
            RTCSessionDescription description = new RTCSessionDescription(RTCSdpType.ANSWER, spd);
            LOG.info("Created desc for answer, set as remote description");
            peerConnection.setRemoteDescription(description, mySetSessionDescriptionObserver);
            LOG.info("Done processing answer");
        } catch (Throwable ex) {
            ex.printStackTrace();
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    private void processAudioDevice() {
        List<AudioDevice> audioCaptureDevices = MediaDevices.getAudioCaptureDevices();
        System.err.println("AudioDevices = " + audioCaptureDevices);
        if (audioCaptureDevices.size() < 1) {
            System.err.println("Can't find an audio capture device");
            return;
        }
        //   AudioDevice audioDevice = audioCaptureDevices.get(0);
        AudioDevice audioDevice = MediaDevices.getDefaultAudioCaptureDevice();

        AudioOptions audioOptions = new AudioOptions();

        AudioTrackSource audioSource = pcf.createAudioSource(audioOptions);
        AudioTrack audioTrack = pcf.createAudioTrack("audio0", audioSource);
        audioTrack.addSink(new AudioTrackSink() {
            @Override
            public void onData(byte[] data, int bitsPerSample, int sampleRate, int channels, int frames) {
                System.err.println("Got audiodata! " + data);
            }
        });
        peerConnection.addTrack(audioTrack, List.of("stream"));
    }

    private void processVideoDevice() {
        List<VideoDevice> videoCaptureDevices = MediaDevices.getVideoCaptureDevices();
        System.err.println("videocapturedevices = " + videoCaptureDevices);
        if (videoCaptureDevices.size() < 1) {
            System.err.println("Can't find a video capture device");
            return;
        }
        VideoDevice videoDevice = videoCaptureDevices.get(0);
        List<VideoCaptureCapability> videoCaptureCapabilities = MediaDevices.getVideoCaptureCapabilities(videoDevice);

        System.err.println("caps = " + videoCaptureCapabilities);
        VideoDeviceSource videoSource = new VideoDeviceSource();

        videoSource.setVideoCaptureDevice(videoDevice);
        if (videoCaptureCapabilities.size() > 0) {
            videoSource.setVideoCaptureCapability(videoCaptureCapabilities.get(0));
        }
        VideoTrack videoTrack = pcf.createVideoTrack("videoTrack", videoSource);
        VideoTrackSink sink = frame -> System.err.println("GotFrame: " + frame);
        videoTrack.addSink(sink);
//        videoSource.stop();
//        videoSource.start();
        peerConnection.addTrack(videoTrack, List.of("stream"));
    }

    public static void main(String[] args) {
        launch(args);
    }

    static class MyPeerConnectionObserver implements PeerConnectionObserver {

        @Override
        public void onTrack(RTCRtpTransceiver transceiver) {
            LOG.info("Got a track: "+transceiver);
            LOG.info("NYI1");
        }

        @Override
        public void onRemoveTrack(RTCRtpReceiver receiver) {
            LOG.severe("NYI2");
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void onAddTrack(RTCRtpReceiver receiver, MediaStream[] mediaStreams) {
            LOG.info("Adding a track: "+receiver+" with mediaStreams "+mediaStreams);
            LOG.severe("NYI");
        }

        @Override
        public void onRenegotiationNeeded() {
            LOG.severe("NYI4");
            Thread.dumpStack();
        }

        @Override
        public void onDataChannel(RTCDataChannel dataChannel) {
            LOG.severe("NYI5");
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void onRemoveStream(MediaStream stream) {
            LOG.severe("NYI6");
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void onAddStream(MediaStream stream) {
            LOG.severe("NYI7");
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void onIceCandidatesRemoved(RTCIceCandidate[] candidates) {
            LOG.severe("NYI8");
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void onIceCandidateError(RTCPeerConnectionIceErrorEvent event) {
            LOG.severe("NYI9");
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void onIceGatheringChange(RTCIceGatheringState state) {
            LOG.severe("NYI");
            System.err.println("iceGatheringState changed to " + state);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
            LOG.severe("NYI");
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void onStandardizedIceConnectionChange(RTCIceConnectionState state) {
            LOG.severe("NYI");
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void onIceConnectionChange(RTCIceConnectionState state) {
            LOG.info("New iceConnectionChange = "+state);
            LOG.severe("NYI");
        }

        @Override
        public void onConnectionChange(RTCPeerConnectionState state) {
            LOG.severe("NYI");
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void onSignalingChange(RTCSignalingState state) {
            LOG.info("SignalingState changed to " + state);
        }

        @Override
        public void onIceCandidate(RTCIceCandidate candidate) {
            LOG.severe("NYI");
            LOG.info("new candidate: " + candidate);
            throw new RuntimeException("Not implemented");
        }

    }

    static class MyCreateSessionDescriptionObserver implements CreateSessionDescriptionObserver {

        private final RTCPeerConnection peerConnection;
        private boolean local;
        RTCSessionDescription description;
        private final CountDownLatch cdl;

        public MyCreateSessionDescriptionObserver(RTCPeerConnection peerConnection, boolean local) {
            this.peerConnection = peerConnection;
            this.local = local;
            this.cdl = new CountDownLatch(1);
        }

        @Override
        public void onSuccess(RTCSessionDescription description) {
            LOG.info("got description " + description);
            MySetSessionDescriptionObserver mssdo = new MySetSessionDescriptionObserver();
            cdl.countDown();
            this.description = description;
//            if (local) {
//                peerConnection.setLocalDescription(description, mssdo);
//            } else {
//                peerConnection.setRemoteDescription(description, mssdo);
//            }
        }

        @Override
        public void onFailure(String error) {
            LOG.info("failed desc");
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public RTCSessionDescription getDescription() {
            try {
                cdl.await(1, TimeUnit.MINUTES);
            } catch (InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            return description;
        }
    }

    static class MySetSessionDescriptionObserver implements SetSessionDescriptionObserver {

        @Override
        public void onSuccess() {
            LOG.info("SetSessionDescription success");
        }

        @Override
        public void onFailure(String error) {
            LOG.info("SetSessionDescription failed with " + error);
        }

    }
}
