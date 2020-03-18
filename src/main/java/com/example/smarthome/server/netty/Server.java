package com.example.smarthome.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class Server implements Runnable {

    public static final Logger log;

    static {
        log = LoggerFactory.getLogger(Server.class);
    }

    private final int PORT;

    public Server(int port) {
        PORT = port;
        new Thread(this, "Netty Server").start();
    }

    @Override
    public void run() {
        log.debug("Netty thread is running");

        EventLoopGroup bossGroup = new EpollEventLoopGroup();
        EventLoopGroup workerGroup = new EpollEventLoopGroup();

        SslContext sslCtx = null;

        try {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                    .sslProvider(SslProvider.OPENSSL)
                    .build();
        } catch (SSLException | CertificateException e) {
            e.printStackTrace();
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .option(ChannelOption.SO_BACKLOG, 1000) // limit of connections
                    .channel(EpollServerSocketChannel.class)
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
