# Astra — celestial arrows and observatory

The nine permanent arrow images are generated sources under `design/source-art/astra/`. Source generation used OMP `generate_image`, `openai-codex/gpt-6-sol` as the calling model and `gpt-image-2` / `gpt-image-2-codex` as the returned image model. Each arrow requested a portrait 2:3 1024×1536 isolated full-length upright arrow with genuine alpha, violet crystal and antique gold metal, no scene, text, UI or people. The source files are unflattened and retain alpha. Every arrow was visually inspected and all nine decoded images differ; alpha spans 0–253/254. Runtime derivatives are 800×1200 WebP in `drawable-nodpi`.

| Tier | Approved source | Generation direction |
| --- | --- | --- |
| 1 — Agneyastra | `astra_motif_t01_v01.webp` | Simple ornate celestial arrow, gold and amethyst, barely awakened warmth. |
| 2 — Vayavyastra | `astra_motif_t02_v01.webp` | Edit tier-one lineage: an airy feathered, wind-etched silver/gold arrowhead and refined fletching. |
| 3 — Nagastra | `astra_motif_t03_v01.webp` | Edit tier-one lineage: serpentine filigree, richer crystal and coiled metalwork. |
| 4 — Varunastra | `astra_motif_t04_v01.webp` | Edit tier-three lineage: water-pronged head, indigo veins and wave-carved shaft. |
| 5 — Vajra | `astra_motif_t05_v01.webp` | Edit tier-three lineage: forked conductive lightning head with embedded white-violet crystal channels. |
| 6 — Garudastra | `astra_motif_t06_v01.webp` | Edit tier-three lineage: wing-like swept head, amethyst core and broad feathered metal fletching. |
| 7 — Brahmastra | `astra_motif_t07_v01.webp` | Edit tier-six lineage: trident-like star core and fine quillwork. |
| 8 — Pashupatastra | `astra_motif_t08_v01.webp` | Edit tier-six lineage: nested nebula crystal in a gold crescent and engraved shaft. |
| 9 — Narayanastra | `astra_motif_t09_v01.webp` | Edit tier-six lineage: star-and-orbit arrowhead, jewel inlays and radial gold fletching. |

`astra_world_t01_v01.webp` is the approved full-bleed painted cosmic observatory: antique starmap mechanisms, celestial architecture, nebula depth, and a dark vacant center for the independently composited arrow. The model returned 941×1672 despite a 1024×1536 request. The Android derivative is center-cropped to 800×1200. One environment serves all nine tier objects; tier four and seven add permanent Compose orbit geometry. A request for two additional scenes failed with `usage_limit_reached`; no ungenerated worlds are listed or implied as approved. The 72-tier-art target is still met: backgrounds are not the nine tier artworks. Existing generated arrows, not fake slots or placeholders, supply all nine tier silhouettes.

Runtime names: `art_astra_motif_t01.webp` … `art_astra_motif_t09.webp`, `art_astra_world_t01.webp`, and `art_astra_preview.webp` (precomposited 144×192 selection tile). The world was generated once; the arrow motifs were made in a first single request, two single edits, and two three-image prompt batches. Preserve these source versions when recompressing; do not regenerate in place.
