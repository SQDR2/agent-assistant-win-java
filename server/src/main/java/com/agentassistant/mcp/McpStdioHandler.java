package com.agentassistant.mcp;

import com.agentassistant.handler.AgentWebSocketHandler;
import com.agentassistant.proto.*;
import com.google.gson.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class McpStdioHandler implements CommandLineRunner {

  private final AgentWebSocketHandler webSocketHandler;
  private final Gson gson = new Gson();

  public McpStdioHandler(AgentWebSocketHandler webSocketHandler) {
    this.webSocketHandler = webSocketHandler;
  }

  @Override
  public void run(String... args) throws Exception {
    new Thread(this::readStdio).start();
  }

  private void readStdio() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      while ((line = reader.readLine()) != null) {
        try {
          handleJsonRpc(line);
        } catch (Exception e) {
          System.err.println("Error handling JSON-RPC: " + e.getMessage());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleJsonRpc(String line) {
    try {
      JsonObject request = JsonParser.parseString(line).getAsJsonObject();

      if (request.has("method")) {
        String method = request.get("method").getAsString();
        String id = request.has("id") ? request.get("id").getAsString() : null;

        if ("initialize".equals(method)) {
          handleInitialize(id);
        } else if ("tools/list".equals(method)) {
          handleToolsList(id);
        } else if ("tools/call".equals(method)) {
          handleToolsCall(id, request.getAsJsonObject("params"));
        } else if ("notifications/initialized".equals(method)) {
          // Ignore
        } else {
          // Ignore other methods
        }
      }
    } catch (Exception e) {
      // Invalid JSON
    }
  }

  private void handleInitialize(String id) {
    JsonObject response = new JsonObject();
    response.addProperty("jsonrpc", "2.0");
    response.addProperty("id", id);

    JsonObject result = new JsonObject();
    result.addProperty("protocolVersion", "2024-11-05");

    JsonObject serverInfo = new JsonObject();
    serverInfo.addProperty("name", "agent-assistant-java");
    serverInfo.addProperty("version", "1.0.0");
    result.add("server", serverInfo);

    JsonObject capabilities = new JsonObject();
    capabilities.add("tools", new JsonObject());
    result.add("capabilities", capabilities);

    response.add("result", result);
    System.out.println(gson.toJson(response));
  }

  private void handleToolsList(String id) {
    JsonObject response = new JsonObject();
    response.addProperty("jsonrpc", "2.0");
    response.addProperty("id", id);

    JsonObject result = new JsonObject();
    JsonArray tools = new JsonArray();

    // ask_question tool
    JsonObject askTool = new JsonObject();
    askTool.addProperty("name", "ask_question");
    askTool.addProperty("description", "Ask the user a question via the Flutter UI.");
    JsonObject askSchema = new JsonObject();
    askSchema.addProperty("type", "object");
    JsonObject askProps = new JsonObject();
    JsonObject qProp = new JsonObject();
    qProp.addProperty("type", "string");
    qProp.addProperty("description", "The question to ask.");
    askProps.add("question", qProp);
    askSchema.add("properties", askProps);
    JsonArray askRequired = new JsonArray();
    askRequired.add("question");
    askSchema.add("required", askRequired);
    askTool.add("inputSchema", askSchema);
    tools.add(askTool);

    // task_finish tool
    JsonObject finishTool = new JsonObject();
    finishTool.addProperty("name", "task_finish");
    finishTool.addProperty("description", "Notify the user that the task is finished.");
    JsonObject finishSchema = new JsonObject();
    finishSchema.addProperty("type", "object");
    JsonObject finishProps = new JsonObject();
    JsonObject sProp = new JsonObject();
    sProp.addProperty("type", "string");
    sProp.addProperty("description", "Summary of the task.");
    finishProps.add("summary", sProp);
    finishSchema.add("properties", finishProps);
    JsonArray finishRequired = new JsonArray();
    finishRequired.add("summary");
    finishSchema.add("required", finishRequired);
    finishTool.add("inputSchema", finishSchema);
    tools.add(finishTool);

    result.add("tools", tools);
    response.add("result", result);
    System.out.println(gson.toJson(response));
  }

  private void handleToolsCall(String id, JsonObject params) {
    String name = params.get("name").getAsString();
    JsonObject args = params.getAsJsonObject("arguments");

    String requestId = UUID.randomUUID().toString();
    WebsocketMessage.Builder msgBuilder = WebsocketMessage.newBuilder();

    if ("ask_question".equals(name)) {
      String question = args.get("question").getAsString();
      msgBuilder.setCmd("AskQuestion");
      msgBuilder.setAskQuestionRequest(
          AskQuestionRequest.newBuilder()
              .setID(requestId)
              .setRequest(
                  McpAskQuestionRequest.newBuilder()
                      .setQuestion(question)
                      .build())
              .build());
    } else if ("task_finish".equals(name)) {
      String summary = args.get("summary").getAsString();
      msgBuilder.setCmd("TaskFinish");
      msgBuilder.setTaskFinishRequest(
          TaskFinishRequest.newBuilder()
              .setID(requestId)
              .setRequest(
                  McpTaskFinishRequest.newBuilder()
                      .setSummary(summary)
                      .build())
              .build());
    } else {
      // Unknown tool
      return;
    }

    // Send to WebSocket and wait
    CompletableFuture<WebsocketMessage> future = webSocketHandler.sendRequest(msgBuilder.build(), requestId);

    try {
      WebsocketMessage responseMsg = future.get(600, TimeUnit.SECONDS); // Long timeout for user interaction

      JsonObject response = new JsonObject();
      response.addProperty("jsonrpc", "2.0");
      response.addProperty("id", id);

      JsonObject result = new JsonObject();
      JsonArray content = new JsonArray();

      if (responseMsg.hasAskQuestionResponse()) {
        for (McpResultContent c : responseMsg.getAskQuestionResponse().getContentsList()) {
          if (c.getType() == 1) { // Text
            JsonObject textContent = new JsonObject();
            textContent.addProperty("type", "text");
            textContent.addProperty("text", c.getText().getText());
            content.add(textContent);
          }
        }
      } else if (responseMsg.hasTaskFinishResponse()) {
        for (McpResultContent c : responseMsg.getTaskFinishResponse().getContentsList()) {
          if (c.getType() == 1) { // Text
            JsonObject textContent = new JsonObject();
            textContent.addProperty("type", "text");
            textContent.addProperty("text", c.getText().getText());
            content.add(textContent);
          }
        }
      }

      result.add("content", content);
      response.add("result", result);
      System.out.println(gson.toJson(response));

    } catch (Exception e) {
      // Timeout or error
      JsonObject response = new JsonObject();
      response.addProperty("jsonrpc", "2.0");
      response.addProperty("id", id);
      JsonObject error = new JsonObject();
      error.addProperty("code", -32603);
      error.addProperty("message", "Internal error: " + e.getMessage());
      response.add("error", error);
      System.out.println(gson.toJson(response));
    }
  }
}
