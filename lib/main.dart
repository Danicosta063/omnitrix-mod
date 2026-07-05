import 'package:flutter/material.dart';

void main() {
  runApp(const MCOCCoachApp());
}

class MCOCCoachApp extends StatelessWidget {
  const MCOCCoachApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MCOC Coach AI',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xFF0D0D0F),
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFED1D24),
          brightness: Brightness.dark,
        ),
      ),
      home: const TesteScreen(),
    );
  }
}

class TesteScreen extends StatelessWidget {
  const TesteScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.shield,
              color: Color(0xFFED1D24),
              size: 80,
            ),
            const SizedBox(height: 20),
            const Text(
              'MCOC COACH AI',
              style: TextStyle(
                color: Color(0xFFFFC72C),
                fontSize: 28,
                fontWeight: FontWeight.bold,
                letterSpacing: 2,
              ),
            ),
            const SizedBox(height: 10),
            const Text(
              'Build funcionando! ✅',
              style: TextStyle(color: Colors.white70, fontSize: 16),
            ),
          ],
        ),
      ),
    );
  }
}
