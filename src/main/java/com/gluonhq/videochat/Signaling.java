/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.gluonhq.videochat;

import dev.onvoid.webrtc.RTCIceCandidate;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author johan
 */
public class Signaling {

    static final int PORT = 43210;

    static DataInputStream dis;
    static DataOutputStream dos;

    public static void connect(String dest) {
        CountDownLatch cdl = new CountDownLatch(1);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(dest, PORT);
                    OutputStream os = socket.getOutputStream();
                    InputStream is = socket.getInputStream();
                    dos = new DataOutputStream(os);
                    dis = new DataInputStream(is);
                    cdl.countDown();
                } catch (IOException ex) {
                    Logger.getLogger(Signaling.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        t.start();
        try {
            cdl.await(10, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Logger.getLogger(Signaling.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * blocks until there is a connection
     */
    public static void listen() {
        CountDownLatch cdl = new CountDownLatch(1);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket(PORT);
                    Socket s = ss.accept();
                    OutputStream os = s.getOutputStream();
                    InputStream is = s.getInputStream();
                    dos = new DataOutputStream(os);
                    dis = new DataInputStream(is);
                    cdl.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
        try {
            cdl.await(10, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Logger.getLogger(Signaling.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void writeAnswer(String o) {
        writeLine(o);
    }

    public static String readAnswer() {
        return readLine();
    }

    public static void writeOffer(String o) {
        writeLine(o);
    }
    
    public static void writeIceCandidates(List<RTCIceCandidate> can) {
        try {
            int len = can.size();
            dos.writeInt(len);
            for (int i = 0; i < len; i++) {
                RTCIceCandidate candidate = can.get(i);
                writeLine(candidate.sdp);
                writeLine(candidate.sdpMid);
                writeLine(candidate.serverUrl);
                dos.writeInt(candidate.sdpMLineIndex);
            }
            dos.flush();
        } catch (IOException ex) {
            Logger.getLogger(Signaling.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static List<RTCIceCandidate> readIceCandidates() {
        List<RTCIceCandidate> answer = new LinkedList<>();
        try {
            int len = dis.readInt();
            for (int i = 0; i < len; i++) {
                String sdp = readLine();
                String sdpMid = readLine();
                String serverUrl = readLine();
                int sdpMLineIndex = dis.readInt();
                RTCIceCandidate candidate = new RTCIceCandidate(sdpMid, sdpMLineIndex, sdp, serverUrl);
            }
        } catch (IOException ex) {
            Logger.getLogger(Signaling.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }
    
    public static String readOffer() {
        return readLine();
    }

    private static void writeLine(String o) {
        try {
            dos.writeInt(o.length());
            dos.write(o.getBytes());
            dos.flush();
        } catch (Throwable ex) {
            ex.printStackTrace();
            Logger.getLogger(Signaling.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String readLine() {
        try {
            int len = dis.readInt();
            byte[] b = new byte[len];
            int read = dis.read(b);
            String off = new String(b);
            return off;
        } catch (IOException ex) {
            Logger.getLogger(Signaling.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
