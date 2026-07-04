/// Base de conhecimento do MCOC embutida no app.
/// Isso é injetado no prompt da IA para ela ter conhecimento
/// ATUALIZADO do meta, mesmo se a versão do modelo não tiver.
class MCOCKnowledgeBase {
  static const String systemKnowledge = '''
### PROGRESSION MILESTONES (ordem de progressão)
1. **Iniciante** → Complete Act 1-4
2. **Uncollected** → Complete Act 5 (bata no Collector)
3. **Cavalier** → Complete Act 6 (bata no Grandmaster)
4. **Thronebreaker** → Rank 3 em um 6★ + Act 6 completo
5. **Paragon** → Rank 2 em um 7★ (ou R5 em 6★) + Act 7 completo
6. **Valiant** → Rank 3 em um 7★ + Act 8 completo + Necropolis
7. **Endgame** → Everest content: Abyss of Legends, Necropolis, Battlegrounds VT

### GOD-TIER CHAMPS (Meta Atual 2025)
**Melhores Attackers gerais:**
- **Serpent** (Cosmic) - dano insano, muta debuffs, poder rampante
- **Photon** (Cosmic) - true damage via power gain
- **Hercules** (Cosmic) - imortal, potência bruta, God-tier eterno
- **Onslaught** (Skill) - buffs infinitos, prowess stack
- **Kushala** (Mystic) - falcon buffs, burn, boa contra Mystic dispel
- **Shathra** (Skill) - counter de buff, poison
- **Kate Bishop** (Skill) - true focus, precisão massiva
- **Bullseye** (Skill) - bleed insano, contra classe skill
- **Nimrod** (Tech) - adapta imunidades, buff control
- **Wiccan** (Mystic) - true damage massivo
- **Nick Fury** (Skill) - clássico, 3 lives, evade counter
- **Doctor Doom** (Mystic) - power control, coldsnap, aura
- **Aegon** (Skill) - fight streak, dano exponencial
- **Kate Bishop, Shathra, Onslaught** - top skill 2025

**Melhores Defenders (AW/BG):**
- **Serpent** - counter difícil
- **Kingpin** - passive fury massivo
- **Photon** - anti-power gain
- **Killmonger** - bleed reflection
- **Mordo** - buff removal + power lock
- **Domino** - crit + auto-block

**Utility especial:**
- **Kitty Pryde** (Mystic) - phase, ignora habilidades defensivas — ESSENCIAL pra content difícil
- **Doctor Doom** - power control
- **Photon** - power gain counter
- **Mister Negative** - resistência a debuffs

### CLASSES E COUNTERS (rock-paper-scissors)
- **Cosmic** > **Tech** > **Mutant** > **Skill** > **Science** > **Mystic** > **Cosmic**
- **Superior** é neutro (roda em todas)

### NODES COMUNS E COUNTERS
- **Unblockable/Unstoppable** → champs com nullify (Doom, Magneto), evade (Nick Fury), phase (Kitty), incinerate ignore
- **Buffet** → champs sem buff (Nick Fury, Aegon, Corvus, Warlock robô)
- **Encroaching Stun** → champs de missão rápida
- **MD (Mystic Dispersion)** → cuidado com Mystic no defender; use nullify pra abusar
- **Aggression** → champs de dano puro
- **Do You Bleed?** → champs de bleed (Bullseye, Blade, Killmonger)
- **Biohazard** → champs poison ignore (Domino, Man-Thing, Mordo com sig)
- **Power Gain** → power drain/control (Doom, Vision, Photon)
- **Masochism** → não pare de atacar (Aegon, Herc, Corvus)
- **Limber** → champs sem bleed/dano físico dependente
- **Willpower** → drain de HP com passive debuffs
- **Caltrops** → não faça bloqueio (Herc imortal, Kitty phase)
- **Flare/Ignition** → Mister Negative, immune fire, ou power control

### CONTENT DIFFICULTY ORDER
1. Act 4-5 (Uncollected path)
2. Act 6 (Cavalier)
3. Variants 1-8 (mais fáceis: V1, V4, V6)
4. Act 7 (Paragon path)
5. Book 1 completion
6. Everest: **Abyss of Legends** (100 lanes), **Labyrinth of Legends**
7. Act 8 (Valiant path)
8. **Necropolis** (com Herc é o mais fácil, requer 6★ R4+ meta champs)
9. Book 2 completion
10. **Carina's Challenges** (endgame)

### RANK-UP GEMS / RECURSOS
- **T5CC (rank 5 class catalyst)** = 6★ R4 ou 7★ R2
- **T6CC** = 7★ R3 (Valiant milestone)
- **T3 Alpha** = geral rank up
- **T6B (basic)** = mais escasso; guarde pros melhores
- **Sig Stones** = priorize champs que MUDAM com awakening (Aegon, Ægon, Nick Fury, Doom, Kitty, Corvus, Doom, Onslaught)

### CHAMPS QUE **REQUEREM** AWAKENING (não sobem sem)
- Aegon (persistence charges), Nick Fury (LMD lives), Corvus (glaive charges), Kitty (phase), Ægon, Torch (nova), Aegon, Namor (fury on parry), Doom (aura), Wolverine (regen)

### CHAMPS BONS SEM AWAKENING (unduped)
- Hercules, Shang-Chi, Serpent, Photon, Onslaught, Kate Bishop, Shathra, Wiccan, Silk, White Mag, Guillotine 2099

### AQ (ALLIANCE QUEST) - meta
- Mapa 8 = maior recompensa (top alliances)
- Mapa 7 = alliance médio-alto
- Mapa 6 = casual mas ainda produtivo
- Sempre coloque um defender difícil na sua linha (Serpent, Kingpin, Photon)

### AW (ALLIANCE WAR) - meta
- **Diamond/Platinum tiers** exigem 6★ R4+ awakened defenders
- Ataca com Herc, Kitty, Shathra, Onslaught
- Defende com Serpent, Kingpin, Photon, Killmonger, Mordo

### BATTLEGROUNDS (BG) - meta PvP
- Draft pick: **Serpent, Onslaught, Kate, Shathra, Photon, Wiccan, Herc, Kitty**
- Ban: campeões que quebram matchup
- Season atual: foca em champs de dano rápido

### RECURSOS - PRIORIDADE DE USO
1. **Ouro** → gaste em rank ups
2. **T5CC/T6CC** → só nos MELHORES champs (verifique tier list antes)
3. **Sig stones** → champs que MUDAM com awakening
4. **Unidades** → energy refills durante eventos, mastery cores, crystals de meta
5. **Odin's** → guarde pra Cyber Weekend (dezembro)

### DICAS DE PROGRESSÃO POR TÍTULO
**Se você é INICIANTE:**
1. Complete Act 1-4 (ganha 5★ arena tokens)
2. Foque em 4★/5★ básicos primeiro
3. Não gaste unidades em crystals — junte pra Cyber Weekend
4. Aprenda **parry** e **intercept** (fundamental!)

**Se você é UNCOLLECTED:**
1. Foque em subir prestígio (rank ups em 5★/6★)
2. Faça Variants 1, 4, 6 (mais fáceis)
3. Junte 6★ shards pra abrir crystals
4. Comece Act 6.1 e 6.2

**Se você é CAVALIER:**
1. Rank 3 em um 6★ META (Herc, Serpent, Photon, Onslaught) → Thronebreaker
2. Faça Book 1 completion
3. Junte T5CC pra próximos rank ups
4. Comece a fazer AW pra alliance rewards

**Se você é THRONEBREAKER:**
1. Foque em 7★ pulls
2. Rank 2 em um 7★ meta → Paragon
3. Faça Act 7
4. Comece Abyss of Legends (com Herc R5 fica fácil)

**Se você é PARAGON:**
1. Rank 3 em um 7★ → Valiant
2. Faça Necropolis (essencial pra Valiant)
3. Battlegrounds Victory Track
4. Book 2

**Se você é VALIANT:**
1. Everest content: Carina's Challenges
2. Battlegrounds Gladiator's Circuit
3. Rank 3 múltiplos 7★s
4. AW Diamond tier
''';
}
