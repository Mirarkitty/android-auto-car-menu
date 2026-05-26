# Contributing to CarMenu

Thanks for the interest. CarMenu is a small personal project; the bar
for accepting changes is "does it keep the app small and the privacy
story simple". Bug fixes and small features welcome; large rewrites or
new permissions are unlikely to land.

## Reporting issues

Open an issue at <https://github.com/Mirarkitty/android-auto-car-menu/issues>.
Useful things to include:

- Phone model + Android version + Android Auto version
- Whether the screen is the **phone-side settings activity** or the
  **AA car screen**
- A `logcat -s CarMenu*` snippet if the bug is functional
- Server JSON that triggers the bug if it's a render issue

## Pull requests

1. Fork the repo on GitHub.
2. Create a branch off `master`: `git checkout -b fix-xyz`.
3. Make the change. Add tests if there's a sensible JVM test
   (`app/src/test/java/...`) — most of the parser / classifier code
   has unit-test coverage already; new logic should follow the same
   pattern.
4. Run `make test` locally to confirm all tests pass.
5. Push and open a PR against `master`.
6. CI will run the test suite + build a debug APK as an artifact you
   can download from the PR's Checks tab.

## What's likely to merge

- **Yes:** new icon slugs (drop a vector in `app/src/main/res/drawable/`
  + register in `IconRegistry`), bug fixes, protocol additions that
  don't break old clients, reference servers in new languages, doc
  fixes.
- **Probably:** UI polish that respects the AA host theme, performance
  fixes, tighter same-origin / security checks.
- **Unlikely:** new runtime permissions (the small permission surface
  is the whole pitch), background services, third-party SDKs, anything
  that talks to a server other than the user-configured one.

## Code style

- Java 8 source / target. No Kotlin in `app/`.
- Programmatic UI in `MainActivity` is intentional — no XML layouts
  for something this small.
- Keep new classes ≤ ~200 lines. Extract pure helpers (no Android
  imports) so they can be unit-tested without Robolectric.
- Comments explain *why*, not *what*. The current code mostly does
  this; please match the tone.

## Releases

Releases (signed AABs uploaded to Play, plus a corresponding git tag)
are cut by the maintainer. CI does **not** build release bundles — the
upload signing key is intentionally off CI.

If you contributed something that ships in a Play release, you'll be
mentioned in the release notes on the GitHub Releases page.

## License

By submitting a contribution you agree it is licensed under the
project's [MIT license](LICENSE).
