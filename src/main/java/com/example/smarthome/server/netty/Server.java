package com.example.smarthome.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {

    private static Logger LOGGER = Logger.getLogger(Server.class.getName());
    private final int PORT;

    public Server(int port) {
        this.PORT = port;
        new Thread(this, "Netty Server").start();
    }

    @Override
    public void run() {
        LOGGER.log(Level.INFO, "Netty thread is running");

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        SslContext sslCtx = null;

        try {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                    .build();
        } catch (SSLException | CertificateException e) {
            e.printStackTrace();
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .option(ChannelOption.SO_BACKLOG, 100) // limit of connections
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerInitializer(sslCtx));

            bootstrap.bind(PORT).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace(); // log the exception
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
