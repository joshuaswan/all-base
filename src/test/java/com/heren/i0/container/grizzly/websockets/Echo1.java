package com.heren.i0.container.grizzly.websockets;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import java.io.IOException;

@ServerEndpoint(value="/echo1")
public class Echo1 {

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("chat1 session.getId() = " + session.getId());
    }

    @OnMessage
    public void echo(String message, Session session) throws IOException {
        session.getBasicRemote().sendText(message);
    }

}