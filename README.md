# Omnitrix Mod — v0.1 Beta

Mod do **Omnitrix da série clássica Ben 10 (2005)** para **Minecraft Java 1.19.4 / Fabric**.

> "É hora do herói!" — Ben Tennyson

---

## 📋 O que tem nesta v0.1 beta

### ✅ Funcionalidades implementadas
- **10 aliens originais da 1ª temporada**:
  Heatblast, Wildmutt, Diamondhead, XLR8, Grey Matter, Four Arms, Stinkfly, Ripjaws, Upgrade, Ghostfreak.
- **Meteorito cinematográfico**: risco verde no céu, estrondo, partículas, fumaça, cratera com obsidiana e um núcleo brilhante. Aproxime-se para ganhar o Omnitrix.
- **Omnitrix permanente**: uma vez encontrado, fica preso ao seu jogador para sempre — sobrevive à morte, respawn, troca de dimensões. Salvo no NBT do jogador.
- **Seletor holográfico**: círculo verde giratório no centro da tela com os 10 aliens distribuídos ao redor, alien selecionado destacado no centro com sua cor temática, brilho pulsante.
- **Sistema de teclas**:
  - **G** → abre o seletor. Toque para abrir / ciclar alien. **Segurar G = cicla automaticamente** entre todos os aliens.
  - **J** → confirma transformação (tapa no relógio).
- **Mecânica fiel ao desenho**:
  - 2 minutos de transformação
  - 1 minuto de cooldown
  - Tela verde 2s ao transformar / Tela vermelha 2s ao destransformar
  - 20% de chance de virar **alien aleatório** se tentar transformar durante o cooldown (mau funcionamento clássico!)
- **Efeitos por alien** (sem texturas no v0.1, conforme pedido):
  - **Heatblast** → Resistência a fogo + Força + Imune a queda em fogo
  - **Wildmutt** → Visão noturna + Velocidade III + Pulo III + Força III
  - **Diamondhead** → Resistência IV + Força III + Lentidão
  - **XLR8** → Velocidade VI + Pulo IV + Haste III
  - **Grey Matter** → Haste V + Pulo IV + Velocidade + Fraqueza
  - **Four Arms** → Força V + Resistência III + +6 corações + Lentidão
  - **Stinkfly** → **Voo** + Queda lenta + Velocidade + Pulo V
  - **Ripjaws** → Respiração aquática + Graça do golfinho + Cura na água
  - **Upgrade** → Resistência + Regeneração + Velocidade
  - **Ghostfreak** → Invisibilidade + Velocidade + Queda lenta + Visão noturna
- **Câmera lenta ao abrir o seletor** (Slowness aplicado para imersão).
- **Comandos**:
  - `/omnitrix meteorite` — invoca um meteorito perto de você
  - `/omnitrix give` (OP) — concede o Omnitrix instantaneamente (debug)
  - `/omnitrix remove` (OP) — remove o Omnitrix

### 🟡 O que vai vir nas próximas versões
- **v0.2** — Modelo 3D GeckoLib do Omnitrix renderizado no braço direito do jogador (1ª e 3ª pessoa); luz dinâmica.
- **v0.3** — Texturas e modelos 3D dos 10 aliens (substituem a skin do jogador durante a transformação).
- **v0.4** — Sons originais do desenho (você cola os arquivos `.ogg` na pasta `sounds/` e o mod já usa).
- **v0.5** — Inimigos clássicos (Vilgax, Kevin 11), itens (Null Void Projector, etc.).

---

## 📥 Como instalar (e o desafio do celular)

### ⚠️ Você NÃO pode usar este projeto como está direto no Zalith Launcher
O Zalith Launcher *roda* mods Java no Android, mas **não compila** código `.java`. Você precisa primeiro **compilar** este projeto em um arquivo `.jar` instalável. Existem **3 caminhos**:

### Opção 1 (RECOMENDADA): GitHub Actions — compila na nuvem, de graça 🌟

1. Crie uma conta no [GitHub](https://github.com) (pelo celular mesmo).
2. Crie um novo repositório (botão verde "New").
3. Faça upload de **todos os arquivos deste ZIP** para o repositório:
   - Use o site do GitHub no navegador do celular: botão "Add file" → "Upload files" → arraste a pasta inteira.
   - **OU** use o app **Termux** com `git`.
4. O arquivo `.github/workflows/build.yml` já está incluído — o GitHub vai detectar e compilar automaticamente.
5. Vá em **Actions** no seu repositório, espere o build (~3 minutos), e baixe o `.jar` em "Artifacts".
6. Instale o `.jar` no Zalith Launcher (pasta de mods).

### Opção 2: Termux no Android (avançado, mais lento)

```bash
pkg update && pkg upgrade
pkg install openjdk-17 git
git clone <seu-repo-aqui>
cd omnitrix-mod
./gradlew build
# .jar fica em build/libs/
```
> Atenção: a primeira compilação baixa Gradle + Fabric loom (~500 MB) e demora bastante em conexão móvel.

### Opção 3: Pedir ajuda a um amigo com PC
Mande o ZIP, ele roda `./gradlew build`, te devolve o `.jar`.

---

## 🎮 Como jogar

1. Instale o Fabric Loader 1.19.4 no Zalith Launcher.
2. Coloque na pasta `mods/`:
   - **Fabric API** (1.19.4) — [Modrinth](https://modrinth.com/mod/fabric-api)
   - **GeckoLib 4** (1.19.4) — [Modrinth](https://modrinth.com/mod/geckolib)
   - **omnitrix-mod-0.1.0-beta.jar** (compilado deste projeto)
3. Crie um mundo e use `/omnitrix meteorite` para invocar o meteoro.
4. Quando o meteoro cair, vá até a cratera (núcleo brilhante) — o Omnitrix se prenderá ao seu braço.
5. Pressione **G** para abrir o seletor, **G** novamente (ou segurar) para mudar de alien, **J** para transformar!

---

## 🔊 Sobre os sons (importante!)

Os arquivos `.ogg` em `src/main/resources/assets/omnitrix/sounds/` são **silenciosos** (placeholders) por motivos de direitos autorais — não posso distribuir os sons originais do desenho.

Para experiência **fiel ao desenho**:
1. Baixe os sons reais do Omnitrix em:
   - [Ben 10 Soundeffects Wiki](https://soundeffects.fandom.com/wiki/Ben_10)
   - [Myinstants — Omnitrix](https://www.myinstants.com/en/search/?name=omnitrix)
   - YouTube ([este vídeo tem a coleção completa](https://www.youtube.com/watch?v=rNd4e-tJ_2Q))
2. Converta para `.ogg` (use [convertio.co](https://convertio.co) no celular).
3. Substitua os arquivos com o **mesmo nome**:
   - `omnitrix_open.ogg` — quando aperta G
   - `omnitrix_cycle.ogg` — quando muda de alien
   - `omnitrix_transform.ogg` — quando transforma (o famoso "TENNNNN!")
   - `omnitrix_detransform.ogg` — quando volta ao normal
   - `omnitrix_equip.ogg` — quando o Omnitrix se prende no braço
   - `omnitrix_fail.ogg` — som de erro/cooldown
   - `meteorite_fall.ogg` — estrondo de meteoro caindo
   - `meteorite_impact.ogg` — impacto

---

## 📁 Estrutura do projeto

```
omnitrix-mod/
├── build.gradle              # Configuração do build
├── gradle.properties         # Versões (MC 1.19.4, Fabric, GeckoLib)
├── settings.gradle
├── gradlew, gradlew.bat      # Scripts do Gradle Wrapper
├── gradle/wrapper/           # JAR do Gradle Wrapper
├── .github/workflows/build.yml  # CI automático
├── src/main/
│   ├── java/com/bentennyson/omnitrix/
│   │   ├── OmnitrixMod.java               # Entry point principal
│   │   ├── common/
│   │   │   ├── OmnitrixState.java         # Estado do jogador (NBT)
│   │   │   ├── OmnitrixStateProvider.java
│   │   │   ├── OmnitrixDataAccessor.java
│   │   │   ├── alien/Alien.java           # Enum dos 10 aliens
│   │   │   ├── event/                     # Persistência, tick, meteorito
│   │   │   ├── network/OmnitrixNetworking.java
│   │   │   └── registry/                  # Sons, items
│   │   ├── client/
│   │   │   ├── OmnitrixModClient.java     # Entry point do cliente
│   │   │   ├── ClientOmnitrixData.java
│   │   │   ├── keybind/OmnitrixKeybinds.java
│   │   │   ├── gui/                       # Holograma, flash verde/vermelho
│   │   │   ├── render/OmnitrixArmRenderer.java
│   │   │   └── net/OmnitrixClientNetworking.java
│   │   └── mixin/PlayerEntityMixin.java   # Anexa Omnitrix ao Player
│   └── resources/
│       ├── fabric.mod.json
│       ├── omnitrix.mixins.json
│       └── assets/omnitrix/
│           ├── icon.png
│           ├── sounds.json
│           ├── sounds/*.ogg               # PLACEHOLDERS — substituir
│           ├── textures/
│           │   ├── entity/omnitrix.png    # Relógio (verde, ampulheta preta)
│           │   └── gui/                   # Ring + ícones de aliens
│           ├── geo/omnitrix.geo.json      # Modelo 3D (GeckoLib)
│           ├── animations/omnitrix.animation.json
│           └── lang/                      # pt_br e en_us
└── README.md (este arquivo)
```

---

## ⚖️ Aviso legal

Este é um **mod de fã não oficial**, sem afiliação com **Cartoon Network / Man of Action / Warner Bros**. Ben 10™ e todos os personagens são marcas registradas de seus respectivos donos. Distribuído sob CC-BY-NC-SA 4.0 (não comercial).

---

## 💚 Créditos da pesquisa

- Lista dos 10 aliens originais: [Ben 10 Wiki — Original Series Transformations](https://ben10.fandom.com/wiki/Category:Original_Series_Transformations)
- Mecânica do Omnitrix clássico: episódio "And Then There Were 10" (Cartoon Network, 27/12/2005)
- GeckoLib: [bernie-g/geckolib](https://github.com/bernie-g/geckolib)
- Fabric API docs: [fabricmc.net/wiki](https://fabricmc.net/wiki/)

**Vamos juntos fazer o melhor mod do Omnitrix do Minecraft!**
