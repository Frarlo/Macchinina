package gov.ismonnet.commons.netty.charstuffing;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CharStuffingDecoder extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CharStuffingDecoder.class);

    private final byte dle;
    private final byte stx;
    private final byte etx;

    private boolean isEscaped;

    private boolean isReadingFrame;
    private ByteBuf frame;

    public CharStuffingDecoder() {
        this((byte) 10, (byte) 2, (byte) 3);
    }

    public CharStuffingDecoder(byte dle, byte stx, byte etx) {
        this.dle = dle;
        this.stx = stx;
        this.etx = etx;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while(in.isReadable()) {
            final byte b = in.readByte();

            if(!isEscaped) {
                if(b == dle)
                    isEscaped = true;
                else if(isReadingFrame)
                    frame.writeByte(b);
            } else /*if (isEscaped)*/ {
                isEscaped = false;

                if(b == stx && !isReadingFrame) {
                    // Start of the packet
                    isReadingFrame = true;
                    frame = Unpooled.buffer();
                } else if(b == dle && isReadingFrame) {
                    // It's escaped, so unwrap it
                    frame.writeByte(b);
                } else if(b == etx && isReadingFrame) {
                    // End of the packet
                    out.add(frame);
                    isReadingFrame = false;
                    frame = null;
                } else {
                    // We are just going to log it and not throw an exception
                    // as the decoder can easily discard the data and go on
                    LOGGER.warn("There was an error while decoding packets. Discarding data." +
                            "(currByte: {}, isEscaped: {}, isReadingFrame: {})",
                            b, isEscaped, isReadingFrame);

                    isReadingFrame = false;
                    frame = null;
                }
            }
        }
    }
}
