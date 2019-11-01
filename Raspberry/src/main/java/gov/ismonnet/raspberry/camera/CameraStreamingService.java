package gov.ismonnet.raspberry.camera;

import gov.ismonnet.commons.di.LifeCycle;
import gov.ismonnet.commons.di.LifeCycleService;
import gov.ismonnet.commons.netty.protocol.spacket.H264NaluPacket;
import gov.ismonnet.commons.utils.SneakyThrow;
import gov.ismonnet.raspberry.netty.ServerNetService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

class CameraStreamingService implements LifeCycle {

    private final ServerNetService serverNetService;
    private final InputStream h264InputStream;
    private final ExecutorService executor;

    private Future<?> future;

    private long lastBytes;

    @Inject CameraStreamingService(ServerNetService serverNetService,
                                   @Camera InputStream h264InputStream,
                                   @Camera ExecutorService executor,
                                   LifeCycleService lifeCycleService) {
        this.serverNetService = serverNetService;
        this.h264InputStream = h264InputStream;
        this.executor = executor;

        lifeCycleService.register(this);
    }


    @Override
    public void start() {
        // Thank you
        // https://stackoverflow.com/questions/24884827/possible-locations-for-sequence-picture-parameter-sets-for-h-264-stream/24890903#24890903
        future = executor.submit(() -> SneakyThrow.runUnchecked(() -> {

            ByteBuf currentNalu = null;
            boolean sendReliably;
            byte oldNaluType = -1;

            while(!Thread.currentThread().isInterrupted()) {
                final byte b = readByte();
                if(currentNalu != null)
                    currentNalu.writeByte(b);

                // Nal unit start
                boolean foundNalUnitStart;
                int startCodeSize = -1;

                // NALU start code
                // 3 bytes
                if(foundNalUnitStart = (getLastBytes(1, 3) == 0x000001))
                    startCodeSize = 3;
                // 4 bytes
                if(!foundNalUnitStart && (foundNalUnitStart = getLastBytes(1, 4) == 0x00000001))
                    startCodeSize = 4;

                if(foundNalUnitStart) {

                    if(currentNalu != null)
                        System.out.println(oldNaluType + " " + currentNalu.readableBytes() + " " + currentNalu);

                    // The first byte of each NALU contains the NALU type, specifically bits 3 through 7.
                    // (bit 0 is always off, and bits 1-2 indicate whether a NALU is referenced by another NALU).
                    byte naluType = (byte) getLastBytes(startCodeSize + 1, 1);
                    naluType = (byte) (((byte)(naluType << 3)) >>> 3);

                    oldNaluType = naluType;

                    if(currentNalu != null)
                        serverNetService.sendPacketToAll(new H264NaluPacket(currentNalu), true);

                    sendReliably = naluType == 7; // Sequence parameter set
                    sendReliably = sendReliably || naluType == 8; // Picture parameter set

                    currentNalu = Unpooled.buffer();
                    for(int i = 0; i < startCodeSize + 1; i++)
                        currentNalu.writeByte(readByte());
                }
            }

        }));
    }

    private byte readByte() throws IOException {
        final int b = h264InputStream.read();
        if(b == -1)
            throw new AssertionError("The camera streaming reached the end of stream");
        lastBytes = (lastBytes << 8) | b;
        return (byte) getLastBytes(1);
    }

    private long getLastBytes() {
        return lastBytes;
    }

    private long getLastBytes(int length) {
        return getLastBytes(0, length);
    }

    private long getLastBytes(int offset, int length) {
        if(length < 0 || length > 8)
            throw new AssertionError("A long is 8 bytes");
        return (lastBytes << (8 * offset)) >>> (8 * (8 - length));
    }

    @Override
    public void stop() {
        future.cancel(true);
        executor.shutdown();
    }
}
