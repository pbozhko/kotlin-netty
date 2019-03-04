package com.example.echo.client

import com.example.echo.common.DefaultProperties
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

object EchoClient {

    private val SSL = System.getProperty("ssl")?.toBoolean() ?: DefaultProperties.SSL
    private val HOST = System.getProperty("host") ?: DefaultProperties.HOST
    private val PORT = System.getProperty("port")?.toInt() ?: DefaultProperties.PORT

    fun run(message: String) {

        val sslContext = if (SSL) {
            SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build()
        } else null

        val eventLoopGroup = NioEventLoopGroup()

        try {
            val bootstrap = Bootstrap()

            bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(object : ChannelInitializer<SocketChannel>() {

                    override fun initChannel(ch: SocketChannel) {

                        val channelPipeline = ch.pipeline()

                        if (sslContext != null) {
                            channelPipeline.addLast(sslContext.newHandler(ch.alloc(), HOST, PORT))
                        }

                        channelPipeline.addLast(EchoClientHandler(message))
                    }
                })

            val channelFuture = bootstrap.connect(HOST, PORT).sync()

            channelFuture.channel().closeFuture().sync()
        } finally {
            eventLoopGroup.shutdownGracefully()
        }
    }
}