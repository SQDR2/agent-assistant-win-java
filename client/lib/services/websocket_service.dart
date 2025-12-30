import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import '../proto/agentassist.pb.dart';

class WebSocketService extends ChangeNotifier {
  WebSocketChannel? _channel;
  bool _isConnected = false;
  String _status = "Disconnected";
  
  // Stream controller for incoming requests
  final _requestController = StreamController<WebsocketMessage>.broadcast();
  Stream<WebsocketMessage> get requestStream => _requestController.stream;

  bool get isConnected => _isConnected;
  String get status => _status;

  Future<void> connect(String url, String token) async {
    try {
      _status = "Connecting...";
      notifyListeners();

      // In a real app, you might pass the token in headers or as a query param
      // For this simple implementation, we'll assume the server accepts the connection
      // and we might send a login message later if needed.
      final uri = Uri.parse(url);
      _channel = WebSocketChannel.connect(uri);
      
      await _channel!.ready;
      
      _isConnected = true;
      _status = "Connected";
      notifyListeners();

      _channel!.stream.listen(
        (message) {
          if (message is List<int>) {
             try {
               final wsMessage = WebsocketMessage.fromBuffer(message);
               _handleMessage(wsMessage);
             } catch (e) {
               debugPrint("Error parsing message: $e");
             }
          }
        },
        onDone: () {
          _isConnected = false;
          _status = "Disconnected";
          notifyListeners();
        },
        onError: (error) {
          _isConnected = false;
          _status = "Error: $error";
          notifyListeners();
        },
      );
      
    } catch (e) {
      _isConnected = false;
      _status = "Connection Failed: $e";
      notifyListeners();
    }
  }

  void disconnect() {
    _channel?.sink.close();
    _isConnected = false;
    _status = "Disconnected";
    notifyListeners();
  }

  void _handleMessage(WebsocketMessage message) {
    // Dispatch message to listeners (UI)
    if (message.hasAskQuestionRequest() || message.hasTaskFinishRequest()) {
      _requestController.add(message);
    }
  }

  void sendReply(WebsocketMessage message) {
    if (_isConnected && _channel != null) {
      _channel!.sink.add(message.writeToBuffer());
    }
  }
}
