# Test checklist (run on a real phone after installing)

Automated: `./gradlew testDebugUnitTest` (email validation, duplicate removal, paste parsing).

## Install / data
- [ ] App installs and opens; launcher name is "Email Manager"
- [ ] Add 3 emails, force-close the app, reopen: all 3 are still there
- [ ] Restart the phone, reopen: data and the highlighted (selected) entry are still there
- [ ] Airplane mode on: every feature below still works

## Add / edit / delete
- [ ] Add Email with an invalid address: Save is disabled and an error shows
- [ ] Add an address that already exists (different capitals): "already saved" error
- [ ] Add with label, notes and each status; the card shows them
- [ ] Edit changes email, label, notes and status; editing keeps the entry's own email without a duplicate error
- [ ] Delete asks for confirmation; Cancel keeps it, Delete removes it

## Copy and navigation
- [ ] "Copy Email" on a card shows "Copied: ..." and pasting elsewhere gives that address
- [ ] Bottom bar "Copy Email" copies the current entry
- [ ] Next moves to the following entry, scrolls it into view, and wraps from last to first
- [ ] Prev moves back and wraps from first to last
- [ ] With nothing selected, Next selects the first entry and Prev selects the last
- [ ] After searching, Next / Prev only move through the matching entries
- [ ] Tapping a card selects it (blue highlight)

## Search / filter / sort
- [ ] Search matches email text, label and notes; the X clears it
- [ ] Status chips filter correctly; tapping the active chip returns to All
- [ ] Each sort order reorders the list as named
- [ ] Dashboard totals update after add / delete / status change

## Paste import
- [ ] Paste a mix of valid, invalid, repeated (different capitals) and already-saved emails: preview counts are right
- [ ] "Import N emails" adds exactly the previewed new ones with the chosen status; nothing is added before pressing it
- [ ] Back button / arrow returns without importing

## Backup
- [ ] Export backup file: choose a location; the JSON file exists
- [ ] Delete all data: confirmation appears; Cancel does nothing; confirming empties the list
- [ ] Import backup file with that JSON: all entries return with labels, notes, statuses
- [ ] Importing it a second time reports everything as already saved (no duplicates)
- [ ] Importing a .txt with one email per line works

## Google shortcuts
- [ ] Gmail opens the Gmail app (or mail.google.com in the browser if not installed)
- [ ] Google Maps opens the Maps app; if Maps is disabled / uninstalled, the browser opens maps.google.com and a message explains
- [ ] Google Account opens https://accounts.google.com/ in the browser
- [ ] Nothing is typed, posted or submitted automatically in any of these

## Layout
- [ ] Portrait on a small phone (about 5 inch) and a large phone: no clipped buttons
- [ ] Keyboard open while searching: the bottom bar hides, list stays usable
- [ ] Rotate to landscape: still usable
