/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.gluonhq.videochat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
        Thread t = new Thread() {
            @Override public void run() {
                try {
                    Socket socket = new Socket(dest, PORT);
                    dos = new DataOutputStream(socket.getOutputStream());
                } catch (IOException ex) {
                    Logger.getLogger(Signaling.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        t.start();
    }
    
    public static void listen() {
        Thread t = new Thread() {
            @Override public void run() {
                try {
                    ServerSocket ss = new ServerSocket(PORT);
                    Socket s = ss.accept();
                    dis = new DataInputStream(s.getInputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    private void writeOffer(String o) {
        try {
            o.getBytes();
            dos.writeInt(o.length());
            dos.write(o.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(Signaling.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getOffer () {
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
