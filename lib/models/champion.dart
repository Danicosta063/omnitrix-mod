class Champion {
  final String id;
  final String name;
  final String championClass; // Cosmic, Tech, Mutant, Skill, Science, Mystic, Superior
  final String tier; // God, Demi-God, Great, Good, Decent, Meh, Awful
  final List<String> abilities;
  final List<String> immunities;
  final String role; // Attacker, Defender, Utility, Hybrid
  final String awakenedAbility;
  final int prestige;
  final String description;
  final List<String> bestNodes;
  final List<String> counters;

  Champion({
    required this.id,
    required this.name,
    required this.championClass,
    required this.tier,
    this.abilities = const [],
    this.immunities = const [],
    this.role = 'Hybrid',
    this.awakenedAbility = '',
    this.prestige = 0,
    this.description = '',
    this.bestNodes = const [],
    this.counters = const [],
  });

  factory Champion.fromJson(Map<String, dynamic> json) {
    return Champion(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      championClass: json['class'] ?? 'Skill',
      tier: json['tier'] ?? 'Decent',
      abilities: List<String>.from(json['abilities'] ?? []),
      immunities: List<String>.from(json['immunities'] ?? []),
      role: json['role'] ?? 'Hybrid',
      awakenedAbility: json['awakenedAbility'] ?? '',
      prestige: json['prestige'] ?? 0,
      description: json['description'] ?? '',
      bestNodes: List<String>.from(json['bestNodes'] ?? []),
      counters: List<String>.from(json['counters'] ?? []),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'class': championClass,
        'tier': tier,
        'abilities': abilities,
        'immunities': immunities,
        'role': role,
        'awakenedAbility': awakenedAbility,
        'prestige': prestige,
        'description': description,
        'bestNodes': bestNodes,
        'counters': counters,
      };
}

/// Representa um campeão específico no roster do usuário
class UserChampion {
  final String championId;
  final String championName;
  final String championClass;
  int stars; // 3, 4, 5, 6, 7
  int rank; // 1-5 (5*), 1-6 (6*), 1-3 (7*)
  int sigLevel; // 0-200
  bool awakened;
  bool ascended;
  DateTime addedAt;

  UserChampion({
    required this.championId,
    required this.championName,
    required this.championClass,
    this.stars = 6,
    this.rank = 1,
    this.sigLevel = 0,
    this.awakened = false,
    this.ascended = false,
    DateTime? addedAt,
  }) : addedAt = addedAt ?? DateTime.now();

  factory UserChampion.fromJson(Map<String, dynamic> json) {
    return UserChampion(
      championId: json['championId'] ?? '',
      championName: json['championName'] ?? '',
      championClass: json['championClass'] ?? 'Skill',
      stars: json['stars'] ?? 6,
      rank: json['rank'] ?? 1,
      sigLevel: json['sigLevel'] ?? 0,
      awakened: json['awakened'] ?? false,
      ascended: json['ascended'] ?? false,
      addedAt: json['addedAt'] != null
          ? DateTime.parse(json['addedAt'])
          : DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() => {
        'championId': championId,
        'championName': championName,
        'championClass': championClass,
        'stars': stars,
        'rank': rank,
        'sigLevel': sigLevel,
        'awakened': awakened,
        'ascended': ascended,
        'addedAt': addedAt.toIso8601String(),
      };

  String get displayRank {
    final asc = ascended ? ' Asc' : '';
    return '${stars}★ R$rank$asc';
  }

  String get shortDescription {
    final aw = awakened ? ' (Awakened sig $sigLevel)' : '';
    return '$championName $displayRank$aw';
  }
}
