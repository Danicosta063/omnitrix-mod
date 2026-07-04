import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/storage_service.dart';
import '../services/gemini_service.dart';
import '../theme/app_theme.dart';
import 'home_screen.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _pageController = PageController();
  final TextEditingController _apiKeyController = TextEditingController();
  int _currentPage = 0;
  bool _testing = false;
  String? _errorMessage;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: PageView(
                controller: _pageController,
                onPageChanged: (p) => setState(() => _currentPage = p),
                children: [
                  _buildWelcomePage(),
                  _buildFeaturesPage(),
                  _buildApiKeyPage(),
                ],
              ),
            ),
            _buildBottomNav(),
          ],
        ),
      ),
    );
  }

  Widget _buildWelcomePage() {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 100,
            height: 100,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: LinearGradient(
                colors: [AppTheme.primaryRed, AppTheme.darkRed],
              ),
            ),
            child: const Icon(Icons.shield, size: 60, color: Colors.white),
          ),
          const SizedBox(height: 30),
          const Text(
            'Bem-vindo, Summoner!',
            style: TextStyle(
              fontSize: 28,
              fontWeight: FontWeight.bold,
              color: AppTheme.gold,
            ),
          ),
          const SizedBox(height: 20),
          const Text(
            'Eu sou seu Coach AI pessoal do MCOC.\n\n'
            'Vou te guiar do zero ao Valiant:\n'
            '• Recomendo quais champs upar\n'
            '• Digo qual missão fazer agora\n'
            '• Ajudo com nodes difíceis\n'
            '• Sei tudo do seu roster\n\n'
            'Bora começar essa jornada!',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 16, color: AppTheme.textPrimary, height: 1.5),
          ),
        ],
      ),
    );
  }

  Widget _buildFeaturesPage() {
    final features = [
      ['💬', 'Chat inteligente', 'Converse comigo sobre qualquer coisa do jogo'],
      ['⚔️', 'Seu roster', 'Cadastre seus champs — vou usar pra recomendações'],
      ['🎯', 'Node Helper', 'Descreva um node e eu digo qual champ usar'],
      ['📈', 'Plano de progressão', 'Roadmap personalizado até o end-game'],
      ['📚', 'Database', 'Consulta rápida de campeões meta'],
    ];

    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'O que você pode fazer',
            style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: AppTheme.gold),
          ),
          const SizedBox(height: 30),
          ...features.map((f) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 12),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(f[0], style: const TextStyle(fontSize: 32)),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(f[1],
                              style: const TextStyle(
                                  fontSize: 18, fontWeight: FontWeight.bold, color: AppTheme.textPrimary)),
                          const SizedBox(height: 4),
                          Text(f[2], style: const TextStyle(color: AppTheme.textSecondary, fontSize: 14)),
                        ],
                      ),
                    ),
                  ],
                ),
              )),
        ],
      ),
    );
  }

  Widget _buildApiKeyPage() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 20),
          const Text('Configure sua IA',
              style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: AppTheme.gold)),
          const SizedBox(height: 12),
          const Text(
            'Preciso de uma chave grátis do Google Gemini para funcionar. '
            'É grátis, leva 30 segundos.',
            style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, height: 1.4),
          ),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppTheme.cardBg,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppTheme.gold.withOpacity(0.3)),
            ),
            child: const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('📝 Como conseguir sua chave:',
                    style: TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold, fontSize: 16)),
                SizedBox(height: 12),
                Text('1. Acesse: aistudio.google.com/apikey',
                    style: TextStyle(color: AppTheme.textPrimary)),
                Text('2. Faça login com sua conta Google', style: TextStyle(color: AppTheme.textPrimary)),
                Text('3. Clique em "Create API Key"', style: TextStyle(color: AppTheme.textPrimary)),
                Text('4. Copie a chave e cole abaixo', style: TextStyle(color: AppTheme.textPrimary)),
              ],
            ),
          ),
          const SizedBox(height: 20),
          TextField(
            controller: _apiKeyController,
            decoration: const InputDecoration(
              labelText: 'Cole sua API Key aqui',
              hintText: 'AIzaSy...',
              prefixIcon: Icon(Icons.vpn_key, color: AppTheme.gold),
            ),
            obscureText: true,
            style: const TextStyle(color: AppTheme.textPrimary),
          ),
          if (_errorMessage != null) ...[
            const SizedBox(height: 12),
            Text(_errorMessage!, style: const TextStyle(color: Colors.redAccent)),
          ],
          const SizedBox(height: 20),
          const Text(
            '🔒 Sua chave fica salva SÓ NO SEU CELULAR. Nunca enviamos pra nenhum servidor nosso.',
            style: TextStyle(color: AppTheme.textSecondary, fontSize: 12, fontStyle: FontStyle.italic),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomNav() {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Row(
        children: [
          // Indicadores
          Row(
            children: List.generate(3, (i) {
              return Container(
                width: 10,
                height: 10,
                margin: const EdgeInsets.only(right: 6),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: i == _currentPage ? AppTheme.gold : AppTheme.surfaceBg,
                ),
              );
            }),
          ),
          const Spacer(),
          ElevatedButton(
            onPressed: _testing ? null : _handleNext,
            child: _testing
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : Text(_currentPage == 2 ? 'COMEÇAR' : 'PRÓXIMO'),
          ),
        ],
      ),
    );
  }

  Future<void> _handleNext() async {
    if (_currentPage < 2) {
      _pageController.nextPage(
          duration: const Duration(milliseconds: 300), curve: Curves.easeInOut);
      return;
    }

    // Última tela: valida e salva API key
    final key = _apiKeyController.text.trim();
    if (key.isEmpty) {
      setState(() => _errorMessage = 'Cola sua API key antes de continuar');
      return;
    }

    setState(() {
      _testing = true;
      _errorMessage = null;
    });

    final ok = await GeminiService.testApiKey(key);

    if (!ok) {
      setState(() {
        _testing = false;
        _errorMessage = 'Chave inválida ou sem acesso ao Gemini. Confere e tenta de novo.';
      });
      return;
    }

    await StorageService.setApiKey(key);
    await StorageService.setFirstRunDone();

    if (!mounted) return;
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(builder: (_) => const HomeScreen()),
    );
  }
}
