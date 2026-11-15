# Contributing

## Branches
- `main` — protected. No direct push. Changes arrive only through PRs.
- `mantis/SCRUM-<n>-short-name` — one branch per Jira story. Hyphens. Delete after merge.
  - `mantis/SCRUM-8-quit-to-menu`, `mantis/SCRUM-15-vt323-font`

## Commits
- Format: `<type>: <subject>` — see `.gitmessage.txt`. Enable it: `git config commit.template .gitmessage.txt`
- Types: `feat` `fix` `refactor` `docs` `test` `ci` `chore` `style`
- Subject ≤ 50 chars, imperative ("add", not "added"), English.
- Small commits. If you need "and" in the subject, split it.
- Footer: `Refs SCRUM-<n>` so the commit points back to its Jira card.

## Pull requests
- One Jira story per PR. Target ~400 changed lines or less.
- Fill the template. Screenshots for anything visible.
- Base branch: `main` of this repository.
- Rebase/merge latest `main` before requesting review. PRs with conflicts are not reviewed.
- Request **1–2 reviewers**, not everyone.
- Merge method: **Squash and merge**. Delete the branch.
- When you open the PR, move the Jira card to **In Review** and paste the PR link on the card.
- If it depends on another story or PR, write `Depends on: SCRUM-<n>` or `Depends on: #N` in the Jira section of the template.

### PR title
The title becomes the squash commit on `main`, so it must say which epic and which Jira item it is.

```
<type>(<epic>): SCRUM-<n> <subject>
```

| Part | Rule |
|---|---|
| `type` | `feat` `fix` `docs` `refactor` `test` `ci` `chore` `style` |
| `epic` | lowercase short name from the table below. No epic → leave out the brackets |
| `SCRUM-<n>` | the Jira story or bug key |
| `subject` | imperative ("add", not "added"), no full stop |
| length | whole title 50 characters or less |

| Jira epic | Short name |
|---|---|
| Optimization (SCRUM-13) | `optimization` |
| Visuals (SCRUM-12) | `visuals` |
| Sound (SCRUM-14) | `sound` |
| Display and Resolution (SCRUM-10) | `display` |

New epic in Jira → add its short name here in the same PR that first uses it.

Examples:
```
feat(optimization): SCRUM-8 skip game over on quit
feat(visuals): SCRUM-15 switch to VT323 font
fix(optimization): SCRUM-21 stop music on quit
docs: SCRUM-20 update PR template
```
A bug takes the epic of the feature it breaks. If a story moves to another epic after it was merged, leave the old title as it is.

## Reviews
Tag every comment with a priority so the author knows what is required:

| Tag | Meaning | Reviewer action |
|---|---|---|
| **P1** | Must fix — breaks behaviour or violates a rule | Request changes |
| **P2** | Should fix — strong recommendation | Request changes |
| **P3** | Consider — fix now or leave a follow-up note | Comment |
| **P4** | Optional | Approve |
| **P5** | Nit / opinion | Approve |

Review focus, in order: (1) does it work, (2) does it touch shared files more than needed, (3) consistency with existing code, (4) naming.

Approval must say **what you checked** in one line. "LGTM" alone is not a review.

The reviewer ticks each acceptance criterion in the PR after checking it in the running game. The author does not tick their own.

## Shared files — extra care
`engine/DrawManager.java`, `engine/Core.java`, `screen/GameScreen.java` are used by every team.
Any change there must be mentioned in the PR description and reviewed by the team that owns the file (menu framework: Hello World).

## Definition of Done
- Code compiles (CI green) and the game runs
- Reviewed and approved
- Screen reachable from the menu and exits with ESC (for screen work)
- Docs/wiki updated if behaviour changed
