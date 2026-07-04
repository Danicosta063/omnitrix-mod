import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/chat_message.dart';
import 'storage_service.dart';
import 'coach_context_builder.dart';

class GeminiService {
  // Gemini 2.0 Flash - grátis e rápido
  static const String _model = 'gemini-2.0-flash';
  static const String _baseUrl =
      'https://generativelanguage.googleapis.com/v1beta/models';

  /// Envia uma mensagem para o Coach AI e retorna a resposta
  static Future<String> sendMessage({
    required String userMessage,
    required List<ChatMessage> history,
  }) async {
    final apiKey = StorageService.apiKey;
    if (apiKey == null || apiKey.isEmpty) {
      throw Exception(
          'API Key do Gemini não configurada. Vá em Configurações e adicione sua chave grátis.');
    }

    // Construir o contexto completo (roster + perfil + base de conhecimento)
    final systemContext = CoachContextBuilder.buildSystemPrompt();

    // Montar histórico da conversa (últimas 10 mensagens)
    final recentHistory = history.length > 10
        ? history.sublist(history.length - 10)
        : history;

    final contents = <Map<String, dynamic>>[];

    // Adicionar o contexto do sistema como primeira mensagem
    contents.add({
      'role': 'user',
      'parts': [
        {'text': systemContext}
      ],
    });
    contents.add({
      'role': 'model',
      'parts': [
        {
          'text':
              'Entendido! Sou seu Coach pessoal de MCOC. Conheço seu roster, seu progresso e todo o meta do jogo. Como posso te ajudar hoje, Summoner?'
        }
      ],
    });

    // Adicionar histórico
    for (final msg in recentHistory) {
      contents.add({
        'role': msg.isUser ? 'user' : 'model',
        'parts': [
          {'text': msg.content}
        ],
      });
    }

    // Mensagem atual
    contents.add({
      'role': 'user',
      'parts': [
        {'text': userMessage}
      ],
    });

    final url = Uri.parse('$_baseUrl/$_model:generateContent?key=$apiKey');

    try {
      final response = await http
          .post(
            url,
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'contents': contents,
              'generationConfig': {
                'temperature': 0.7,
                'topK': 40,
                'topP': 0.95,
                'maxOutputTokens': 2048,
              },
              'safetySettings': [
                {
                  'category': 'HARM_CATEGORY_HARASSMENT',
                  'threshold': 'BLOCK_ONLY_HIGH'
                },
                {
                  'category': 'HARM_CATEGORY_HATE_SPEECH',
                  'threshold': 'BLOCK_ONLY_HIGH'
                },
              ],
            }),
          )
          .timeout(const Duration(seconds: 60));

      if (response.statusCode == 200) {
        final data = jsonDecode(utf8.decode(response.bodyBytes));
        final candidates = data['candidates'] as List?;
        if (candidates == null || candidates.isEmpty) {
          return '⚠️ Não consegui gerar uma resposta. Tenta reformular a pergunta.';
        }
        final parts = candidates[0]['content']?['parts'] as List?;
        if (parts == null || parts.isEmpty) {
          return '⚠️ Resposta vazia da IA.';
        }
        return parts[0]['text'] ?? '';
      } else {
        final err = jsonDecode(response.body);
        final msg = err['error']?['message'] ?? response.body;
        throw Exception('Erro Gemini (${response.statusCode}): $msg');
      }
    } catch (e) {
      throw Exception('Erro ao consultar o Coach AI: $e');
    }
  }

  /// Testa se a API key é válida
  static Future<bool> testApiKey(String apiKey) async {
    final url = Uri.parse('$_baseUrl/$_model:generateContent?key=$apiKey');
    try {
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'contents': [
            {
              'role': 'user',
              'parts': [
                {'text': 'Diga apenas "ok"'}
              ]
            }
          ],
        }),
      ).timeout(const Duration(seconds: 15));
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }
}
