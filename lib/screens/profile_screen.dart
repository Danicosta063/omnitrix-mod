import 'package:flutter/material.dart';
import '../models/user_profile.dart';
import '../services/storage_service.dart';
import '../theme/app_theme.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  late UserProfile _profile;
  final _nameCtrl = TextEditingController();
  final _prestigeCtrl = TextEditingController();
  final _actCtrl = TextEditingController();
  final _levelCtrl = TextEditingController();
  final _allianceCtrl = TextEditingController();
  final _notesCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _profile = StorageService.getProfile();
    _nameCtrl.text = _profile.playerName;
    _prestigeCtrl.text = _profile.prestige.toString();
    _actCtrl.text = _profile.currentAct;
    _levelCtrl.text = _profile.summonerLevel.toString();
    _allianceCtrl.text = _profile.allianceName;
    _notesCtrl.text = _profile.notes;
  }

  Future<void> _save() async {
    _profile.playerName = _nameCtrl.text.trim();
    _profile.prestige = int.tryParse(_prestigeCtrl.text) ?? 0;
    _profile.currentAct = _actCtrl.text.trim();
    _profile.summonerLevel = int.tryParse(_levelCtrl.text) ?? 1;
    _profile.allianceName = _allianceCtrl.text.trim();
    _profile.notes = _notesCtrl.text.trim();
    await StorageService.saveProfile(_profile);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Perfil salvo! ✅'), backgroundColor: Colors.green),
      );
      Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('MEU PERFIL')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            controller: _nameCtrl,
            decoration: const InputDecoration(
              labelText: 'Nome do Summoner',
              prefixIcon: Icon(Icons.person),
            ),
          ),
          const SizedBox(height: 16),
          const Text('Título',
              style: TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: UserProfile.titles.map((t) {
              return ChoiceChip(
                label: Text(t),
                selected: _profile.title == t,
                onSelected: (_) => setState(() => _profile.title = t),
                selectedColor: AppTheme.gold,
                labelStyle: TextStyle(
                    color: _profile.title == t ? Colors.black : AppTheme.textPrimary,
                    fontWeight: FontWeight.bold),
              );
            }).toList(),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _prestigeCtrl,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Prestígio',
              prefixIcon: Icon(Icons.star),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _actCtrl,
            decoration: const InputDecoration(
              labelText: 'Act atual (ex: 6.2.5)',
              prefixIcon: Icon(Icons.map),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _levelCtrl,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Level do Summoner',
              prefixIcon: Icon(Icons.trending_up),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _allianceCtrl,
            decoration: const InputDecoration(
              labelText: 'Aliança (opcional)',
              prefixIcon: Icon(Icons.group),
            ),
          ),
          const SizedBox(height: 16),
          const Text('Cargo na Aliança',
              style: TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: ['Member', 'Officer', 'Leader'].map((r) {
              return ChoiceChip(
                label: Text(r),
                selected: _profile.allianceRole == r,
                onSelected: (_) => setState(() => _profile.allianceRole = r),
                selectedColor: AppTheme.primaryRed,
                labelStyle: TextStyle(
                    color: _profile.allianceRole == r ? Colors.white : AppTheme.textPrimary,
                    fontWeight: FontWeight.bold),
              );
            }).toList(),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _notesCtrl,
            maxLines: 4,
            decoration: const InputDecoration(
              labelText: 'Observações / Metas',
              hintText: 'Ex: Quero chegar em Paragon até dezembro',
              alignLabelWithHint: true,
            ),
          ),
          const SizedBox(height: 30),
          ElevatedButton(onPressed: _save, child: const Text('SALVAR PERFIL')),
        ],
      ),
    );
  }
}
