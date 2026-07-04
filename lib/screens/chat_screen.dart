import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:uuid/uuid.dart';
import '../models/chat_message.dart';
import '../services/gemini_service.dart';
import '../services/storage_service.dart';
import '../theme/app_theme.dart';

class ChatScreen extends StatefulWidget {
  const ChatScreen({super.key});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final _uuid = const Uuid();
  List<ChatMessage> _messages = [];
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _messages = StorageService.getChatHistory();
    if (_messages.isEmpty) {
      _messages.add(ChatMessage(
        id: _uuid.v4(),
        content: '👋 Fala, Summoner! Eu sou seu **Coach AI** do MCOC.\n\n'
            'Posso te ajudar com:\n'
            '• Recomendação de rank ups\n'
            '• Qual missão fazer agora\n'
            '• Node helper (qual champ usar)\n'
            '• Plano de progressão\n'
            '• Análise do seu roster\n\n'
            'O que você quer saber hoje? 🎯',
        isUser: false,
      ));
    }
  }

  Future<void> _sendMessage() async {
    final text = _controller.text.trim();
    if (text.isEmpty || _loading) return;

    if (!StorageService.hasApiKey) {
      _showApiKeyMissing();
      return;
    }

    final userMsg = ChatMessage(id: _uuid.v4(), content: text, isUser: true);

    setState(() {
      _messages.add(userMsg);
      _loading = true;
    });
    _controller.clear();
    _scrollToBottom();

    try {
      final response = await GeminiService.sendMessage(
        userMessage: text,
        history: _messages.where((m) => m.id != userMsg.id).toList(),
      );

      final aiMsg = ChatMessage(id: _uuid.v4(), content: response, isUser: false);
      setState(() {
        _messages.add(aiMsg);
        _loading = false;
      });
      await StorageService.saveChatHistory(_messages);
      _scrollToBottom();
    } catch (e) {
      setState(() {
        _messages.add(ChatMessage(
          id: _uuid.v4(),
          content: '❌ Erro: ${e.toString().replaceAll("Exception: ", "")}',
          isUser: false,
        ));
        _loading = false;
      });
      _scrollToBottom();
    }
  }

  void _scrollToBottom() {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _showApiKeyMissing() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Configure sua API Key do Gemini em Configurações!'),
        backgroundColor: AppTheme.primaryRed,
      ),
    );
  }

  Future<void> _clearChat() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Limpar conversa?'),
        content: const Text('Isso apaga todo o histórico do chat.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('LIMPAR')),
        ],
      ),
    );
    if (confirm == true) {
      await StorageService.clearChatHistory();
      setState(() {
        _messages = [
          ChatMessage(
            id: _uuid.v4(),
            content: 'Conversa limpa! Como posso te ajudar? 🎯',
            isUser: false,
          )
        ];
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('COACH AI'),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_sweep),
            onPressed: _clearChat,
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.all(16),
              itemCount: _messages.length + (_loading ? 1 : 0),
              itemBuilder: (context, i) {
                if (i == _messages.length && _loading) {
                  return _buildTypingIndicator();
                }
                return _buildMessageBubble(_messages[i]);
              },
            ),
          ),
          _buildSuggestions(),
          _buildInputBar(),
        ],
      ),
    );
  }

  Widget _buildMessageBubble(ChatMessage msg) {
    final isUser = msg.isUser;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        mainAxisAlignment: isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!isUser) ...[
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(colors: [AppTheme.primaryRed, AppTheme.darkRed]),
              ),
              child: const Icon(Icons.shield, size: 20, color: Colors.white),
            ),
            const SizedBox(width: 8),
          ],
          Flexible(
            child: GestureDetector(
              onLongPress: () {
                Clipboard.setData(ClipboardData(text: msg.content));
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Copiado!'), duration: Duration(seconds: 1)),
                );
              },
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                decoration: BoxDecoration(
                  color: isUser ? AppTheme.primaryRed : AppTheme.cardBg,
                  borderRadius: BorderRadius.only(
                    topLeft: const Radius.circular(16),
                    topRight: const Radius.circular(16),
                    bottomLeft: Radius.circular(isUser ? 16 : 4),
                    bottomRight: Radius.circular(isUser ? 4 : 16),
                  ),
                  border: !isUser
                      ? Border.all(color: AppTheme.gold.withOpacity(0.2))
                      : null,
                ),
                child: isUser
                    ? Text(msg.content, style: const TextStyle(color: Colors.white, fontSize: 15))
                    : MarkdownBody(
                        data: msg.content,
                        styleSheet: MarkdownStyleSheet(
                          p: const TextStyle(color: AppTheme.textPrimary, fontSize: 15, height: 1.4),
                          strong: const TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold),
                          listBullet: const TextStyle(color: AppTheme.textPrimary),
                          h1: const TextStyle(color: AppTheme.gold, fontSize: 18, fontWeight: FontWeight.bold),
                          h2: const TextStyle(color: AppTheme.gold, fontSize: 16, fontWeight: FontWeight.bold),
                          h3: const TextStyle(color: AppTheme.gold, fontSize: 15, fontWeight: FontWeight.bold),
                          code: TextStyle(
                              backgroundColor: AppTheme.surfaceBg,
                              color: AppTheme.gold,
                              fontFamily: 'monospace'),
                        ),
                      ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTypingIndicator() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: LinearGradient(colors: [AppTheme.primaryRed, AppTheme.darkRed]),
            ),
            child: const Icon(Icons.shield, size: 20, color: Colors.white),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: AppTheme.cardBg,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppTheme.gold.withOpacity(0.2)),
            ),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.gold)),
                SizedBox(width: 12),
                Text('Analisando...', style: TextStyle(color: AppTheme.textSecondary)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSuggestions() {
    if (_messages.length > 1) return const SizedBox.shrink();
    final suggestions = [
      'O que fazer agora?',
      'Analisa meu roster',
      'Melhor rank up?',
      'Plano até Thronebreaker',
    ];
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        children: suggestions.map((s) {
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ActionChip(
              label: Text(s),
              onPressed: () {
                _controller.text = s;
                _sendMessage();
              },
              backgroundColor: AppTheme.cardBg,
              side: BorderSide(color: AppTheme.gold.withOpacity(0.4)),
              labelStyle: const TextStyle(color: AppTheme.gold, fontSize: 12),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildInputBar() {
    return Container(
      padding: EdgeInsets.only(
        left: 12,
        right: 12,
        top: 8,
        bottom: MediaQuery.of(context).viewInsets.bottom > 0 ? 8 : 16,
      ),
      decoration: BoxDecoration(
        color: AppTheme.cardBg,
        border: Border(top: BorderSide(color: AppTheme.gold.withOpacity(0.2))),
      ),
      child: SafeArea(
        child: Row(
          children: [
            Expanded(
              child: TextField(
                controller: _controller,
                maxLines: 4,
                minLines: 1,
                textCapitalization: TextCapitalization.sentences,
                decoration: InputDecoration(
                  hintText: 'Pergunta pro Coach...',
                  hintStyle: const TextStyle(color: AppTheme.textSecondary),
                  fillColor: AppTheme.surfaceBg,
                  contentPadding:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(24),
                    borderSide: BorderSide.none,
                  ),
                ),
                onSubmitted: (_) => _sendMessage(),
              ),
            ),
            const SizedBox(width: 8),
            CircleAvatar(
              radius: 24,
              backgroundColor: _loading ? AppTheme.surfaceBg : AppTheme.primaryRed,
              child: IconButton(
                icon: Icon(Icons.send, color: _loading ? AppTheme.textSecondary : Colors.white),
                onPressed: _loading ? null : _sendMessage,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
