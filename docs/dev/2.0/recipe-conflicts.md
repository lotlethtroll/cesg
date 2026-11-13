## Recipe conflicts with Create addons and vanilla

A player reported that with Create Aeronautics installed, crushing End Stone gave CESG's sand and
pearl dust instead of Aeronautics' End Stone Powder, which blocks Levitite.

Root cause: `RecipeManager.getRecipeFor` returns the first match in a list ordered by recipe-ID hash
buckets, and the bucket count depends on how many recipe files the whole pack loads. When two recipes
accept the same input, the winner depends on the pack. Create 6.0.10 crushing wheels try crushing,
then fall back to milling; the millstone uses milling only. Verified in Create bytecode and Minecraft
1.21.1 source. Reproduced with real Aeronautics 1.3.2 jars: Aeronautics won with 3,041 recipes, and
CESG won once the pack passed 6,144 recipes.

Audit (2026-11-13): `RecipeCompatTest.cesgRecipesHaveNoSameInputTwin` builds the input of every CESG
recipe and lists any other recipe of the same type that accepts it. Run with only Create installed,
then with 59 Create addons from Modrinth plus required dependencies (including Quark and
Supplementaries), 20,788 recipes in total. TFMG, Power Grid, Numismatics and Create Central Kitchen
cannot start in the dev harness and were scanned statically; they are clean.

Findings and fixes:

- End Stone crushing is also claimed by Aeronautics, Create Sifting and Create Ultimate Factory, and
  milling by Create Sifting. Washing was free in every addon, so pearl dust moved to
  `create:splashing`. This stops crushing-wheel pearl farms, which is why the release is 2.0.0.
- The enhanced box dye recipes collided with vanilla `minecraft:shulker_box_coloring`, which accepts
  any `ShulkerBoxBlock`. Depending on the pack, dyeing produced an empty box (CESG recipe) or a
  vanilla box (vanilla recipe); already-colored boxes always became vanilla boxes. Both lose the
  box's data. `EnhancedAwareShulkerBoxColoring` now replaces the vanilla recipe under its own ID. Mod
  data always loads over vanilla data, so the replacement holds in every pack. JEI and EMI show
  display-only entries for each color.
- `polished_end_stone` used vanilla's End Stone Bricks pattern (2×2 End Stone). It is now 2×2 Smooth
  End Stone; the stonecutter recipe is unchanged. Approved by Andrew.
- Clean in all runs: ender pearl crushing, sandstone haunting, End Stone and Purpur smelting, and all
  other crafting recipes.
- Create Food disables vanilla cake, which broke `batchRefillPreservesCakeBuckets`. The test now
  skips packs without the vanilla cake recipe.

Validation (2026-11-13):

- Gradle build, unit tests and Ponder verification pass.
- All 33 GameTests pass with only Create installed, and again with the 59-addon set on NeoForge
  21.1.266.
- Dyeing tier-3 boxes holding 64 diamonds (uncolored, blue and red) keeps tier and contents.
  Vanilla boxes dye as before.

Not verified: JEI/EMI dye entries in a running client, and End Stone washing with a physical fan
(the recipe lookup is tested).

To re-run the audit, put addon jars in `run/gametest/mods` and run
`./gradlew runGameTestServer -Pneo_version=21.1.266` (several addons need NeoForge 21.1.235–21.1.252).
