import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import '../services/gemini_service.dart';
import '../services/storage_service.dart';
import '../theme/app_theme.dart';

class NodeHelperScreen extends StatefulWidget {
  const NodeHelperScreen({super.key});

  @override
  State<NodeHelperScreen> createState() => _NodeHelperScreenState();
}

class _NodeHelperScreenState extends State<NodeHelperScreen> {
  final _nodesCtrl = TextEditingController();
  final _bossCtrl = TextEditingController();
  bool _loading = false;
  String? _result;
  String? _error;

  Future<void> _analyze() async {
    if (_nodesCtrl.text.trim().isEmpty && _bossCtrl.text.trim().isEmpty) {
      setState(() => _error = 'Descreva pelo menos o node ou o boss');
      return;
    }
    if (!StorageService.hasApiKey) {
      setState(() => _error = 'Configure sua API Key em Configurações');
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
      _result = null;
    });

    final prompt = '''
Analisa esta missão e me diga QUAL CAMPEÃO DO MEU ROSTER usar (só recomende champs que EU TENHO):

**Boss / Champion inimigo:** ${_bossCtrl.text.trim().isEmpty ? "(não especificado)" : _bossCtrl.text.trim()}
**Nodes / Modificadores:** ${_nodesCtrl.text.trim().isEmpty ? "(sem nodes específicos)" : _nodesCtrl.text.trim()}

Formato da resposta:
1. **Melhor escolha do meu roster** (nome do champ + rank + por que funciona)
2. **2ª opção** (backup)
3. **3ª opção** (se necessário)
4. **Estratégia de luta** (dicas rápidas: parry, intercept, quando usar special)
5. **Boosts recomendados** (se aplicável)

Se o meu roster não tem nenhum champ ideal, sugere o "menos ruim" e diz o que evitar.
''';

    try {
      final response = await GeminiService.sendMessage(
        userMessage: prompt,
        history: [],
      );
      setState(() {
        _result = response;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll("Exception: ", "");
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('NODE HELPER')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppTheme.cardBg,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: Colors.orange.withOpacity(0.3)),
            ),
            child: const Row(
              children: [
                Icon(Icons.gps_fixed, color: Colors.orange, size: 30),
                SizedBox(width: 12),
                Expanded(
                  child: Text(
                    'Descreva o node/boss e eu digo qual champ DO SEU ROSTER usar',
                    style: TextStyle(color: AppTheme.textPrimary, fontSize: 14),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          TextField(
            controller: _bossCtrl,
            decoration: const InputDecoration(
              labelText: 'Boss / Champion inimigo',
              hintText: 'Ex: Serpent, Photon, Wolverine',
              prefixIcon: Icon(Icons.person_off),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _nodesCtrl,
            maxLines: 5,
            decoration: const InputDecoration(
              labelText: 'Nodes / Modificadores',
              hintText: 'Ex: Buffet, Encroaching Stun, MD, Aggression, Do You Bleed?',
              alignLabelWithHint: true,
              prefixIcon: Padding(
                padding: EdgeInsets.only(bottom: 60),
                child: Icon(Icons.list_alt),
              ),
            ),
          ),
          const SizedBox(height: 20),
          ElevatedButton.icon(
            onPressed: _loading ? null : _analyze,
            icon: _loading
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : const Icon(Icons.search),
            label: Text(_loading ? 'ANALISANDO...' : 'ANALISAR COM O COACH'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.orange,
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.red.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.red),
              ),
              child: Text(_error!, style: const TextStyle(color: Colors.redAccent)),
            ),
          ],
          if (_result != null) ...[
            const SizedBox(height: 20),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppTheme.cardBg,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.gold.withOpacity(0.4)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.shield, color: AppTheme.gold),
                      SizedBox(width: 8),
                      Text('RECOMENDAÇÃO DO COACH',
                          style: TextStyle(
                              color: AppTheme.gold,
                              fontWeight: FontWeight.bold,
                              letterSpacing: 1)),
                    ],
                  ),
                  const SizedBox(height: 12),
                  MarkdownBody(
                    data: _result!,
                    styleSheet: MarkdownStyleSheet(
                      p: const TextStyle(color: AppTheme.textPrimary, fontSize: 14, height: 1.5),
                      strong: const TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
