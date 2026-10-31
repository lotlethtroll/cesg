# Fluid routing

Fluid routing is an independent, opt-in Core setting, off for old and new worlds. With it off, fluids keep following the active channel even while item routing is enabled. Both routing toggles respect the existing server fan-out permission.

Each channel has nine fluid ghost selections and its own whitelist/blacklist flag. In the existing filter editor, switch Items/Fluids; click a fluid slot with a filled fluid container to copy its fluid selection without consuming anything, or with an empty cursor to clear it. Non-fluid items and empty containers do not replace a selection. Matching uses fluid identity and components, not container item identity or amount.

Enabled fluid routing selects the first bound accepting channel in ascending channel order. Empty whitelist accepts nothing; empty blacklist is a catch-all, best placed after specific routes. Unmatched fluids stay buffered. A full, incompatible, unloaded or missing selected destination does not fall through to another route. Items remain governed by the independent item-routing toggle.

The Port's existing shared per-flush fuel budget charges once per fuel type that actually moves cargo, using the selected destination's dimension and existing travel reserve. Failed moves refund the reservation. No fluid enters gateway fuel tanks. Filters, independent toggles, and buffers persist across reloads.

Targets `1.2-dev`; https://github.com/lotlethtroll/cesg/issues/9.

## Implementation and validation (2026-10-28)

Implemented in ChannelFilter, the Core, the Port's routed flush, and the existing channel/filter screens. Item and fluid routing controls share the existing footer row, preserving the panel height at Auto GUI scale. The filter editor shows one active row at a time while keeping item and fluid ghost inventories separate. Filled containers are the selection UI; recipe-viewer fluid dragging is not supported.

- `gradlew.bat build runData runGameTestServer --console=plain` passed, including all 27 required GameTests.
- Four new GameTests cover opt-in routing, channel order and catch-all behavior, old save compatibility, filter/toggle serialization, ghost editing without consuming containers, separate blacklist modes, fluid/component matching, same- and cross-dimension destinations, shared item/fluid fuel charging, travel reserve and recovery, legacy active-channel fluid transfer, full/incompatible/unloaded destinations without fallback or fuel loss, buffer persistence, and successful recovery without fluid loss or duplication.
- Live client in the isolated creative QA world at Auto GUI scale: both routing buttons and tooltips fit; enabled fluid routing while leaving item routing off; set water from a bucket and returned the original bucket unchanged; switching to Items leaves its filter empty; reopened the Core and filter editor and verified the fluid-routing toggle and water selection persisted.
- Multiplayer and non-bucket modded fluid containers have not been exercised. Containers use the standard NeoForge fluid-item capability. Fluid routing uses already-loaded destinations and the existing active-channel chunk-loading policy; enabling it does not load every routed destination.

Manual regression: right-click the empty-handed Core, enable Fluids: Route, right-click a bound channel, switch Items to Fluids, and add a filled container to its ghost row. Configure a later empty blacklist for unmatched fluid if desired. Pipe the selected fluid into a Gateway Port; check its selected destination receives it. Fill or unload that destination and confirm the source buffers fluid; restore it and confirm delivery resumes. Turn fluid routing off and confirm the active channel receives fluids again.
