# Open App Tool Design

## Goal

Allow the assistant to open an installed, launchable Android app when the user's latest message explicitly asks to open or launch that app.

## Behavior

- Add an `open_app` function tool with one required string argument: `app_name`.
- The tool description must prohibit proactive launches and limit use to explicit user requests.
- Match against launcher app labels and package names. Prefer exact normalized matches, then accept one unique partial match.
- If no app matches or several apps match, do not launch anything. Return a clear tool failure so the assistant can explain it.
- On a unique match, launch the resolved activity immediately without a confirmation dialog.
- Show progress through the existing tool timeline using `Open app: <name>` / `打开应用：<名称>`.

## Android Boundary

- Discover only activities that handle `ACTION_MAIN` plus `CATEGORY_LAUNCHER`.
- Declare that launcher intent under manifest `<queries>` for Android 11+ package visibility.
- Do not request `QUERY_ALL_PACKAGES`.
- Launch with an explicit component and `FLAG_ACTIVITY_NEW_TASK` from the application context.

## Scope

- Supports apps with launcher activities. Background-only services and hidden system components are out of scope.
- No app control after launch, no accessibility automation, no confirmation UI, and no installed-app list exposed in the chat UI.

## Verification

- Unit tests cover the tool schema, exact/partial/ambiguous matching, and ViewModel tool execution.
- Build debug and signed release APKs.
- Install on `emulator-5554` and verify an explicit request opens a known launcher app and the tool timeline remains in the conversation.
