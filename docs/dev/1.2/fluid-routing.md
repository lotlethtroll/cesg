## Scope

- [ ] Specify fluid filter selection, matching order, catch-all behavior, and UI before implementation.
- [ ] Preserve active-channel fluid behavior for existing worlds unless fluid routing is enabled.
- [ ] Route against the actual destination fuel and reserve; charge once per fuel type used per flush.
- [ ] Test blocked/full/unloaded destinations, mixed destinations, save/reload, and no-loss/no-duplication behavior.

Targets `1.2-dev`. Scaffold only; implementation and validation are pending.

https://github.com/lotlethtroll/cesg/issues/9
