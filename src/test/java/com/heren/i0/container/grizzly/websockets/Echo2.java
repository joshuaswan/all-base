package com.heren.i0.container.grizzly.websockets;

import com.heren.i0.container.grizzly.internal.tryus.WebsocketConfigurator;

import javax.inject.Inject;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;


@ServerEndpoint(value="/echo2", configurator = WebsocketConfigurator.class)
public class Echo2 {

    @Inject
    private Service service;


    public Echo2(){
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("chat session.getId() = " + session.getId());
    }

    @OnMessage
    public String echo(String message) {
        String message1 = this.service.message();
        return message1 + " " + message;
    }

}