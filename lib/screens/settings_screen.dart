import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/storage_service.dart';
import '../services/gemini_service.dart';
import '../theme/app_theme.dart';
import 'onboarding_screen.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _apiKeyCtrl = TextEditingController();
  bool _obscure = true;
  bool _testing = false;

  @override
  void initState() {
    super.initState();
    _apiKeyCtrl.text = StorageService.apiKey ?? '';
  }

  Future<void> _saveApiKey() async {
    final key = _apiKeyCtrl.text.trim();
    if (key.isEmpty) return;
    setState(() => _testing = true);
    final ok = await GeminiService.testApiKey(key);
    setState(() => _testing = false);
    if (ok) {
      await StorageService.setApiKey(key);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('API Key salva e validada! ✅'), backgroundColor: Colors.green),
        );
      }
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Chave inválida'), backgroundColor: AppTheme.primaryRed),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('CONFIGURAÇÕES')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const Text('IA COACH',
              style: TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold, letterSpacing: 2)),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Google Gemini API Key',
                      style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 4),
                  const Text(
                      'Grátis em aistudio.google.com/apikey',
                      style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _apiKeyCtrl,
                    obscureText: _obscure,
                    decoration: InputDecoration(
                      hintText: 'AIzaSy...',
                      suffixIcon: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          IconButton(
                            icon: Icon(_obscure ? Icons.visibility : Icons.visibility_off),
                            onPressed: () => setState(() => _obscure = !_obscure),
                          ),
                          IconButton(
                            icon: const Icon(Icons.paste),
                            onPressed: () async {
                              final data = await Clipboard.getData('text/plain');
                              if (data?.text != null) {
                                _apiKeyCtrl.text = data!.text!.trim();
                              }
                            },
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: _testing ? null : _saveApiKey,
                      child: _testing
                          ? const SizedBox(
                              width: 20, height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                          : const Text('TESTAR E SALVAR'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),
          const Text('DADOS',
              style: TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold, letterSpacing: 2)),
          const SizedBox(height: 12),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.delete_sweep, color: Colors.orange),
                  title: const Text('Limpar histórico do chat'),
                  onTap: () async {
                    await StorageService.clearChatHistory();
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Chat limpo')),
                      );
                    }
                  },
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.refresh, color: AppTheme.primaryRed),
                  title: const Text('Refazer onboarding'),
                  onTap: () {
                    Navigator.pushReplacement(context,
                        MaterialPageRoute(builder: (_) => const OnboardingScreen()));
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: 30),
          const Center(
            child: Column(
              children: [
                Text('MCOC Coach AI', style: TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold)),
                SizedBox(height: 4),
                Text('v1.0.0', style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                SizedBox(height: 12),
                Text('App não-oficial. Não afiliado com Kabam ou Marvel.',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                    textAlign: TextAlign.center),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
