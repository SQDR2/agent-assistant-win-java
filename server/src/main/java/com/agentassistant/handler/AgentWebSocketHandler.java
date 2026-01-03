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

  private final java.util.concurrent.CompletableFuture<Void> connectionFuture = new java.util.concurrent.CompletableFuture<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    sessions.put(session.getId(), session);
    System.err.println("Client connected: " + session.getId());
    connectionFuture.complete(null);
  }

  public void waitForConnection(long timeout, java.util.concurrent.TimeUnit unit) {
    if (!sessions.isEmpty()) return;
    try {
      System.err.println("Waiting for client connection...");
      connectionFuture.get(timeout, unit);
      System.err.println("Client connected, proceeding.");
    } catch (Exception e) {
      System.err.println("Timeout waiting for client connection: " + e.getMessage());
    }
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
          System.err.println("Sending WebSocket message to session: " + session.getId() + ", Cmd: " + message.getCmd());
          session.sendMessage(new BinaryMessage(message.toByteArray()));
          System.err.println("Sent successfully.");
        } catch (IOException e) {
          System.err.println("Error sending to session " + session.getId() + ": " + e.getMessage());
          e.printStackTrace();
        }
      } else {
        System.err.println("Session " + session.getId() + " is closed, skipping.");
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
