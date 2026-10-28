# Terminal recipe shortcuts

Hover a network item and use the installed viewer's configured recipe or uses key (normally R/U). Local and Partner entries use the displayed, filtered and scrolled item. Search focus and a carried item suppress the custom hover target; empty cells expose no ingredient. Storage mouse clicks retain their normal behavior.

JEI and EMI register adapters to a viewer-neutral terminal hover query. Neither viewer is required. Clicking the grid or a Local/Partner tab leaves search; Tab navigation is also available.

## Validation (2026-10-28)

`gradlew.bat build runGameTestServer --console=plain` passed, including all 20 required GameTests.

| Client configuration | In-game result |
| --- | --- |
| EMI only (`runClient -PwithoutJei`) | R opens recipes and U opens uses for local white wool; Partner oak planks opens its recipe. Focused search types R/U instead; empty filtered grid does nothing. Withdrawal and deposit preserve counts. |
| JEI only (`runClient -PwithoutEmi`) | Remapped G/H opens local recipes/uses. Focused search types G instead. Withdrawal and deposit preserve counts. |
| Neither (`runClient -PwithoutEmi -PwithoutJei`) | Terminal opens normally, R/U safely do nothing, and withdrawal/deposit preserve counts. |

Tests used the isolated creative QA world at Auto GUI scale. Multiplayer, a remapped EMI binding, and JEI Partner hover were not separately exercised; both adapters share the same local/partner hover query. Temporary test key bindings were restored afterward.

Closes https://github.com/lotlethtroll/cesg/issues/11. Targets `1.2-dev`.
