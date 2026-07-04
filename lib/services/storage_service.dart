import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/champion.dart';
import '../models/user_profile.dart';
import '../models/chat_message.dart';

class StorageService {
  static late SharedPreferences _prefs;

  static const String _kApiKey = 'gemini_api_key';
  static const String _kUserProfile = 'user_profile';
  static const String _kRoster = 'user_roster';
  static const String _kChatHistory = 'chat_history';
  static const String _kFirstRun = 'first_run';

  static Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
  }

  // === API Key ===
  static String? get apiKey => _prefs.getString(_kApiKey);
  static Future<void> setApiKey(String key) => _prefs.setString(_kApiKey, key);
  static bool get hasApiKey => (apiKey ?? '').trim().isNotEmpty;

  // === First Run ===
  static bool get isFirstRun => _prefs.getBool(_kFirstRun) ?? true;
  static Future<void> setFirstRunDone() => _prefs.setBool(_kFirstRun, false);

  // === User Profile ===
  static UserProfile getProfile() {
    final json = _prefs.getString(_kUserProfile);
    if (json == null) return UserProfile();
    return UserProfile.fromJson(jsonDecode(json));
  }

  static Future<void> saveProfile(UserProfile profile) async {
    await _prefs.setString(_kUserProfile, jsonEncode(profile.toJson()));
  }

  // === Roster ===
  static List<UserChampion> getRoster() {
    final json = _prefs.getString(_kRoster);
    if (json == null) return [];
    final List decoded = jsonDecode(json);
    return decoded.map((e) => UserChampion.fromJson(e)).toList();
  }

  static Future<void> saveRoster(List<UserChampion> roster) async {
    final json = jsonEncode(roster.map((c) => c.toJson()).toList());
    await _prefs.setString(_kRoster, json);
  }

  static Future<void> addChampion(UserChampion champion) async {
    final roster = getRoster();
    roster.add(champion);
    await saveRoster(roster);
  }

  static Future<void> removeChampion(String championId, int stars) async {
    final roster = getRoster();
    roster.removeWhere((c) => c.championId == championId && c.stars == stars);
    await saveRoster(roster);
  }

  // === Chat History ===
  static List<ChatMessage> getChatHistory() {
    final json = _prefs.getString(_kChatHistory);
    if (json == null) return [];
    final List decoded = jsonDecode(json);
    return decoded.map((e) => ChatMessage.fromJson(e)).toList();
  }

  static Future<void> saveChatHistory(List<ChatMessage> messages) async {
    // Manter apenas as últimas 50 mensagens pra não ficar pesado
    final trimmed = messages.length > 50
        ? messages.sublist(messages.length - 50)
        : messages;
    final json = jsonEncode(trimmed.map((m) => m.toJson()).toList());
    await _prefs.setString(_kChatHistory, json);
  }

  static Future<void> clearChatHistory() async {
    await _prefs.remove(_kChatHistory);
  }
}
