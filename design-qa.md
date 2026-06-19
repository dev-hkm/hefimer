# Design QA — Exchange Split workspace

## Comparison target

- Source visual truth: `C:\Users\ASUS\.codex\generated_images\019ede81-7ea1-7653-b34d-7898b78dbc2a\exec-17458cea-ea98-4de5-906c-8b8e6838eba0.png`
- Implementation: `http://127.0.0.1:3000`
- Intended viewport: desktop, 1280 × 720
- Verified state: Send Text active, Receive panel visible

## Evidence collected

- DOM verification confirms the desktop workspace has a Send region at x=24 with width=822 and a separate Receive region at x=959 with width=291.
- The Send Text action opens the existing Prism editor, including language control, line-number control, copy/clear controls, and the editable code surface.
- The Receive code input and disabled-until-valid Retrieve action are present in the independent right column.

## Blocker

The in-app browser repeatedly timed out while capturing `Page.captureScreenshot`, including a clipped 1280 × 720 attempt. A rendered implementation screenshot could therefore not be placed side-by-side with the source visual.

## Findings

- [P2] Screenshot-based visual comparison is unavailable.
  - Location: in-app browser capture.
  - Evidence: two `Page.captureScreenshot` requests timed out.
  - Impact: typography, spacing rhythm, icon treatment, and final visual fidelity cannot be objectively compared against the selected reference image.
  - Fix: retry the local visual capture when the in-app browser screenshot channel is available, then compare it with the source at the same desktop viewport.

## Patches made

- Replaced the constrained central toolbox layout with a responsive two-column workspace.
- Kept File and Text as Send modes in the left workspace.
- Made Receive permanently available in a distinct right-side region.
- Moved Chat Room, Board, and History into secondary tools.
- Retained the original Send Text component and Prism syntax highlighting.

## Implementation checklist

- [x] TypeScript lint passes.
- [x] Production Vite build passes.
- [x] Local server responds successfully.
- [x] Send Text and Receive DOM states verified.
- [ ] Capture and visually compare the desktop implementation.

final result: blocked
