# Remote Premium (Firebase)

Signed-in accounts unlock when **either**:
- `users/{uid}.premium == true`, or
- a doc exists at `premiumGrants/{uid}` with `premium: true`

## Easiest way (recommended) — use `premiumGrants`

This avoids the Firebase Console nesting bug on the big `users` document.

1. Open [Firestore](https://console.firebase.google.com/project/fitquest-1f082/firestore/data)
2. Click **Start collection** (or open collection) named exactly: `premiumGrants`
3. Document ID = the player’s Auth **UID** (from Authentication → Users)
4. Add field:
   - `premium` → boolean → **true**
5. Save

That’s it. The app picks it up live while signed in.

To revoke: set `premium` to **false**, or delete the `premiumGrants/{uid}` document.

## Find a user’s UID

**Authentication → Users** → match email → copy **User UID**.

## Alternate — field on `users/{uid}`

Only works if the field is at the **document root** (next to `character`), not inside `gearInstances` / `stacks`.

Console tip: before **Add field**, click the **document ID** in the middle column so nothing nested is selected. If a map is highlighted, new fields go inside it.

The app now also detects a misplaced `premium` under `gearInstances` and auto-moves it to the root.

## Notes

- Player must be **signed in**
- Guests cannot receive remote Premium
- Redeem codes still work and write `users/{uid}.premium = true`
