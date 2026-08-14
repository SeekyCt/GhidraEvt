package ghidraevt;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import ghidra.util.Msg;

public class IpcServer implements Runnable {
    private volatile int port;
    private CountDownLatch portReady = new CountDownLatch(1);
    private ObjectMapper mapper = new ObjectMapper();

    private static class Request {
        public long length;
    }

    public IpcServer() {
        mapper.configure(Feature.AUTO_CLOSE_SOURCE, false);
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()))
        {
            port = server.getLocalPort();
            Msg.info(this, "Started server on " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());
            portReady.countDown();

            while (true) {
                try (Socket socket = server.accept()) {
                    var input = socket.getInputStream();
                    OutputStream output = socket.getOutputStream();
                    Msg.info(this, "Got client");

                    MappingIterator<Request> reader = mapper.readerFor(Request.class).readValues(input);
                    int i = 0;
                    while (reader.hasNextValue()) {
                        Request request = reader.nextValue();
                        Msg.info(this, "Got request " + " " + request.length);

                        Msg.info(this, "Write");
                        if (i++ % 4 == 3)
                            output.write(new byte[] {1});
                        else
                            output.write(new byte[] {0});
                        Msg.info(this, "Wrote");
                    }
                    Msg.info(this, "Finished client");

                    // Request request = mapper.readValue(input, Request.class);
                    // Msg.info(this, "Got request " + " " + request.length);

                    // Msg.info(this, "Write");
                    // output.write(new byte[] {0, 0, 0, 1});
                    // Msg.info(this, "Wrote");
                }
                catch (IOException e) {
                    Msg.warn(this, e);
                }
            }
        }
        catch (IOException e) {

        }
    }

    public int getPort() throws InterruptedException {
        portReady.await();
        return port;
    }
    
}
