## Scope

- [ ] Start with lone batteries; decide pickup semantics before changing behavior.
- [ ] Specify fluid ownership when breaking or splitting arrays; array relocation remains deferred until this is settled.
- [ ] Prevent duplicate fuel across block drops, world tanks, placement, and repeated break/place cycles.
- [ ] Test both fuel types, empty/full tanks, survival/creative, save/reload, and array boundary cases; do not change recipes without Andrew's sign-off.

Targets `1.2-dev`. Scaffold only; implementation and validation are pending.

https://github.com/lotlethtroll/cesg/issues/15
