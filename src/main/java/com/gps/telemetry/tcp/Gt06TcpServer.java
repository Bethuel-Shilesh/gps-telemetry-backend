package com.gps.telemetry.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gps.telemetry.service.TelemetryService;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class Gt06TcpServer {

    private static final Logger logger =
            LoggerFactory.getLogger(Gt06TcpServer.class);

    private final TelemetryService telemetryService;

    @Value("${gps.tcp.port}")
    private int port;

    private final Gt06AckService ackService;

    public Gt06TcpServer(
            Gt06AckService ackService,
            TelemetryService telemetryService) {

        this.ackService = ackService;
        this.telemetryService = telemetryService;
    }

    @PostConstruct
    public void initialize() {
        Thread tcpThread = new Thread(this::start);
        tcpThread.setName("gt06-tcp-server");
        tcpThread.start();
    }

    public void start() {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(
                                SocketChannel socketChannel) {

                            socketChannel.pipeline()
                                    .addLast(
                                            new Gt06TcpHandler(
                                                    ackService,
                                                    telemetryService
                                            )
                                    );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            Channel channel =
                    bootstrap.bind(port).sync().channel();

            logger.info(
                    "GT06 TCP Server started on port: {}",
                    port
            );

            channel.closeFuture().sync();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            logger.warn(
                    "GT06 TCP Server thread interrupted",
                    e
            );

        } finally {

            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}