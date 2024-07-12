package com.hardbacknutter.sshd;
/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
 * This program will demonstrate how to provide a network service like
 * inetd by using remote port-forwarding functionality.
 */

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelForwardedTCPIP;
import com.jcraft.jsch.ForwardedTCPIPDaemon;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.io.OutputStream;

public class Daemon {
    public static void main2(String privateKey) {
        try {
            String host = "127.0.0.1";
            int port = 2223;
            String user = "user";
            JSch jsch = new JSch();
            try {
                jsch.addIdentity(privateKey, "");
            } catch (JSchException e) {
                e.fillInStackTrace();
            }
            System.out.println("privateKeyPath: " + privateKey);
            Session session = jsch.getSession(user, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive");
            session.connect(5000);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("ls /sdcard");
            channel.connect();
            InputStream in = channel.getInputStream();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    System.out.println("exit-status: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main() {
        try {
            JSch jsch = new JSch();
            String host = "10.18.16.46";
            int port = 22;
            String user = "softfive";
            String passwd = "123456";
            Session session = jsch.getSession(user, host, port);
            int rport = 2222;
            session.setPassword(passwd);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            session.setPortForwardingR(rport, Parrot.class.getName());
//            session.setPortForwardingR(rport, "Daemon$Parrot");
            System.out.println(host + ":" + rport + " <--> " + "Parrot");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Parrot implements ForwardedTCPIPDaemon {
        ChannelForwardedTCPIP channel;
        Object[] arg;
        InputStream in;
        OutputStream out;

        public void setChannel(ChannelForwardedTCPIP c, InputStream in, OutputStream out) {
            this.channel = c;
            this.in = in;
            this.out = out;
        }

        public void setArg(Object[] arg) {
            this.arg = arg;
        }

        public void run() {
            try {
                byte[] buf = new byte[1024];
                System.out.println("remote port: " + channel.getRemotePort());
                System.out.println("remote host: " + channel.getSession().getHost());
                while (true) {
                    int i = in.read(buf, 0, buf.length);
                    if (i <= 0) break;
                    out.write(buf, 0, i);
                    out.flush();
                    if (buf[0] == '.') break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}