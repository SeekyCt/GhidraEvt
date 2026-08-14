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

import org.apache.commons.lang3.ArrayUtils;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.mem.MemoryBlockException;
import ghidra.util.Msg;

public class IpcServer implements Runnable {
    private volatile int port;
    private CountDownLatch portReady = new CountDownLatch(1);
    private ObjectMapper mapper = new ObjectMapper();
    private volatile Program currentProgram;

    private static class Request {
        public long address;
        public int length;
    }

    public IpcServer() {
        mapper.configure(Feature.AUTO_CLOSE_SOURCE, false);
    }

    public void setCurrentProgram(Program currentProgram) {
        this.currentProgram = currentProgram;
    }

    @Override
    public void run() {
        // int portRequest = 0;
        int portRequest = 7777;
        try (ServerSocket server = new ServerSocket(portRequest, 1, InetAddress.getLoopbackAddress()))
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
                        Msg.info(this, "Got request " + Long.toHexString(request.address) + " " + request.length);

                        Address address = currentProgram.getAddressFactory().getDefaultAddressSpace().getAddress(request.address);
                        MemoryBlock block = currentProgram.getMemory().getBlock(address);
                        byte[] data = new byte[request.length];
                        int finalLength = block.getBytes(address, data);
                        data = ArrayUtils.subarray(data, 0, finalLength);

                        Msg.info(this, "Write");
                        output.write(data);
                        Msg.info(this, "Wrote");
                    }
                    Msg.info(this, "Finished client");
                }
                catch (MemoryAccessException e) {
                    Msg.warn(this, e);
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
