# Fuel-preserving battery pickup

Sneak-right-click an isolated Gateway Flux Battery with a Create wrench to pick it up with its fuel. The block is removed without ordinary loot, its world tank is cleared, and exactly one item receives the fuel. A full inventory drops that item at the player. Charged items are unstackable and show their fuel and mB amount in the tooltip. Both Teleport Essence and Liquid Eye of Ender are supported.

Placement transfers the saved fuel into one isolated battery. Charged placement consumes one item in survival and creative; it never auto-fills an array layer. Empty items retain normal stacking, creative placement and layer-placement behavior. Charged placement beside any battery, or across an unresolved neighboring chunk boundary, is refused without consuming the item. Other batteries can subsequently be added through ordinary empty-item placement.

## Ownership and deferred array relocation

Array fuel remains owned by the existing controller tank. Fuel-preserving pickup refuses controllers, members, and adjacent batteries until the clicked block is an isolated 1x1x1 controller with all six neighboring positions loaded and free of batteries. It does not divide or copy the shared array fuel into a per-block item.

Normal mining, explosions, and Create's array split/resize behavior are unchanged: their drops do not gain stored fuel. Removing capacity can discard fuel under the existing split/resize rules, so drain arrays before dismantling them. This PR does not implement fueled array relocation or alter recipes. Drain an array, dismantle it, and relocate empty blocks; isolated fuel-bearing singles can use the new wrench path.

Only the fuel payload is retained; window/gauge placement uses existing defaults. Placement validates the two accepted fuel types and bounds restored amount to the configured single-block capacity. Fuel is kept in item custom data, not a copied controller/block-entity tag.

## Validation (2026-10-28)

`gradlew.bat build runData runGameTestServer --console=plain` passed, including all 30 required GameTests.

Three new GameTests cover both fuels, empty/partial/full tanks, survival and creative, three repeated pickup/place cycles per combination, item and placed-tank serialization, one item owning all retained fuel, array-adjacent refusal, charged placement refusal without consumption, and full-inventory fallback producing exactly one charged drop.

Multiplayer and third-party block-moving tools have not been exercised. Those tools are outside this pickup path. Recipes are unchanged.

Targets `1.2-dev`; https://github.com/lotlethtroll/cesg/issues/15.

Live client at Auto GUI scale: sneak-wrenched an isolated 3000 mB Teleport Essence battery in the creative QA world; the world block disappeared and exactly one battery appeared in the hotbar. Its tooltip reported the same 3000 mB. Actual placement and creative consumption were exercised by the GameTests; the live placement attempt was inconclusive because mouse capture moved the aim. Temporary toggle-sneak was restored afterward.
