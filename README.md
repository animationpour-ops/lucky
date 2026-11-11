# Lucky Brush — mod Forge 1.21.1 (projet perso)

Thème **Lucky (vert)** pour une vidéo « Minecraft mais ». Tout est basé sur le système d'archéologie vanilla (le brush).

## Le système de brush
1. Tu craftes un **brush normal** (recette vanilla : plume + lingot de cuivre + bâton).
2. Chaque **bloc suspect terminé** (sable/gravier suspect des ruines) compte : l'infobulle du brush affiche `x/10`, la barre d'action aussi.
3. Au 10e bloc, le brush devient un **Brush Lucky** (cérémonie : pilier de lumière, foudres, titre à l'écran).
4. Avec le Brush Lucky, **chaque bloc terminé donne un objet cheat vanilla** (blaze rod, ender pearl, diamant, pommes dorées, totem, élytres, TNT, étoile du Nether…).
5. **Tous les 3 blocs**, il donne un **objet Lucky custom** (épée, arc, briquet, pioche, pelle, casque, plastron, jambières, bottes) — sans doublon tant que tu n'as pas tout eu.

Le Brush Lucky reste un vrai brush (même vitesse que le vanilla) avec des particules de trèfles en plus.

## Les objets (2 pouvoirs chacun)
| Objet | Pouvoir 1 | Pouvoir 2 |
|---|---|---|
| **Épée** | *Foudre Jackpot* — chaque coup : éclair vert qui rebondit sur 6 monstres, 1 chance sur 5 de JACKPOT (x3) | *Dash Fantôme* (clic droit) — charge dans un tunnel d'anneaux qui lacère tout |
| **Arc** (tendu à fond) | *Rail Jackpot* — rayon perforant de 110 blocs + hélices + explosion | *Trou Noir* (accroupi) — vortex de portail qui aspire 4,5 s puis implose |
| **Briquet** | *Tornade de Feu Vert* (clic droit) — cyclone qui avance, aspire, enflamme, arrache le décor | *Pluie de Météores* (accroupi) — 16 météores sur la zone visée |
| **Pioche** | *Séisme Sphérique* (clic droit sur un bloc) — implosion puis sphère de 5,5 blocs pulvérisée | *Foreuse Lucky* (accroupi) — tunnel 3,5 de large sur 48 blocs |
| **Pelle** | *Raz-de-Marée* (clic droit) — mur d'énergie qui laboure 24 blocs et projette tout | *Impact Gravitationnel* (accroupi) — bond puis chute qui creuse un cratère de 7 blocs |
| **Armure** | *Aura Porte-Bonheur* (passif) — effets par pièce ; ensemble complet = Force II + trèfles en orbite | *Nova Jackpot* — ensemble complet, accroupi 2 s : onde de choc de 24 blocs, foudres, surpuissance 10 s |

Tous les minerais/blocs détruits par les pouvoirs vont directement dans l'inventaire (les blocs communs en trop sont perdus si l'inventaire est plein, les objets précieux sont jetés au sol).

## Textures et modèles
- Texture **portail de l'End en vert**, **animée** (étoiles qui défilent, scintillent + reflet qui balaie la lame) sur tous les objets et le Brush Lucky.
- **Armure : modèle de la Netherite** (modèle vanilla, seule la texture change). Les textures d'armure portées ne peuvent pas être animées dans Minecraft : l'armure compense avec une aura de particules.
- Particules maison (9 types : étincelle, lueur, anneau, cristal, traînée, trèfle, portail, flash, braise) + moteur de VFX (faisceaux, éclairs, hélices, vortex, ondes de choc, cercles runiques).
- Tout est régénérable avec `python3 tools/gen_assets.py` (Pillow + numpy).

## Compiler (GitHub Actions)
1. Crée un dépôt GitHub, envoie tout le contenu de ce dossier (y compris `.github/workflows/build.yml`).
2. Onglet **Actions** → le workflow *Build mod* se lance → télécharge l'artefact `lucky-mod-jar` → mets le `.jar` dans `mods/` (Forge 1.21.1).

Le code n'a **pas pu être compilé ici** (pas d'accès à Maven/Forge depuis mon environnement) : si le build échoue, copie-colle le log et je corrige.

## Tester vite
- `/give @s luckymod:lucky_brush` ou prends-le dans l'onglet créatif *Lucky Brush*.
- Pour tester le brush : `/give @s brush` et `/give @s suspicious_sand` (pose, puis brosse). Une ruine du désert, un puits du désert, des ruines océaniques chaudes ou des ruines de sentier en contiennent.
- Les cooldowns : épée 3,5 s, arc 2,5 s (rail) / 11 s (trou noir), briquet 8,5 s / 13 s, pioche 4 s / 7 s, pelle 5,5 s / 9 s, nova 20 s.
