# 🛡️ MCOC Coach AI

Seu treinador pessoal inteligente para **Marvel Contest of Champions**. Um app Flutter para Android que usa IA (Google Gemini) para te guiar do zero ao Valiant, com base no SEU roster real.

## ✨ Recursos

- 💬 **Chat com Coach AI** — pergunta qualquer coisa do jogo
- ⚔️ **Roster Manager** — cadastre seus champs (estrela, rank, sig, awakened, ascended)
- 👤 **Perfil do jogador** — título, prestígio, act atual
- 🎯 **Node Helper** — descreva um node e receba a melhor escolha do seu roster
- 📚 **Database** — consulta de campeões meta com filtros
- 🔒 **Privacidade total** — API key e roster ficam SÓ NO SEU CELULAR
- 🎨 **Tema Marvel** — vermelho + dourado, tipografia gamer

## 📦 Como compilar o APK

### Opção A — Compilar online (RECOMENDADO, sem instalar nada)

1. Crie uma conta grátis em [Codemagic.io](https://codemagic.io)
2. Faça upload do projeto (ou conecte via GitHub)
3. Escolha "Flutter App" → "Android APK"
4. Clique em "Start new build"
5. Baixe o APK gerado (~15 min)

### Opção B — Compilar localmente (se você tem Flutter instalado)

```bash
cd mcoc_coach_ai
flutter pub get
flutter build apk --release
```

O APK ficará em: `build/app/outputs/flutter-apk/app-release.apk`

### Opção C — Testar em emulador/celular conectado

```bash
flutter pub get
flutter run
```

## 🔑 Configuração inicial (obrigatório)

Ao abrir o app pela primeira vez, você precisa colar uma **API Key grátis do Google Gemini**:

1. Acesse: https://aistudio.google.com/apikey
2. Faça login com sua conta Google
3. Clique em **"Create API Key"**
4. Copie a chave (começa com `AIzaSy...`)
5. Cole no app na tela de onboarding

**Limites do tier grátis do Gemini 2.0 Flash:**
- 15 requisições/minuto
- 1.500 requisições/dia
- 1M tokens de contexto

Isso é MAIS do que suficiente para uso pessoal do app.

## 📱 Como instalar o APK no Android

1. Baixe o APK gerado
2. No celular, vá em **Configurações → Segurança → Fontes desconhecidas** e ative
3. Abra o APK baixado
4. Toque em "Instalar"
5. Abra o app e configure sua API key

## 🎮 Como usar

### Fluxo recomendado:
1. **Preencha seu perfil** (Meu Perfil → título, prestígio, act)
2. **Cadastre seu roster completo** (Meu Roster → adicionar champs)
3. **Converse com o Coach** — ele já vai saber tudo do seu perfil e sugerir ações específicas

### Exemplos de perguntas:
- "O que eu devo fazer agora?"
- "Analisa meu roster e me diz os pontos fracos"
- "Qual champ eu upo pra R5 primeiro?"
- "Tô no Act 6.2.6, qual champ uso contra o Champion?"
- "Como chego em Thronebreaker?"
- "Vale a pena awakear meu Aegon?"

### Node Helper:
Se você está numa luta específica e não sabe qual champ usar:
1. Abra **Node Helper**
2. Digite o nome do boss (ex: "Serpent")
3. Digite os nodes (ex: "Buffet, Encroaching Stun, MD")
4. O Coach vai recomendar o MELHOR champ do SEU roster

## 🗂️ Estrutura do projeto

```
mcoc_coach_ai/
├── lib/
│   ├── main.dart                    # Entry point
│   ├── models/                      # Modelos de dados
│   │   ├── champion.dart            # Champion + UserChampion
│   │   ├── user_profile.dart        # Perfil do jogador
│   │   └── chat_message.dart        # Mensagens do chat
│   ├── services/                    # Lógica de negócio
│   │   ├── storage_service.dart     # SharedPreferences
│   │   ├── gemini_service.dart      # API do Gemini
│   │   └── coach_context_builder.dart  # Monta contexto pra IA
│   ├── screens/                     # Telas
│   │   ├── splash_screen.dart
│   │   ├── onboarding_screen.dart
│   │   ├── home_screen.dart
│   │   ├── chat_screen.dart
│   │   ├── roster_screen.dart
│   │   ├── add_champion_screen.dart
│   │   ├── profile_screen.dart
│   │   ├── node_helper_screen.dart
│   │   ├── database_screen.dart
│   │   └── settings_screen.dart
│   ├── data/
│   │   ├── champions_list.dart      # Lista de champs (editável)
│   │   └── mcoc_knowledge_base.dart # Base de conhecimento do meta
│   └── theme/
│       └── app_theme.dart           # Tema visual
├── android/                         # Config Android
├── assets/data/                     # JSONs de nodes e progressão
├── pubspec.yaml                     # Dependências Flutter
└── codemagic.yaml                   # Config build online
```

## 🔧 Personalização

### Adicionar mais champs
Edite `lib/data/champions_list.dart` e adicione novos entries no formato:
```dart
{'name': 'Nome do Champ', 'class': 'Cosmic', 'tier': 'God', 'role': 'Attacker'},
```

### Atualizar meta / conhecimento do coach
Edite `lib/data/mcoc_knowledge_base.dart` para atualizar tier list, meta atual, dicas, etc.

### Mudar cores
Edite `lib/theme/app_theme.dart`.

## ⚠️ Disclaimer

- Este é um app **não-oficial e não-afiliado** com a Kabam ou Marvel
- É apenas um **companion app** — NÃO joga o jogo por você
- **NÃO viola os Termos de Serviço** do MCOC (não automatiza gameplay)
- Marvel Contest of Champions é marca registrada da Kabam Games Inc.

## 📄 Licença

MIT License — Use, modifique e distribua livremente.

---

Feito com ⚔️ e IA 🤖
