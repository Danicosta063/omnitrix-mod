# 🔨 Como gerar o APK do MCOC Coach AI

Como você desenvolve no celular e não tem Flutter instalado, o caminho mais fácil é **compilar o APK online**. Segue 3 opções em ordem de facilidade:

---

## 🥇 OPÇÃO 1 — Codemagic (mais fácil e gratuito)

1. Crie conta em https://codemagic.io (grátis, sem cartão)
2. Baixe o ZIP deste projeto e descompacte
3. Suba o projeto no seu GitHub (crie um repo privado)
4. No Codemagic:
   - "Add application" → conecte GitHub
   - Escolha o repo `mcoc_coach_ai`
   - Selecione "Flutter App (Android)"
   - Codemagic vai detectar o `codemagic.yaml` automaticamente
5. Clique em **"Start new build"**
6. Aguarde ~15 minutos
7. Baixe o `app-release.apk` gerado

**Créditos grátis Codemagic:** 500 minutos/mês (dá pra fazer uns 30 builds)

---

## 🥈 OPÇÃO 2 — GitHub Actions (grátis ilimitado)

1. Suba o projeto no GitHub
2. Crie o arquivo `.github/workflows/build.yml` com esse conteúdo:

```yaml
name: Build APK
on: [push, workflow_dispatch]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.24.0'
          channel: 'stable'
      - run: flutter pub get
      - run: flutter build apk --release
      - uses: actions/upload-artifact@v4
        with:
          name: mcoc-coach-apk
          path: build/app/outputs/flutter-apk/app-release.apk
```

3. Faça push → o build inicia automaticamente
4. Vá em "Actions" no GitHub → baixe o artefato

---

## 🥉 OPÇÃO 3 — FlutLab.io (IDE Flutter no navegador)

1. Acesse https://flutlab.io (grátis)
2. Crie uma conta
3. "Create new project" → "Import from ZIP" → sub o zip
4. Clique em "Build APK"
5. Baixe

---

## 📲 Instalar o APK no Android

1. No celular: **Configurações → Aplicativos → Acesso especial → Instalar apps desconhecidos**
2. Autorize seu navegador ou gerenciador de arquivos
3. Toque no APK baixado
4. Instalar
5. Abra o app e configure sua Gemini API Key

---

## 🔑 Não esqueça: pegue sua Gemini API Key GRÁTIS

1. https://aistudio.google.com/apikey
2. Login com Google
3. "Create API Key"
4. Copie e cole no app

**Você está pronto! 🚀**
