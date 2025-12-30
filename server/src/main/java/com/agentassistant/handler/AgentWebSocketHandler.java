package com.agentassistant.handler;

import com.agentassistant.proto.WebsocketMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentWebSocketHandler extends BinaryWebSocketHandler {

  private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

  // Map to store pending requests waiting for a response from the client
  // Key: Request ID, Value: CompletableFuture
  private final ConcurrentHashMap<String, CompletableFuture<WebsocketMessage>> pendingRequests = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    sessions.put(session.getId(), session);
    System.err.println("Client connected: " + session.getId());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    sessions.remove(session.getId());
    System.err.println("Client disconnected: " + session.getId());
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
    ByteBuffer payload = message.getPayload();
    WebsocketMessage wsMessage = WebsocketMessage.parseFrom(payload);

    // Check if this is a response to a pending request
    if (wsMessage.getCmd().endsWith("Reply")) {
      // Extract ID from the response.
      // AskQuestionResponse has ID, TaskFinishResponse has ID.
      String requestId = null;
      if (wsMessage.hasAskQuestionResponse()) {
        requestId = wsMessage.getAskQuestionResponse().getID();
      } else if (wsMessage.hasTaskFinishResponse()) {
        requestId = wsMessage.getTaskFinishResponse().getID();
      }

      if (requestId != null && pendingRequests.containsKey(requestId)) {
        pendingRequests.get(requestId).complete(wsMessage);
        pendingRequests.remove(requestId);
      }
    }
  }

  public void broadcastMessage(WebsocketMessage message) {
    for (WebSocketSession session : sessions.values()) {
      if (session.isOpen()) {
        try {
          session.sendMessage(new BinaryMessage(message.toByteArray()));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public CompletableFuture<WebsocketMessage> sendRequest(WebsocketMessage message, String requestId) {
    CompletableFuture<WebsocketMessage> future = new CompletableFuture<>();
    pendingRequests.put(requestId, future);
    broadcastMessage(message);
    return future;
  }
}
