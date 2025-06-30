package com.genymobile.scrcpy;

import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;

import com.android.scrcpy.v2.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public final class Server {

    private static final String SERVER_PATH = "/data/local/tmp/scrcpy-server.jar";
    private static Handler handler;

    private Server() {
    }

    public static void scrcpy(Options options, SocketChannel videoSocket) throws IOException {
        final Device device = new Device(options);
        try (DesktopConnection connection = DesktopConnection.open(videoSocket)) {
            ScreenEncoder screenEncoder = new ScreenEncoder(options, device);
            handler = screenEncoder.getHandler();
            try {
                screenEncoder.streamScreen(device, connection.getVideoChannel());
            } catch (IOException e) {
                Ln.i("exit: " + e.getMessage());
            }
        }
    }


    @SuppressWarnings("checkstyle:MagicNumber")
    private static Options createOptions(String... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Missing client version");
        }

        String clientVersion = args[0];
        Ln.i("VERSION_NAME: " + BuildConfig.VERSION_NAME);
        if (!clientVersion.equals(BuildConfig.VERSION_NAME)) {
            throw new IllegalArgumentException("The server version (" + clientVersion + ") does not match the client " + "(" + BuildConfig.VERSION_NAME + ")");
        }

        if (args.length != 8) {
            throw new IllegalArgumentException("Expecting 8 parameters");
        }

        Options options = new Options();

        int maxSize = Integer.parseInt(args[1]) & ~7; // multiple of 8
        options.setMaxSize(maxSize);

        int bitRate = Integer.parseInt(args[2]);
        options.setBitRate(bitRate);

        int maxFps = Integer.parseInt(args[3]);
        options.setMaxFps(maxFps);

        Rect crop = parseCrop(args[5]);
        options.setCrop(crop);

        boolean sendFrameMeta = Boolean.parseBoolean(args[6]);
        options.setSendFrameMeta(sendFrameMeta);
        return options;
    }

    public static Options customOptions(String... args) {
        org.apache.commons.cli.CommandLine commandLine = null;
        org.apache.commons.cli.CommandLineParser parser = new org.apache.commons.cli.BasicParser();
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        options.addOption("Q", true, "JPEG quality (0-100)");
        options.addOption("r", true, "Frame rate (frames/s)");
        options.addOption("P", true, "Display projection (1080, 720, 480...).");
        options.addOption("c", false, "Control only");
        options.addOption("L", false, "Library path");
        options.addOption("D", false, "Dump window hierarchy");
        options.addOption("h", false, "Show help");
        try {
            commandLine = parser.parse(options, args);
        } catch (Exception e) {
            Ln.e(e.getMessage());
            System.exit(0);
        }

        if (commandLine.hasOption('h')) {
            System.out.println("Usage: [-h]\n\n" + "jpeg:\n" + "  -r <value>:    Frame rate (frames/sec).\n" + "  -P <value>:    Display projection (1080, 720, 480, 360...).\n" + "  -Q <value>:    JPEG quality (0-100).\n" + "\n" + "  -c:            Control only.\n" + "  -L:            Library path.\n" + "  -D:            Dump window hierarchy.\n" + "  -h:            Show help.\n");
            System.exit(0);
        }
        if (commandLine.hasOption('L')) {
            System.out.println(System.getProperty("java.library.path"));
            System.exit(0);
        }
        Options o = new Options();
        o.setMaxSize(0);
        o.setCrop(null);
        // global
        o.setMaxFps(24);
        o.setScale(480);
        // jpeg
        o.setQuality(60);
        o.setBitRate(1000000);
        o.setSendFrameMeta(true);
        // control
        o.setControlOnly(false);
        // dump
        o.setDumpHierarchy(false);
        if (commandLine.hasOption('Q')) {
            int i = 0;
            try {
                i = Integer.parseInt(commandLine.getOptionValue('Q'));
            } catch (Exception e) {
            }
            if (i > 0 && i <= 100) {
                o.setQuality(i);
            }
        }
        if (commandLine.hasOption('r')) {
            int i = 0;
            try {
                i = Integer.parseInt(commandLine.getOptionValue('r'));
            } catch (Exception e) {
            }
            if (i > 0 && i <= 100) {
                o.setMaxFps(i);
            }
        }
        if (commandLine.hasOption('P')) {
            int i = 0;
            try {
                i = Integer.parseInt(commandLine.getOptionValue('P'));
            } catch (Exception e) {
            }
            if (i > 0) {
                o.setScale(i);
            }
        }
        if (commandLine.hasOption('c')) {
            o.setControlOnly(true);
        }
        if (commandLine.hasOption('D')) {
            o.setDumpHierarchy(true);
        }
        return o;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private static Rect parseCrop(String crop) {
        if ("-".equals(crop)) {
            return null;
        }
        // input format: "width:height:x:y"
        String[] tokens = crop.split(":");
        if (tokens.length != 4) {
            throw new IllegalArgumentException("Crop must contains 4 values separated by colons: \"" + crop + "\"");
        }
        int width = Integer.parseInt(tokens[0]);
        int height = Integer.parseInt(tokens[1]);
        int x = Integer.parseInt(tokens[2]);
        int y = Integer.parseInt(tokens[3]);
        return new Rect(x, y, x + width, y + height);
    }

    private static void unlinkSelf() {
        try {
            new File(SERVER_PATH).delete();
        } catch (Exception e) {
            Ln.e("Could not unlink server", e);
        }
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private static void suggestFix(Throwable e) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (e instanceof MediaCodec.CodecException) {//api level 21
//                MediaCodec.CodecException mce = (MediaCodec.CodecException) e;
//                if (mce.getErrorCode() == 0xfffffc0e) {
//                    Ln.e("The hardware encoder is not able to encode at the given definition.");
//                    Ln.e("Try with a lower definition:");
//                    Ln.e("    scrcpy -m 1024");
//                }
//            }
        }
    }

    public static void main(String... args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Ln.e("Exception on thread " + t, e);
            suggestFix(e);
        });

//        unlinkSelf();
//        Options options = createOptions(args);
        final Options options = customOptions(args);
        Ln.i("Options frame rate: " + options.getMaxFps() + " (1 ~ 60)");
        Ln.i("Options quality: " + options.getQuality() + " (1 ~ 100)");
        Ln.i("Options projection: " + options.getScale() + " (1080, 720, 480, 360...)");
        Ln.i("Options control only: " + options.getControlOnly() + " (true / false)");
//        scrcpy(options);
    }
}
