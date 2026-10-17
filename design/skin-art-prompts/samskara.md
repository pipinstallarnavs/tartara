# Saṃskāra — growth in the moonlit forest

## Source and versions

Tier-one assets are the app's previously approved `samskara_sapling.png` and `samskara_forest.png`, copied without retouching to `design/source-art/samskara/samskara_motif_t01_v01.png` and `samskara_world_t01_v01.png` before replacing the old runtime PNGs. Their original image-generation prompts are in `docs/samskara-art.md`. The subsequent eight motif states and two later worlds were generated with OMP `generate_image` and versioned below. The original model outputs of tiers 2–9 remain in `design/source-art/samskara/gpt-originals/`; the canonical foreground WebPs in the parent directory have a cleaned transparent edge to prevent alpha-fringe artifacts. Do not treat the archived originals as an additional set of tier art.

Generation contract for t02–t09: premium painterly botanical cutout, one centered tree of the same species, upright full portrait source at requested 1024×1536, true transparent background, separately compositable with the forest, no text/people/UI; each tier gains durable structural growth rather than a transient glow. Eight generated tree outputs were visually reviewed together; later worlds are opaque night-forest environments, not mockups. The exact request text is not fully preserved in the CLI session history (arguments were truncated by logging); these are the recorded direction and resulting approved versions, not invented verbatim prompts.

| Tier | Approved source | Durable growth |
| --- | --- | --- |
| 1 — Ārambha | `samskara_motif_t01_v01.png` | Original small rooted sapling with sparse leaves. |
| 2 — Abhyāsa | `samskara_motif_t02_v01.webp` | Stronger rooted young shoot, more leaves. |
| 3 — Tapas | `samskara_motif_t03_v01.webp` | Branching young tree, thicker trunk. |
| 4 — Sthiti | `samskara_motif_t04_v01.webp` | Stable tree with spreading roots and growing canopy. |
| 5 — Saṃskāra | `samskara_motif_t05_v01.webp` | Dense branching, visibly mature crown. |
| 6 — Dhyāna | `samskara_motif_t06_v01.webp` | Broader, more interwoven canopy and roots. |
| 7 — Sthitaprajña | `samskara_motif_t07_v01.webp` | Large established luminous tree. |
| 8 — Siddhi | `samskara_motif_t08_v01.webp` | Deep intertwined trunk, expansive crown. |
| 9 — Svabhāva | `samskara_motif_t09_v01.webp` | Monumental mature tree with complex roots and full branches. |

Worlds: `samskara_world_t01_v01.png` is the original approved forest-water backdrop for tiers 1–3. `samskara_world_t04_v01.webp` develops the misty moonlit forest for tiers 4–6; `samskara_world_t07_v01.webp` is the fully established forest lake for tiers 7–9. Foreground bitmaps remain separate from environments and all text remains in Compose. The painted tree grows continuously through each tier as held level rises; today's completion only adjusts transient light. The final tier has eleven intermediate steps through level 100.

Android resources are resized 800×1200 WebP derivatives `art_samskara_motif_t01.webp` … `art_samskara_motif_t09.webp` and `art_samskara_world_t01.webp`, `_t04.webp`, `_t07.webp`; `art_samskara_preview.webp` is the 144×192 settings thumbnail. The runtime renderer decodes only the selected tier motif and world, not every source image.
