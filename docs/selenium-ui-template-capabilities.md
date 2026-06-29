# Selenium UI Template Capabilities

This project keeps Selenium UI interaction primitives in the reusable `core.ui` layer so generated page objects and tests use stable framework methods instead of regenerating raw WebDriver code.

## Core UI action classes

- `ua.demo.agentlab.core.ui.actions.ElementActions`
  Covers element lookup, click, JavaScript click, send keys, append text, clear, clear-and-type, submit, text, tag, attribute, DOM property, CSS value, rect, selected/enabled/visible state, existence/count, and checkbox-style toggle helpers.
- `ua.demo.agentlab.core.ui.actions.DropdownActions`
  Covers single-select and multi-select operations: select by text/value/index, selected option(s), all options, `isMultiple`, and deselect operations.
- `ua.demo.agentlab.core.ui.actions.AlertActions`
  Covers alert presence, text, accept, dismiss, prompt input, and safe conditional accept/dismiss helpers.
- `ua.demo.agentlab.core.ui.actions.FrameActions`
  Covers switching by frame element, locator, name/id, index, parent frame, and default content.
- `ua.demo.agentlab.core.ui.actions.WindowActions`
  Covers current/all handles, switching, new tab/window, close-and-switch-back, count, size, position, maximize, minimize, and fullscreen.
- `ua.demo.agentlab.core.ui.actions.NavigationActions`
  Covers navigate to, back, forward, and refresh.
- `ua.demo.agentlab.core.ui.actions.JavascriptActions`
  Covers execute script, async script, JS click, scroll into view, focus, blur, set value, set/remove attribute, and top/bottom scrolling.
- `ua.demo.agentlab.core.ui.actions.AdvancedUserActions`
  Covers Selenium Actions API primitives: move/hover, double click, context click, click and hold, release, drag and drop, drag by offset, keyboard send keys, key down/up, pause, wheel scrolling, and release input state.

## Base page exposure

`ua.demo.agentlab.core.ui.BasePage` exposes reusable protected helpers:

- `waits`
- `elements`
- `dropdowns`
- `alerts`
- `frames`
- `windows`
- `navigation`
- `scripts`
- `interactions`

Generated page objects inherit these helpers automatically.

## Design intent

- Keep Selenium API usage centralized in framework support classes.
- Let template-driven page objects compose reusable primitives instead of embedding ad-hoc WebDriver code.
- Preserve universal cross-project generation by giving the AI a stable capability surface.
