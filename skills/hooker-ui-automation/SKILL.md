---
name: hooker-ui-automation
description: Use when operating Android apps through Hooker UI MCP or the /hooker/ui and /hooker/mediaprojection endpoints. Covers stable UI automation strategy, element selection, screenshot/inspect alignment, WebView and React Native handling, validation after actions, and recovery from common failures such as stale hooker_id, inspect null states, and unexpected page jumps.
---

# Hooker UI Automation

Use this skill when the task is to inspect, navigate, and operate an Android app through Hooker UI automation endpoints or the Hooker UI MCP server.

## Core Rules

- Prefer `inspect_current_ui` to discover controls before acting.
- Prefer `click_view` over coordinate clicks whenever a reliable target exists.
- Treat `hooker_id` as ephemeral. Do not assume an old `hooker_id` is still valid after page refresh, list recycling, dialog changes, navigation, or data reload.
- After every meaningful action, verify the result with `inspect_current_ui`, `get_activity_stack`, `get_screen_info`, `inspect_overlay`, or screenshot tools.
- If a click causes an unexpected page jump or modal state, recover immediately with `go_back`, then re-inspect.
- Use coordinate-based actions only as a fallback for WebView, React Native, canvas-like hot zones, or overlay-heavy pages.

## Default Workflow

1. Inspect first.
2. Identify the target using stable traits:
   `text`, `content_description`, `view_type`, `class_name`, `screen_rectangle`, visible position, parent context.
3. Choose the safest action:
   `click_view`, `set_text`, `send_search_action`, `swipe_view`, `set_checked`, `tab_layout_select`, etc.
4. Verify the result immediately:
   inspect again, compare activity stack, or use overlay/screenshot.
5. If verification fails, recover:
   retry with a fresh inspect, use a different target, or go back.

## Element Selection Strategy

Use this priority order:

- Stable resource id or current-turn `hooker_id`
- Text or content description
- View type plus nearby text
- Screen position from `screen_rectangle`
- Coordinate fallback

Do not reuse an old `hooker_id` across turns or after UI changes unless a fresh inspect confirms it is still present.

## Choosing Actions

Preferred actions by scenario:

- Native button, text row, image row: `click_view`
- Search box or text input: `set_text` then `send_search_action`
- Scrollable native container: `swipe_view` or dedicated scroll tool
- List/grid item without stable node identity: inspect, then click the current match only
- Toggle/radio/checkbox: `set_checked`
- WebView / React Native / hybrid page: `inspect_overlay`, then coordinate fallback if tree actions are unreliable

Use `click_by_position` only when all view-based options are unreliable.

## Coordinate Safety

Do not assume screenshot pixels equal tappable screen coordinates.

Before using coordinates:

- Read screenshot metadata:
  `image_width`, `image_height`, `display_width`, `display_height`, `real_display_width`, `real_display_height`, `rotation`, `orientation`, `app_window`
- Prefer `screen_rectangle` from `inspect_current_ui` over guessing from raw screenshot pixels
- If the model sees a scaled image, convert back into device coordinates before tapping

For overlays, prefer the center of `screen_rectangle`:

- `center_x`
- `center_y`

## Inspect vs Screenshot

Use these outputs for different jobs:

- `inspect_current_ui`
  Best for machine-readable structure, current `hooker_id`, type, text, and `screen_rectangle`
- `inspect_overlay`
  Best for model-facing visual reasoning because it draws boxes directly onto the screenshot
- `capture_media_projection_screenshot`
  Best when you need the raw screen image and metadata

When possible, inspect and overlay should be paired:

1. `inspect_current_ui`
2. `inspect_overlay`
3. action
4. re-inspect

## WebView and React Native

For WebView and React Native:

- Expect partial or misleading control trees
- Do not trust `is_clickable` alone
- Prefer `inspect_overlay` to anchor the visible layout
- Use coordinate taps or swipes only after identifying the right region visually
- Re-verify after every interaction because hot zones may trigger unexpected navigation

If a close button or dismiss area is visually obvious but not reliably clickable through the tree:

- use `click_by_position` or `long_click_view` fallback only after confirming the region via overlay

## Validation After Actions

After risky actions, always verify at least one of:

- `top_activity` unchanged when it should stay on the same page
- target text disappeared or changed
- dialog count or visible controls changed as expected
- screenshot visually reflects the intended result

High-risk actions include:

- taps near hot zones
- WebView taps
- privacy agreement or permission-related controls
- taps outside dialogs
- overlay close attempts

## Recovery Patterns

If something goes wrong:

- stale `hooker_id`: re-run inspect and reacquire the target
- inspect failure or null state: confirm current activity, wait briefly, then retry inspect
- unexpected Activity: `go_back`, inspect again, and continue from a stable page
- screenshot unauthorized: call media projection status/permission before depending on screenshots
- hybrid page mismatch: switch from tree-first to overlay-plus-coordinate strategy

## Common Failure Modes

### Stale hooker_id

Symptom:
- action returns not found
- action applies to the wrong recycled list item

Response:
- re-run inspect
- reacquire the current target

### Inspect null state

Symptom:
- page just closed
- top activity missing
- decor/root view invalid

Response:
- query activity stack
- retry after the page stabilizes
- avoid acting until inspect succeeds again

### Unexpected navigation

Symptom:
- new Activity after a tap that should have stayed in place

Response:
- inspect activity stack
- go back if it is unintended
- mark the tapped region as risky

### Screenshot mismatch

Symptom:
- AI points to the correct place visually but tap lands elsewhere

Response:
- use screenshot metadata
- use `screen_rectangle`
- prefer overlay + current inspect instead of direct pixel guessing

## Practical Guidance For Agents

- Keep each step small and verifiable.
- Do not batch multiple risky actions without an inspection checkpoint between them.
- If there is ambiguity between two nearby targets, inspect again with tighter filters or use overlay.
- On unstable pages, favor repeated inspect/verify loops over aggressive clicking.
- When in doubt, choose the action that is easiest to verify and reverse.
