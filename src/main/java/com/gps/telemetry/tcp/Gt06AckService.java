package com.gps.telemetry.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Service;

@Service
public class Gt06AckService {

    public void sendAck(
            ChannelHandlerContext ctx,
            byte protocolNumber,
            byte[] serialNumber) {

        ByteBuf response = Unpooled.buffer();

        response.writeByte(0x78);
        response.writeByte(0x78);

        response.writeByte(0x05);
        response.writeByte(protocolNumber);

        response.writeBytes(serialNumber);

        int crc = calculateCrc(
                response,
                3,
                response.writerIndex()
        );

        response.writeShort(crc);

        response.writeByte(0x0D);
        response.writeByte(0x0A);

        ctx.writeAndFlush(response);

        System.out.println(
                "GT06 ACK sent for protocol: " +
                String.format(
                        "0x%02X",
                        protocolNumber & 0xFF
                )
        );
    }

    private int calculateCrc(
            ByteBuf buffer,
            int start,
            int end) {

        int crc = 0xFFFF;

        for (int i = start; i < end; i++) {

            crc ^= buffer.getUnsignedByte(i);

            for (int j = 0; j < 8; j++) {

                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0x8408;
                } else {
                    crc >>>= 1;
                }
            }
        }

        return (~crc) & 0xFFFF;
    }
}