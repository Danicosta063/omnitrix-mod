import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';
import '../models/champion.dart';
import '../data/champions_list.dart';
import '../services/storage_service.dart';
import '../theme/app_theme.dart';

class AddChampionScreen extends StatefulWidget {
  final UserChampion? editChampion;
  const AddChampionScreen({super.key, this.editChampion});

  @override
  State<AddChampionScreen> createState() => _AddChampionScreenState();
}

class _AddChampionScreenState extends State<AddChampionScreen> {
  String? _selectedChampName;
  String _selectedClass = 'Skill';
  int _stars = 6;
  int _rank = 1;
  int _sigLevel = 0;
  bool _awakened = false;
  bool _ascended = false;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    if (widget.editChampion != null) {
      _selectedChampName = widget.editChampion!.championName;
      _selectedClass = widget.editChampion!.championClass;
      _stars = widget.editChampion!.stars;
      _rank = widget.editChampion!.rank;
      _sigLevel = widget.editChampion!.sigLevel;
      _awakened = widget.editChampion!.awakened;
      _ascended = widget.editChampion!.ascended;
    }
  }

  int get _maxRank {
    switch (_stars) {
      case 7: return 4;
      case 6: return 5;
      case 5: return 5;
      case 4: return 5;
      case 3: return 5;
      default: return 5;
    }
  }

  Future<void> _save() async {
    if (_selectedChampName == null || _selectedChampName!.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Selecione um campeão'), backgroundColor: AppTheme.primaryRed),
      );
      return;
    }

    if (widget.editChampion != null) {
      // Editar: remover antigo e adicionar novo
      await StorageService.removeChampion(
          widget.editChampion!.championId, widget.editChampion!.stars);
    }

    final champ = UserChampion(
      championId: const Uuid().v4(),
      championName: _selectedChampName!,
      championClass: _selectedClass,
      stars: _stars,
      rank: _rank,
      sigLevel: _awakened ? _sigLevel : 0,
      awakened: _awakened,
      ascended: _ascended,
    );

    await StorageService.addChampion(champ);
    if (mounted) Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.editChampion != null;
    return Scaffold(
      appBar: AppBar(
        title: Text(isEditing ? 'EDITAR CHAMP' : 'ADICIONAR CHAMP'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Seletor de champ
          const Text('Campeão',
              style: TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          TextField(
            decoration: const InputDecoration(
              hintText: 'Buscar campeão...',
              prefixIcon: Icon(Icons.search),
            ),
            onChanged: (v) => setState(() => _searchQuery = v.toLowerCase()),
          ),
          const SizedBox(height: 8),
          Container(
            height: 200,
            decoration: BoxDecoration(
              color: AppTheme.surfaceBg,
              borderRadius: BorderRadius.circular(8),
            ),
            child: ListView(
              children: ChampionsList.all
                  .where((c) => _searchQuery.isEmpty ||
                      c['name']!.toLowerCase().contains(_searchQuery))
                  .map((c) {
                final isSelected = _selectedChampName == c['name'];
                return ListTile(
                  dense: true,
                  selected: isSelected,
                  selectedTileColor: AppTheme.primaryRed.withOpacity(0.2),
                  leading: CircleAvatar(
                    radius: 14,
                    backgroundColor: AppTheme.classColors[c['class']] ?? Colors.grey,
                    child: Text(c['class']!.substring(0, 1),
                        style: const TextStyle(color: Colors.white, fontSize: 12)),
                  ),
                  title: Text(c['name']!, style: const TextStyle(fontSize: 14)),
                  subtitle: Text(c['class']!, style: const TextStyle(fontSize: 11)),
                  onTap: () => setState(() {
                    _selectedChampName = c['name'];
                    _selectedClass = c['class']!;
                  }),
                );
              }).toList(),
            ),
          ),
          if (_selectedChampName != null) ...[
            const SizedBox(height: 8),
            Text('Selecionado: $_selectedChampName ($_selectedClass)',
                style: const TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold)),
          ],
          const SizedBox(height: 20),

          // Estrelas
          const Text('Estrelas',
              style: TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: [3, 4, 5, 6, 7].map((s) {
              return ChoiceChip(
                label: Text('$s★'),
                selected: _stars == s,
                onSelected: (_) => setState(() {
                  _stars = s;
                  if (_rank > _maxRank) _rank = _maxRank;
                }),
                selectedColor: AppTheme.gold,
                labelStyle: TextStyle(
                    color: _stars == s ? Colors.black : AppTheme.textPrimary,
                    fontWeight: FontWeight.bold),
              );
            }).toList(),
          ),
          const SizedBox(height: 20),

          // Rank
          Text('Rank (1 - $_maxRank)',
              style: const TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: List.generate(_maxRank, (i) => i + 1).map((r) {
              return ChoiceChip(
                label: Text('R$r'),
                selected: _rank == r,
                onSelected: (_) => setState(() => _rank = r),
                selectedColor: AppTheme.primaryRed,
                labelStyle: TextStyle(
                    color: _rank == r ? Colors.white : AppTheme.textPrimary,
                    fontWeight: FontWeight.bold),
              );
            }).toList(),
          ),
          const SizedBox(height: 20),

          // Awakened
          SwitchListTile(
            title: const Text('Awakened (Duplicado)'),
            subtitle: const Text('O champ está com signature ability?'),
            value: _awakened,
            activeColor: AppTheme.gold,
            onChanged: (v) => setState(() => _awakened = v),
          ),
          if (_awakened) ...[
            const SizedBox(height: 8),
            Text('Signature Level: $_sigLevel',
                style: const TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold)),
            Slider(
              value: _sigLevel.toDouble(),
              min: 0,
              max: 200,
              divisions: 40,
              label: '$_sigLevel',
              activeColor: AppTheme.gold,
              onChanged: (v) => setState(() => _sigLevel = v.round()),
            ),
          ],
          const SizedBox(height: 8),

          // Ascended (só faz sentido pra 6★ e 7★)
          if (_stars >= 6)
            SwitchListTile(
              title: const Text('Ascended'),
              subtitle: const Text('Champ ascendido?'),
              value: _ascended,
              activeColor: Colors.orange,
              onChanged: (v) => setState(() => _ascended = v),
            ),

          const SizedBox(height: 30),
          ElevatedButton(
            onPressed: _save,
            child: Text(isEditing ? 'SALVAR ALTERAÇÕES' : 'ADICIONAR AO ROSTER'),
          ),
          const SizedBox(height: 40),
        ],
      ),
    );
  }
}
