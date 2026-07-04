import 'package:flutter/material.dart';
import '../data/champions_list.dart';
import '../theme/app_theme.dart';

class DatabaseScreen extends StatefulWidget {
  const DatabaseScreen({super.key});

  @override
  State<DatabaseScreen> createState() => _DatabaseScreenState();
}

class _DatabaseScreenState extends State<DatabaseScreen> {
  String _search = '';
  String _classFilter = 'Todos';
  String _tierFilter = 'Todos';

  List<Map<String, String>> get _filtered {
    return ChampionsList.all.where((c) {
      final matchSearch = _search.isEmpty ||
          c['name']!.toLowerCase().contains(_search.toLowerCase());
      final matchClass = _classFilter == 'Todos' || c['class'] == _classFilter;
      final matchTier = _tierFilter == 'Todos' || c['tier'] == _tierFilter;
      return matchSearch && matchClass && matchTier;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('DATABASE')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              decoration: const InputDecoration(
                hintText: 'Buscar campeão...',
                prefixIcon: Icon(Icons.search),
              ),
              onChanged: (v) => setState(() => _search = v),
            ),
          ),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: [
                ...['Todos', 'Cosmic', 'Tech', 'Mutant', 'Skill', 'Science', 'Mystic', 'Superior']
                    .map((c) => Padding(
                          padding: const EdgeInsets.only(right: 6),
                          child: FilterChip(
                            label: Text(c, style: const TextStyle(fontSize: 11)),
                            selected: _classFilter == c,
                            onSelected: (_) => setState(() => _classFilter = c),
                            selectedColor: AppTheme.primaryRed,
                            backgroundColor: AppTheme.cardBg,
                          ),
                        )),
              ],
            ),
          ),
          const SizedBox(height: 4),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children:
                  ['Todos', 'God', 'Demi-God', 'Great', 'Good', 'Decent'].map((t) {
                return Padding(
                  padding: const EdgeInsets.only(right: 6),
                  child: FilterChip(
                    label: Text(t, style: const TextStyle(fontSize: 11)),
                    selected: _tierFilter == t,
                    onSelected: (_) => setState(() => _tierFilter = t),
                    selectedColor: AppTheme.gold,
                    backgroundColor: AppTheme.cardBg,
                    labelStyle: TextStyle(
                        color: _tierFilter == t ? Colors.black : AppTheme.textPrimary,
                        fontSize: 11),
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: _filtered.length,
              itemBuilder: (context, i) {
                final c = _filtered[i];
                final classColor = AppTheme.classColors[c['class']] ?? Colors.grey;
                return Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: classColor.withOpacity(0.2),
                      child: Text(c['class']!.substring(0, 2),
                          style: TextStyle(color: classColor, fontWeight: FontWeight.bold)),
                    ),
                    title: Text(c['name']!,
                        style: const TextStyle(fontWeight: FontWeight.bold)),
                    subtitle: Text('${c['class']} • ${c['role']}'),
                    trailing: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: _tierColor(c['tier']!).withOpacity(0.2),
                        borderRadius: BorderRadius.circular(4),
                        border: Border.all(color: _tierColor(c['tier']!)),
                      ),
                      child: Text(c['tier']!,
                          style: TextStyle(
                              color: _tierColor(c['tier']!),
                              fontSize: 11,
                              fontWeight: FontWeight.bold)),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Color _tierColor(String tier) {
    switch (tier) {
      case 'God': return Colors.deepOrange;
      case 'Demi-God': return Colors.orange;
      case 'Great': return Colors.amber;
      case 'Good': return Colors.lightGreen;
      case 'Decent': return Colors.blueGrey;
      default: return Colors.grey;
    }
  }
}
