import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  // Paleta inspirada em MCOC - vermelho Marvel + dourado
  static const Color primaryRed = Color(0xFFED1D24);
  static const Color darkRed = Color(0xFF9E0A0F);
  static const Color gold = Color(0xFFFFC72C);
  static const Color darkBg = Color(0xFF0D0D0F);
  static const Color cardBg = Color(0xFF1A1A1F);
  static const Color surfaceBg = Color(0xFF25252D);
  static const Color textPrimary = Color(0xFFF5F5F7);
  static const Color textSecondary = Color(0xFFB0B0B8);

  // Cores por classe do MCOC
  static const Map<String, Color> classColors = {
    'Cosmic': Color(0xFFFFD700),      // Dourado
    'Tech': Color(0xFF3B82F6),        // Azul
    'Mutant': Color(0xFFEAB308),      // Amarelo
    'Skill': Color(0xFFDC2626),       // Vermelho
    'Science': Color(0xFF16A34A),     // Verde
    'Mystic': Color(0xFF9333EA),      // Roxo
    'Superior': Color(0xFFEC4899),    // Rosa
  };

  static ThemeData get darkTheme => ThemeData(
        brightness: Brightness.dark,
        useMaterial3: true,
        scaffoldBackgroundColor: darkBg,
        primaryColor: primaryRed,
        colorScheme: const ColorScheme.dark(
          primary: primaryRed,
          secondary: gold,
          surface: cardBg,
          onSurface: textPrimary,
        ),
        textTheme: GoogleFonts.rajdhaniTextTheme(
          ThemeData.dark().textTheme.copyWith(
                bodyLarge: const TextStyle(color: textPrimary),
                bodyMedium: const TextStyle(color: textPrimary),
                titleLarge: const TextStyle(color: textPrimary, fontWeight: FontWeight.bold),
              ),
        ),
        appBarTheme: AppBarTheme(
          backgroundColor: darkBg,
          elevation: 0,
          centerTitle: true,
          titleTextStyle: GoogleFonts.rajdhani(
            color: gold,
            fontSize: 22,
            fontWeight: FontWeight.bold,
            letterSpacing: 1.2,
          ),
          iconTheme: const IconThemeData(color: gold),
        ),
        cardTheme: CardTheme(
          color: cardBg,
          elevation: 4,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: BorderSide(color: gold.withOpacity(0.2), width: 1),
          ),
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: primaryRed,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
            textStyle: GoogleFonts.rajdhani(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              letterSpacing: 1.1,
            ),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: surfaceBg,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: BorderSide.none,
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: gold, width: 2),
          ),
          labelStyle: const TextStyle(color: textSecondary),
        ),
      );
}
