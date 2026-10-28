# Transfer diagnostics

The Storage Bridge shows a small status indicator on each Push/Pull header. Hover the header, enable button, or filter-mode button for the server's current explanation. Green means transfer available, grey means disabled or no matching work, and amber means a blocker or a truncated network scan.

Supported explanations: destination cannot accept matching items, partner locked against extraction, no matching channel route, protected player-travel fuel reserve, insufficient fuel, gateway not rotating, unavailable network controller, unreachable partner, and network scan limit reached.

These describe automatic bridge transfers. A disabled push may still report a blocker while its previously staged outgoing items wait for delivery. Already-crossed incoming items are diagnosed against local storage before gateway or lock checks. Locks block extraction, not deposits. Destination checks respect handler insertion restrictions; source checks simulate extraction and respect handler restrictions and outgoing-buffer space.

## Precedence and refresh

- Disabled with no pending buffer takes precedence.
- Local controller availability and scan truncation precede gateway checks.
- Pending incoming-buffer delivery precedes remote checks, since it no longer needs a crossing.
- Otherwise check gateway rotation, active partner/source availability and scan truncation, then pull lock.
- With matching work, any available candidate wins. When all candidates fail, the enum's explicit declaration order breaks ties: unavailable controller, scan limit, no route, offline partner, protected reserve, insufficient fuel, destination full.
- No matching extractable items reports idle. Pending push-buffer items are checked even when automatic push is disabled.

An open bridge menu recomputes server diagnostics on opening and every 10 broadcasts (normally half a second). It replaces old states on recovery. Network membership retains the existing 20-tick cache. Diagnosis simulates insertion/extraction and shares the actual automation reserve predicate; it consumes no items or fuel and checks loaded chunks before remote lookup.

The terminal prefixes its title with `!` when its local scan is truncated. Hover the title for the wrapped explanation. Exactly 256 members is not a truncation: a warning requires additional discovered members beyond the cap. The server sends warning changes even when item totals do not change. Bridge diagnostics also cover truncated source/destination networks.

## Validation (2026-10-28)

- `gradlew.bat build runGameTestServer --console=plain`: build and all 23 required GameTests passed.
- Three new GameTests cover destination-full recovery, missing-route recovery, unloaded partner and recovery without loading its chunk, gateway rotation recovery, pull lock and unlock, insufficient fuel and refill, reserve gating consistent with the spending predicate, item/fuel preservation, and exact-limit/truncated/recovered scans.
- `gradlew.bat runData --console=plain`: generated the English diagnostic strings from CESGLangProvider.
- Live client, Auto GUI scale, isolated creative QA world: verified bridge indicators, wrapped enable-button tooltips, inactive gateway explanation, and switching back to disabled clears the stale blocker. Verified terminal scan warning and its header tooltip; fixed top-edge clipping found during this check and rechecked the wrapped tooltip in-game.
- Multiplayer has not been exercised. Diagnostics are advisory snapshots; another transfer can change capacity after a status refresh. Transfer behavior and item/fuel accounting remain authoritative on the server.

Targets `1.2-dev`; issue https://github.com/lotlethtroll/cesg/issues/13.
