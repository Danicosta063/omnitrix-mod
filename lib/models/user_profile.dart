class UserProfile {
  String playerName;
  String title; // Uncollected, Cavalier, Thronebreaker, Paragon, Valiant
  int prestige;
  String currentAct; // "6.2", "7.4", "8.2", etc.
  String currentChapter;
  int summonerLevel;
  String allianceName;
  String allianceRole; // Officer, Member, Leader
  List<String> goals; // Metas do jogador
  String notes;

  UserProfile({
    this.playerName = '',
    this.title = 'Iniciante',
    this.prestige = 0,
    this.currentAct = '1.1',
    this.currentChapter = '',
    this.summonerLevel = 1,
    this.allianceName = '',
    this.allianceRole = 'Member',
    this.goals = const [],
    this.notes = '',
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      playerName: json['playerName'] ?? '',
      title: json['title'] ?? 'Iniciante',
      prestige: json['prestige'] ?? 0,
      currentAct: json['currentAct'] ?? '1.1',
      currentChapter: json['currentChapter'] ?? '',
      summonerLevel: json['summonerLevel'] ?? 1,
      allianceName: json['allianceName'] ?? '',
      allianceRole: json['allianceRole'] ?? 'Member',
      goals: List<String>.from(json['goals'] ?? []),
      notes: json['notes'] ?? '',
    );
  }

  Map<String, dynamic> toJson() => {
        'playerName': playerName,
        'title': title,
        'prestige': prestige,
        'currentAct': currentAct,
        'currentChapter': currentChapter,
        'summonerLevel': summonerLevel,
        'allianceName': allianceName,
        'allianceRole': allianceRole,
        'goals': goals,
        'notes': notes,
      };

  static const List<String> titles = [
    'Iniciante',
    'Uncollected',
    'Cavalier',
    'Thronebreaker',
    'Paragon',
    'Valiant',
  ];
}
