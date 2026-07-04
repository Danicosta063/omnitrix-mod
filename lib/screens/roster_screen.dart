import 'package:flutter/material.dart';
import '../models/champion.dart';
import '../services/storage_service.dart';
import '../theme/app_theme.dart';
import 'add_champion_screen.dart';

class RosterScreen extends StatefulWidget {
  const RosterScreen({super.key});

  @override
  State<RosterScreen> createState() => _RosterScreenState();
}

class _RosterScreenState extends State<RosterScreen> {
  List<UserChampion> _roster = [];
  String _filter = 'Todos';
  String _sortBy = 'Ranking';

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    setState(() {
      _roster = StorageService.getRoster();
      _sortRoster();
    });
  }

  void _sortRoster() {
    _roster.sort((a, b) {
      if (_sortBy == 'Ranking') {
        // Ordena por estrelas > rank > sig
        final s = b.stars.compareTo(a.stars);
        if (s != 0) return s;
        final r = b.rank.compareTo(a.rank);
        if (r != 0) return r;
        return b.sigLevel.compareTo(a.sigLevel);
      } else if (_sortBy == 'Nome') {
        return a.championName.compareTo(b.championName);
      } else {
        // Classe
        return a.championClass.compareTo(b.championClass);
      }
    });
  }

  List<UserChampion> get _filteredRoster {
    if (_filter == 'Todos') return _roster;
    if (_filter == 'Awakened') return _roster.where((c) => c.awakened).toList();
    if (_filter == '7★') return _roster.where((c) => c.stars == 7).toList();
    if (_filter == '6★') return _roster.where((c) => c.stars == 6).toList();
    if (_filter == '5★') return _roster.where((c) => c.stars == 5).toList();
    return _roster.where((c) => c.championClass == _filter).toList();
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _filteredRoster;
    return Scaffold(
      appBar: AppBar(
        title: Text('ROSTER (${_roster.length})'),
        actions: [
          PopupMenuButton<String>(
            icon: const Icon(Icons.sort),
            onSelected: (v) => setState(() {
              _sortBy = v;
              _sortRoster();
            }),
            itemBuilder: (_) => ['Ranking', 'Nome', 'Classe']
                .map((s) => PopupMenuItem(value: s, child: Text('Ordenar: $s')))
                .toList(),
          ),
        ],
      ),
      body: Column(
        children: [
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: Row(
              children: [
                'Todos', 'Awakened', '7★', '6★', '5★',
                'Cosmic', 'Tech', 'Mutant', 'Skill', 'Science', 'Mystic', 'Superior'
              ].map((f) {
                final isSelected = _filter == f;
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    label: Text(f),
                    selected: isSelected,
                    onSelected: (_) => setState(() => _filter = f),
                    selectedColor: AppTheme.primaryRed,
                    backgroundColor: AppTheme.cardBg,
                    labelStyle: TextStyle(
                      color: isSelected ? Colors.white : AppTheme.textPrimary,
                      fontSize: 12,
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
          Expanded(
            child: filtered.isEmpty
                ? _buildEmptyState()
                : ListView.builder(
                    padding: const EdgeInsets.all(12),
                    itemCount: filtered.length,
                    itemBuilder: (context, i) => _buildChampCard(filtered[i]),
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () async {
          await Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const AddChampionScreen()),
          );
          _load();
        },
        backgroundColor: AppTheme.primaryRed,
        icon: const Icon(Icons.add, color: Colors.white),
        label: const Text('ADICIONAR', style: TextStyle(color: Colors.white)),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.shield_outlined, size: 80, color: AppTheme.textSecondary),
            const SizedBox(height: 20),
            Text(
              _filter == 'Todos'
                  ? 'Roster vazio'
                  : 'Nenhum champ nesse filtro',
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            const Text(
              'Cadastre seus champs pra o Coach AI dar recomendações certeiras',
              textAlign: TextAlign.center,
              style: TextStyle(color: AppTheme.textSecondary),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildChampCard(UserChampion c) {
    final classColor = AppTheme.classColors[c.championClass] ?? Colors.grey;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Container(
          width: 50,
          height: 50,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: classColor.withOpacity(0.2),
            border: Border.all(color: classColor, width: 2),
          ),
          child: Center(
            child: Text(c.championClass.substring(0, 2),
                style: TextStyle(color: classColor, fontWeight: FontWeight.bold)),
          ),
        ),
        title: Text(c.championName,
            style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Row(
          children: [
            _buildBadge('${c.stars}★', AppTheme.gold),
            const SizedBox(width: 4),
            _buildBadge('R${c.rank}', AppTheme.primaryRed),
            if (c.awakened) ...[
              const SizedBox(width: 4),
              _buildBadge('SIG ${c.sigLevel}', Colors.purpleAccent),
            ],
            if (c.ascended) ...[
              const SizedBox(width: 4),
              _buildBadge('ASC', Colors.orange),
            ],
          ],
        ),
        trailing: IconButton(
          icon: const Icon(Icons.more_vert),
          onPressed: () => _showActions(c),
        ),
        onTap: () async {
          await Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => AddChampionScreen(editChampion: c),
            ),
          );
          _load();
        },
      ),
    );
  }

  Widget _buildBadge(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withOpacity(0.2),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: color.withOpacity(0.5)),
      ),
      child: Text(text,
          style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.bold)),
    );
  }

  void _showActions(UserChampion c) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppTheme.cardBg,
      builder: (_) => Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(
            leading: const Icon(Icons.edit, color: AppTheme.gold),
            title: const Text('Editar'),
            onTap: () async {
              Navigator.pop(context);
              await Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => AddChampionScreen(editChampion: c)),
              );
              _load();
            },
          ),
          ListTile(
            leading: const Icon(Icons.delete, color: AppTheme.primaryRed),
            title: const Text('Remover'),
            onTap: () async {
              Navigator.pop(context);
              await StorageService.removeChampion(c.championId, c.stars);
              _load();
            },
          ),
        ],
      ),
    );
  }
}
