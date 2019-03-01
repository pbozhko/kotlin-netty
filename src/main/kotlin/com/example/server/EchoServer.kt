package com.example.server

import com.example.common.DefaultProperties
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

object EchoServer {

    private val SSL = System.getProperty("ssl")?.toBoolean() ?: DefaultProperties.SSL
    private val PORT = System.getProperty("port")?.toInt() ?: DefaultProperties.PORT

    fun run() {

        // Configure SSL
        val sslContext = if (SSL) {
            val ssc = SelfSignedCertificate()
            SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build()
        } else null

        // Configure the server
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()

        val serverHandler = EchoServerHandler()

        try {
            val serverBootstrap = ServerBootstrap()

            serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .option(ChannelOption.SO_BACKLOG, 100)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(object : ChannelInitializer<SocketChannel>() {

                    override fun initChannel(ch: SocketChannel) {

                        val channelPipeline = ch.pipeline()

                        if (sslContext != null) {
                            channelPipeline.addLast(sslContext.newHandler(ch.alloc()))
                        }

                        channelPipeline.addLast(serverHandler)
                    }
                })

            // Start the server
            val channelFuture = serverBootstrap.bind(PORT).sync()

            // Wait until the server socket is closed
            channelFuture.channel().closeFuture().sync()
        } finally {

            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}