package com.gps.telemetry.tcp;
import com.gps.telemetry.service.TelemetryService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
public class Gt06TcpHandler extends ChannelInboundHandlerAdapter {
    private final Gt06PacketDecoder decoder;
    private final Gt06AckService ackService;
    private final Gt06PacketFramer packetFramer;
    private ChannelHandlerContext currentContext;
    public Gt06TcpHandler(
            Gt06AckService ackService,
            TelemetryService telemetryService) {
        this.ackService = ackService;
        this.decoder = new Gt06PacketDecoder(telemetryService);
        this.packetFramer = new Gt06PacketFramer(this::handlePacket);
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        currentContext = ctx;
        System.out.println(
                "Tracker connected: " +
                ctx.channel().remoteAddress()
        );
    }
    @Override
    public void channelRead(
            ChannelHandlerContext ctx,
            Object msg) {
        ByteBuf buffer = (ByteBuf) msg;
        byte[] bytes =
                new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        System.out.println(
                "Received TCP data: " +
                bytes.length +
                " bytes"
        );
        packetFramer.addBytes(bytes);
    }
    private void handlePacket(byte[] packet) {
        decoder.decode(packet);
        if (currentContext != null && packet.length >= 10) {
            byte protocolNumber = packet[3];
            byte[] serialNumber = {
                    packet[packet.length - 6],
                    packet[packet.length - 5]
            };
            System.out.println(
                    "Preparing ACK for protocol: " +
                    String.format(
                            "0x%02X",
                            protocolNumber & 0xFF
                    )
            );
            ackService.sendAck(
                    currentContext,
                    protocolNumber,
                    serialNumber
            );
        }
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        currentContext = null;
        System.out.println(
                "Tracker disconnected: " +
                ctx.channel().remoteAddress()
        );
    }
    @Override
    public void exceptionCaught(
            ChannelHandlerContext ctx,
            Throwable cause) {
        System.err.println(
                "TCP error: " +
                cause.getMessage()
        );
        ctx.close();
    }
}