import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/websocket_service.dart';
import '../proto/agentassist.pb.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final TextEditingController _urlController = TextEditingController(text: "ws://localhost:8080/ws");
  final TextEditingController _tokenController = TextEditingController(text: "test-token");

  @override
  void initState() {
    super.initState();
    // Listen for incoming requests
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<WebSocketService>().requestStream.listen((message) {
        if (message.hasAskQuestionRequest()) {
          _showAskQuestionDialog(message.askQuestionRequest);
        } else if (message.hasTaskFinishRequest()) {
          _showTaskFinishDialog(message.taskFinishRequest);
        }
      });
    });
  }

  void _showAskQuestionDialog(AskQuestionRequest request) {
    final TextEditingController answerController = TextEditingController();
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text("Agent Question"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(request.request.question),
            const SizedBox(height: 16),
            TextField(
              controller: answerController,
              decoration: const InputDecoration(
                labelText: "Your Answer",
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () {
              // Send reply
              final reply = WebsocketMessage()
                ..cmd = "AskQuestionReply"
                ..askQuestionResponse = (AskQuestionResponse()
                  ..iD = request.iD
                  ..contents.add(McpResultContent()
                    ..type = 1 // Text
                    ..text = (TextContent()
                      ..type = "text"
                      ..text = answerController.text
                    )
                  )
                );
              context.read<WebSocketService>().sendReply(reply);
              Navigator.of(context).pop();
            },
            child: const Text("Submit"),
          ),
        ],
      ),
    );
  }

  void _showTaskFinishDialog(TaskFinishRequest request) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text("Task Finished"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("The agent has finished the task:"),
            const SizedBox(height: 8),
            Text(request.request.summary, style: const TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () {
              // Send reply
              final reply = WebsocketMessage()
                ..cmd = "TaskFinishReply"
                ..taskFinishResponse = (TaskFinishResponse()
                  ..iD = request.iD
                  ..contents.add(McpResultContent()
                    ..type = 1 // Text
                    ..text = (TextContent()
                      ..type = "text"
                      ..text = "Acknowledged"
                    )
                  )
                );
              context.read<WebSocketService>().sendReply(reply);
              Navigator.of(context).pop();
            },
            child: const Text("OK"),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final wsService = context.watch<WebSocketService>();

    return Scaffold(
      appBar: AppBar(
        title: const Text("Agent Assistant Client"),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    TextField(
                      controller: _urlController,
                      decoration: const InputDecoration(
                        labelText: "Server URL",
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.link),
                      ),
                      enabled: !wsService.isConnected,
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _tokenController,
                      decoration: const InputDecoration(
                        labelText: "Token",
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.key),
                      ),
                      obscureText: true,
                      enabled: !wsService.isConnected,
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: wsService.isConnected
                            ? wsService.disconnect
                            : () => wsService.connect(_urlController.text, _tokenController.text),
                        icon: Icon(wsService.isConnected ? Icons.link_off : Icons.link),
                        label: Text(wsService.isConnected ? "Disconnect" : "Connect"),
                        style: FilledButton.styleFrom(
                          backgroundColor: wsService.isConnected ? Colors.red : null,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),
            Text("Status: ${wsService.status}", style: Theme.of(context).textTheme.titleMedium),
            const Divider(),
            const Expanded(
              child: Center(
                child: Text(
                  "Waiting for agent requests...",
                  style: TextStyle(color: Colors.grey),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
