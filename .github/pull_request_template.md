<!--
TITLE: type(epic): SCRUM-<n> subject      (50 characters max, no full stop)
  e.g. feat(optimization): SCRUM-8 skip game over on quit
  types: feat fix docs refactor test ci chore style
  epics: optimization | visuals | sound | display
  no epic -> leave out the brackets: docs: SCRUM-20 update PR template
  Full rules: CONTRIBUTING.md
-->

## Jira
SCRUM-__ → https://mantis-spaceinvaders.atlassian.net/browse/SCRUM-__
Depends on: <!-- SCRUM-__ / PR #__ / none -->

## What
<!-- One sentence. What does this PR do? -->

## Decisions
<!-- Behaviour choices made in this story, e.g. "coins from this level are taken back on quit". Write none if none. -->

## Shared files touched
<!-- Core / DrawManager / GameScreen: which ones and why. Write none if none. -->

## How to test
1. Build & run
   - Windows (PowerShell): `javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName; java -cp "bin;res" engine.Core`
   - macOS / Linux: `javac -d bin $(find src -name "*.java") && java -cp bin:res engine.Core`
2. 

## Acceptance criteria (reviewer ticks after checking)
<!-- Paste from the Jira story -->
- [ ] 
- [ ] 

## Screenshots / GIF
<!-- Required if anything on screen changed -->

## Before requesting review (author ticks)
- [ ] Title follows `type(epic): SCRUM-<n> subject`
- [ ] Branch is `mantis/SCRUM-<n>-short-name`, based on latest `main`
- [ ] One story, under ~400 changed lines
- [ ] I ran the game after my last commit
- [ ] I checked `git diff HEAD` before any merge commit
- [ ] CI is green
- [ ] Jira card moved to **In Review**, with this PR's link

<!-- Reviewers: tag every comment P1–P5 (see CONTRIBUTING.md). Approval says what you checked, not just "LGTM". -->
