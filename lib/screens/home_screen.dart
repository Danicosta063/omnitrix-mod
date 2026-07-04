import 'package:flutter/material.dart';
import '../services/storage_service.dart';
import '../theme/app_theme.dart';
import 'chat_screen.dart';
import 'roster_screen.dart';
import 'profile_screen.dart';
import 'node_helper_screen.dart';
import 'settings_screen.dart';
import 'database_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  Widget build(BuildContext context) {
    final profile = StorageService.getProfile();
    final roster = StorageService.getRoster();

    return Scaffold(
      appBar: AppBar(
        title: const Text('MCOC COACH AI'),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () async {
              await Navigator.push(context,
                  MaterialPageRoute(builder: (_) => const SettingsScreen()));
              setState(() {});
            },
          ),
        ],
      ),
      body: RefreshIndicator(
        color: AppTheme.gold,
        onRefresh: () async => setState(() {}),
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _buildProfileCard(profile, roster.length),
            const SizedBox(height: 20),
            _buildMainAction(context),
            const SizedBox(height: 24),
            const Text('FERRAMENTAS',
                style: TextStyle(
                    color: AppTheme.gold,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 2)),
            const SizedBox(height: 12),
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 12,
              crossAxisSpacing: 12,
              childAspectRatio: 1.1,
              children: [
                _buildTool(context,
                    icon: Icons.shield_moon,
                    label: 'Meu Roster',
                    subtitle: '${roster.length} champs',
                    color: AppTheme.primaryRed, onTap: () async {
                  await Navigator.push(context,
                      MaterialPageRoute(builder: (_) => const RosterScreen()));
                  setState(() {});
                }),
                _buildTool(context,
                    icon: Icons.person,
                    label: 'Perfil',
                    subtitle: profile.title,
                    color: AppTheme.gold, onTap: () async {
                  await Navigator.push(context,
                      MaterialPageRoute(builder: (_) => const ProfileScreen()));
                  setState(() {});
                }),
                _buildTool(context,
                    icon: Icons.gps_fixed,
                    label: 'Node Helper',
                    subtitle: 'Qual champ?',
                    color: Colors.orange, onTap: () {
                  Navigator.push(context,
                      MaterialPageRoute(builder: (_) => const NodeHelperScreen()));
                }),
                _buildTool(context,
                    icon: Icons.menu_book,
                    label: 'Database',
                    subtitle: 'Champs meta',
                    color: Colors.purpleAccent, onTap: () {
                  Navigator.push(context,
                      MaterialPageRoute(builder: (_) => const DatabaseScreen()));
                }),
              ],
            ),
            const SizedBox(height: 30),
            _buildQuickTips(),
          ],
        ),
      ),
    );
  }

  Widget _buildProfileCard(profile, int rosterCount) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppTheme.darkRed.withOpacity(0.6), AppTheme.cardBg],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppTheme.gold.withOpacity(0.3)),
      ),
      child: Row(
        children: [
          Container(
            width: 60,
            height: 60,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: AppTheme.gold.withOpacity(0.2),
              border: Border.all(color: AppTheme.gold, width: 2),
            ),
            child: const Icon(Icons.person, size: 30, color: AppTheme.gold),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  profile.playerName.isEmpty ? 'Summoner' : profile.playerName,
                  style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: AppTheme.textPrimary),
                ),
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: AppTheme.gold.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(profile.title.toUpperCase(),
                      style: const TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          color: AppTheme.gold,
                          letterSpacing: 1)),
                ),
                const SizedBox(height: 6),
                Text(
                  'Act ${profile.currentAct} • Prestígio ${profile.prestige} • $rosterCount champs',
                  style: const TextStyle(
                      color: AppTheme.textSecondary, fontSize: 12),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMainAction(BuildContext context) {
    return InkWell(
      onTap: () {
        Navigator.push(context,
            MaterialPageRoute(builder: (_) => const ChatScreen()));
      },
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: [AppTheme.primaryRed, AppTheme.darkRed],
          ),
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: AppTheme.primaryRed.withOpacity(0.4),
              blurRadius: 20,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.chat_bubble, color: Colors.white, size: 32),
            ),
            const SizedBox(width: 16),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('CONVERSAR COM O COACH',
                      style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                          letterSpacing: 1)),
                  SizedBox(height: 4),
                  Text('Pergunta qualquer coisa do jogo',
                      style: TextStyle(color: Colors.white70, fontSize: 13)),
                ],
              ),
            ),
            const Icon(Icons.arrow_forward, color: Colors.white),
          ],
        ),
      ),
    );
  }

  Widget _buildTool(BuildContext context,
      {required IconData icon,
      required String label,
      required String subtitle,
      required Color color,
      required VoidCallback onTap}) {
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: 40, color: color),
              const SizedBox(height: 12),
              Text(label,
                  style: const TextStyle(
                      fontWeight: FontWeight.bold, fontSize: 15)),
              const SizedBox(height: 4),
              Text(subtitle,
                  style: const TextStyle(
                      color: AppTheme.textSecondary, fontSize: 12)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildQuickTips() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.lightbulb, color: AppTheme.gold, size: 20),
                const SizedBox(width: 8),
                const Text('DICA RÁPIDA',
                    style: TextStyle(
                        color: AppTheme.gold,
                        fontWeight: FontWeight.bold,
                        letterSpacing: 1)),
              ],
            ),
            const SizedBox(height: 8),
            const Text(
              'Cadastre seu roster completo pra eu dar recomendações precisas. '
              'Quanto mais dados você me der, melhor eu te ajudo! 🎯',
              style: TextStyle(color: AppTheme.textPrimary, fontSize: 13, height: 1.4),
            ),
          ],
        ),
      ),
    );
  }
}
