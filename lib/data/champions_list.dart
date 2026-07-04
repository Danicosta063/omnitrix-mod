/// Lista dos principais campeões do MCOC com classe, tier e role.
/// Você pode editar/expandir essa lista conforme necessário.
class ChampionsList {
  static const List<Map<String, String>> all = [
    // COSMIC
    {'name': 'Hercules', 'class': 'Cosmic', 'tier': 'God', 'role': 'Attacker'},
    {'name': 'Serpent', 'class': 'Cosmic', 'tier': 'God', 'role': 'Hybrid'},
    {'name': 'Photon', 'class': 'Cosmic', 'tier': 'God', 'role': 'Hybrid'},
    {'name': 'Captain Marvel (Movie)', 'class': 'Cosmic', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Corvus Glaive', 'class': 'Cosmic', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Angela', 'class': 'Cosmic', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Hyperion', 'class': 'Cosmic', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Medusa', 'class': 'Cosmic', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Thanos', 'class': 'Cosmic', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'King Groot', 'class': 'Cosmic', 'tier': 'Good', 'role': 'Defender'},
    {'name': 'Odin', 'class': 'Cosmic', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Knull', 'class': 'Cosmic', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Cosmic Ghost Rider', 'class': 'Cosmic', 'tier': 'Great', 'role': 'Hybrid'},

    // TECH
    {'name': 'Nimrod', 'class': 'Tech', 'tier': 'God', 'role': 'Hybrid'},
    {'name': 'Warlock', 'class': 'Tech', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Iron Man Infinity War', 'class': 'Tech', 'tier': 'Great', 'role': 'Hybrid'},
    {'name': 'Guardian', 'class': 'Tech', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Peni Parker', 'class': 'Tech', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Doctor Doom', 'class': 'Mystic', 'tier': 'God', 'role': 'Hybrid'},
    {'name': 'Silver Centurion', 'class': 'Tech', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Vision Aarkus', 'class': 'Tech', 'tier': 'Good', 'role': 'Defender'},
    {'name': 'Sentinel', 'class': 'Tech', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Iron Patriot', 'class': 'Tech', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Hulkbuster', 'class': 'Tech', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Nick Fury', 'class': 'Skill', 'tier': 'God', 'role': 'Attacker'},

    // MUTANT
    {'name': 'Onslaught', 'class': 'Mutant', 'tier': 'God', 'role': 'Attacker'},
    {'name': 'Professor X', 'class': 'Mutant', 'tier': 'Great', 'role': 'Hybrid'},
    {'name': 'Magneto Red', 'class': 'Mutant', 'tier': 'Great', 'role': 'Hybrid'},
    {'name': 'Omega Red', 'class': 'Mutant', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Apocalypse', 'class': 'Mutant', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Storm Pyramid X', 'class': 'Mutant', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Sunspot', 'class': 'Mutant', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Colossus', 'class': 'Mutant', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Wolverine', 'class': 'Mutant', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Dazzler', 'class': 'Mutant', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Domino', 'class': 'Mutant', 'tier': 'Great', 'role': 'Hybrid'},
    {'name': 'Bishop', 'class': 'Mutant', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Mister Sinister', 'class': 'Mutant', 'tier': 'Good', 'role': 'Attacker'},

    // SKILL
    {'name': 'Shang-Chi', 'class': 'Skill', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Aegon', 'class': 'Skill', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Kate Bishop', 'class': 'Skill', 'tier': 'God', 'role': 'Attacker'},
    {'name': 'Bullseye', 'class': 'Skill', 'tier': 'God', 'role': 'Attacker'},
    {'name': 'Shathra', 'class': 'Skill', 'tier': 'God', 'role': 'Attacker'},
    {'name': 'Karnak', 'class': 'Skill', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Falcon', 'class': 'Skill', 'tier': 'Good', 'role': 'Hybrid'},
    {'name': 'Killmonger', 'class': 'Skill', 'tier': 'Good', 'role': 'Hybrid'},
    {'name': 'Blade', 'class': 'Skill', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Elektra', 'class': 'Skill', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Black Widow Deadly Origin', 'class': 'Skill', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Winter Soldier', 'class': 'Skill', 'tier': 'Good', 'role': 'Attacker'},

    // SCIENCE
    {'name': 'Hulk (Immortal)', 'class': 'Science', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Toad', 'class': 'Science', 'tier': 'Great', 'role': 'Hybrid'},
    {'name': 'Void', 'class': 'Science', 'tier': 'Great', 'role': 'Hybrid'},
    {'name': 'Quicksilver', 'class': 'Science', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Human Torch', 'class': 'Science', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Red Guardian', 'class': 'Science', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Kingpin', 'class': 'Science', 'tier': 'God', 'role': 'Defender'},
    {'name': 'Titania', 'class': 'Science', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'She-Hulk', 'class': 'Science', 'tier': 'Good', 'role': 'Attacker'},

    // MYSTIC
    {'name': 'Kitty Pryde', 'class': 'Mystic', 'tier': 'God', 'role': 'Attacker'},
    {'name': 'Kushala', 'class': 'Mystic', 'tier': 'God', 'role': 'Attacker'},
    {'name': 'Wiccan', 'class': 'Mystic', 'tier': 'God', 'role': 'Attacker'},
    {'name': 'Tigra', 'class': 'Mystic', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Mordo', 'class': 'Mystic', 'tier': 'Great', 'role': 'Defender'},
    {'name': 'Man-Thing', 'class': 'Mystic', 'tier': 'Great', 'role': 'Hybrid'},
    {'name': 'Longshot', 'class': 'Mystic', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Symbiote Supreme', 'class': 'Mystic', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Guillotine 2099', 'class': 'Mystic', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Mister Negative', 'class': 'Mystic', 'tier': 'Great', 'role': 'Hybrid'},
    {'name': 'Sasquatch', 'class': 'Mystic', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Werewolf by Night', 'class': 'Mystic', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'White Magneto', 'class': 'Mutant', 'tier': 'God', 'role': 'Hybrid'},
    {'name': 'Silk', 'class': 'Science', 'tier': 'Great', 'role': 'Attacker'},

    // SUPERIOR
    {'name': 'Kang', 'class': 'Superior', 'tier': 'Good', 'role': 'Defender'},

    // 7-STAR EXCLUSIVOS FORTES
    {'name': 'Cassie Lang', 'class': 'Tech', 'tier': 'Great', 'role': 'Attacker'},
    {'name': 'Aarkus', 'class': 'Cosmic', 'tier': 'Good', 'role': 'Defender'},
    {'name': 'Zemo', 'class': 'Skill', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Adam Warlock', 'class': 'Cosmic', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Sage', 'class': 'Mutant', 'tier': 'Great', 'role': 'Hybrid'},
    {'name': 'Enchantress', 'class': 'Mystic', 'tier': 'Great', 'role': 'Hybrid'},
    {'name': 'Bloodline', 'class': 'Mutant', 'tier': 'Good', 'role': 'Attacker'},
    {'name': 'Cable Deadpool', 'class': 'Mutant', 'tier': 'Good', 'role': 'Attacker'},
  ];
}
