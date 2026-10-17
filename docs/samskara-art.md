# Samskara illustrated world

Generated with the built-in imagegen tool, September 25, 2026.
The user's `Mystical Forest Growth Dashboard.png` supplied environment style and composition guidance only. No screenshot UI is used in the app.

The original approved forest and sapling are preserved in
`design/source-art/samskara/` as tier-one PNG sources. The Android runtime now
uses optimized `art_samskara_world_t01.webp` and `art_samskara_motif_t01.webp`
in `app/src/main/res/drawable-nodpi/`, alongside two further forest worlds
and eight further transparent tree stages. See `design/skin-art-prompts/samskara.md`
for source versions and tier progression. Compose draws the selected environment,
tree, permanent level-dependent growth of the painted tree, and independent daily light.
All dashboard text and state remain Compose UI. Assets are density-independent,
with no runtime image download, full-screen blur, or new dependency.

## Environment prompt

Use case: stylized-concept. Generate a production environment background asset for an Android game-like wellness dashboard, not a mockup. Reference image is atmosphere/composition guidance only. Portrait 1024x1536. Exquisite cinematic painted-realism enchanted nocturnal forest lake, deep navy teal shadows, intricate moss and distant pine silhouettes with layered depth, pale moon in upper right, volumetric silver-blue moonlight and wisps of mist, water reflections in lower 20%. Center is EMPTY to later composite a separate small sapling: leave an open clearing and a low mossy island at x50%, y83%, no central tree, no foreground plant obscuring center. Frame with tall detailed trees at left and right edges. Upper central half is airy dark misty negative space. Restrained luminous jade accents and a few tiny light points. Match the richness, textures, luminous depth of the reference scene. No UI whatsoever: no words, text, numbers, letters, icons, bars, cards, borders, ornamental rings, watermark. Only environment artwork. Dark edges blending toward nearly black blue green.

## Sapling prompt

Use case: stylized-concept. Asset type: isolated transparent foreground sprite for premium mystical forest Android dashboard. Create ONE delicate young sapling with tiny moss-covered root mound, photoreal painterly fantasy botanical illustration. TRUE transparent alpha background, no environment, no ground plane outside small root mound, no rectangle or checkerboard. Thin slightly twisting dark green-brown trunk, three fine asymmetric branches, just 12-16 individual sparse pointed jade/lime leaves with detailed veins and brilliant pale green moonlit edges. Tiny roots wrap dark wet mossy stones in a compact low mound. Subtle green bioluminescence along bark cracks and roots, glowing leaves with restrained soft bloom. Beginning of growth: no canopy, no mature tree, no dense foliage. Full sapling and all roots visible centered with 10% transparent margin. Portrait 1024x1536; sapling tip at 12% height, root mound at 90%, branches spread 60% of width. Light from upper right pale blue moonlight plus living green glints. Rich realistic textures matching a high-end fantasy game environment. No text, no UI, no ornaments, no ring, no watermark.
