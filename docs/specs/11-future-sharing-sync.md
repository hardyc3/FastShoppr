# Future Sharing and Sync Specification

## Status

Deferred. Not part of Phases 0–11. This document records the intended direction so the MVP does not block a future implementation.

## Goal

Allow multiple people to view and edit the same shopping list at the same time, across devices.

## Why it is deferred

- The MVP is local-first and has no backend.
- Real-time collaboration requires authentication, a shared data store, access rules, and conflict handling.
- Shipping a reliable collaborative experience is a separate product and security effort.
- Adding it early would slow down the core value: fast list capture and aisle routing.

## Requirements (future)

- Sign in with Google.
- Invite collaborators by email or share link.
- Roles: owner, editor, viewer.
- Real-time item and list updates.
- Offline edits that sync when connectivity returns.
- Clear conflict handling.
- Revoke access.
- Show who changed what, at least at a basic level.
- Do not expose lists publicly by default.

## Proposed architecture

- **Auth:** Firebase Authentication with Google Sign-In.
- **Data:** Cloud Firestore with real-time snapshot listeners and offline persistence.
- **Client:** Android Java SDKs.
- **Rules:** Firestore security rules enforce membership and roles.
- **Local cache:** Room remains the local source of truth for UI; a sync layer reconciles remote changes.
- **IDs:** Use client-generated UUIDs for lists and items so offline creation is possible.
- **Timestamps:** Store server timestamps for conflict resolution and `updatedAt` for local ordering.

## Data model changes

Add:

```text
RemoteList
  id
  ownerUid
  name
  shoppingDate
  members: Map<uid, role>
  updatedAt
  deletedAt

RemoteItem
  id
  listId
  storeId
  rawText
  displayName
  normalizedText
  targetDate
  aisle
  bay
  matchedProductName
  lookupStatus
  isChecked
  updatedAt
  updatedBy
  deletedAt
```

Rules:

- `rawText` remains immutable except by explicit user edits.
- Deletes are soft deletes (`deletedAt`) so offline clients can reconcile.
- Membership and roles live on the list document or a subcollection.
- Store aisle cache may stay local; sharing lists does not require sharing cache.

## Conflict resolution

MVP strategy for the sharing phase:

- Field-level last-write-wins using server timestamps.
- Checked state toggles are last-write-wins.
- Text edits use last-write-wins with an `updatedBy` label.
- Deletes are soft deletes and win over edits after the delete timestamp.
- Show a non-blocking notice if a conflict is detected.
- Do not attempt operational transformation or CRDTs initially.

Future improvements:

- Per-field merge for text.
- Presence indicators.
- Activity history.

## Security rules

- Only authenticated users can read or write shared lists.
- A user can read a list if they are the owner or a member.
- Only owners can change membership or delete the list.
- Editors can modify items.
- Viewers can read only.
- Validate field types and lengths in rules.
- Deny access by default.
- Do not trust client-provided roles.

## Offline behavior

- Firestore offline persistence caches shared lists.
- Local edits are queued and synced.
- Show a "pending sync" indicator.
- Resolve conflicts on reconnect.
- Do not lose local edits when the app is killed.

## Migration from local-only

1. Add optional sign-in.
2. Upload existing local lists only after explicit user consent.
3. Keep local-only lists working without an account.
4. Link local list IDs to remote IDs.
5. Provide a "stop syncing" option that leaves a local copy.
6. Do not delete local data on sign-out without confirmation.

## UX

- "Share" action on a list.
- Invite by email or link.
- Role selector.
- Member list with revoke action.
- Account screen with sign-out.
- Sync status indicator.
- Conflict notice.

## Privacy and security

- Shared list contents leave the device only after the user opts in.
- Do not share images or location by default.
- Provide a clear privacy explanation before enabling sync.
- Allow the owner to delete the remote list and revoke access.
- Follow `10-security-privacy.md`.

## Open questions

- Should shared lists support images or only text items?
- Should viewers see aisle data?
- How long should deleted lists be retained?
- Should membership be per list or per household?
- Do we need web access?
- Which backend is acceptable: Firebase, Supabase, or custom?

## Phase outline (future)

1. Sharing spike and backend decision.
2. Authentication and account shell.
3. Remote list model and security rules.
4. Sync layer and conflict handling.
5. Share/invite UI and roles.
6. Offline and conflict testing.
7. Privacy review and release.

Do not start any of these phases until the MVP is released and a new decision entry approves a backend.
