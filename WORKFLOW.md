# Git Workflow

## 1. Workflow and rationale

**Workflow** : GITHUB Flow

**Rationale** : 

We chose **GitHub Flow** because our project requires multiple team members to work on different features at the same time. It provides a simple workflow that allows us to develop features separately while keeping the main branch stable.

1. *Short - lived branches from main* : 
Each team member creates a branch from the latest main branch for a specific task. Since the branches are used only for a specific task and are merged after completion, there is less chance of large merge conflicts. Short-lived branches also make the changes easier for other team members to review.

2. *Changes integrated through Pull Requests(PRs)* :
Completed changes are submitted through a Pull Request instead of being directly pushed to main. This allows team members to review the purpose of the change, the code modifications, and the testing results before the changes are merged.

3. *Stable main branch with parallel development* :
Our project has multiple requirements that can be developed by different team members. GitHub Flow allows team members to work on separate branches at the same time while keeping the main branch stable. This is useful because our team needs to make frequent changes during development.

---

## 2. Branch Strategy

We use two main types of branches:

- feature/ - used to develop new functionality, such as achievement systems and achievement unlock conditions.
- fix/ - used to  fix bugs or problems found during development or testing.

The development of the achievement system is divided into several batches. Each batch focuses on one part of the system and is tested before moving to the next stage. This allows the team to build the system step by step instead of developing all achievements at the same time.

### Fix Branch Priority
- A fix/ branch can be created at *any stage of development* when a bug or problem is discovered.
- Fix branches take priority over new feature branches because existing problems should be resolved before additional functionality is added. Once the fix has been completed, tested, reviewed, and merged, the fix/ branch should be closed as soon as possible.
- This helps prevent known bugs from affecting the development of later features.

### Batch 1: Basic Record System and Test Achievement

The first batch focuses on preparing the basic data and a simple achievement that can be used to test the system.

- Create the *Record System* for storing data that already exists in the game.
- Create one *non-tiered achievement unlock condition* as the test achievement.
- The test achievement will be based on the condition *"Defeat the first enemy."*

A non-tiered achievement is used as the first test because it is the simplest type of achievement and does not require multiple achievement levels or hidden information.

### Batch 2 : Basic Achievement System

The second batch focuses on developing the actual achievement system and testing it with the first non-tiered achievement.

The system should be able to:

- Check whether the achievement condition has been met by the end of the level.
- Display the achievement when it is unlocked.
- Give currency as a reward when the achievement is unlocked.
- Adjust the currency reward value based on playtesting.
- Display an achievement icon on the title screen after the achievement is unlocked.
- Play a sound effect when an achievement is unlocked, in collaboration with the Sound Design team.

Our team will first confirm that the achievement system works correctly and is stable. Only after the system has been successfully tested will we create branches for the remaining non-tiered achievement unlock conditions.

### Batch 3: Tiered Achievements

The third batch focuses on implementing tiered achievements.

- Create one *tiered achievement unlock condition* as a test.
- Develop the system required to support tiered achievements.
- Test the system using the first tiered achievement.
- After the system works correctly, create the remaining tiered achievement unlock conditions.

### Batch 4: Hidden Achievements

The fourth batch focuses on implementing hidden achievements.

- Create one *hidden achievement unlock condition* as a test.
- Develop the system for hidden achievements.
- Hide the achievement information from the player until the achievement is unlocked.
- Test the hidden achievement system using the first hidden achievement.
- After the system works correctly, create the remaining hidden achievement unlock conditions.

This batch-based strategy allows the team to test each type of achievement system using one example before creating the remaining achievements. As a result, problems can be identified and fixed early without affecting a large number of achievements.

---

## 3. Commit Rules

We follow the rule that *one commit should represent one clear logical change*. Unrelated changes should not be combined into the same commit.

### Commit Message Format

We use the following format:

type(scope): short description

For example:

feat(achievements): add achievement domain model
feat(records): persist achievement progress and total kills
feat(achievements): add Enemy Hunter unlock logic
feat(achievements): display Enemy Hunter progress

The main commit types used by our team are:

- feat — adding a new feature
- fix — fixing a bug
- docs — changing documentation
- refactor — improving code structure without changing its behavior
- test — adding or modifying tests
- chore — performing general maintenance tasks

Using clear commit messages makes it easier for team members to understand what was changed and to find specific changes in the project history.

---

## 4. Pull Request and Review

A Pull Request should be opened when a feature or bug fix has been completed and tested.

Before a Pull Request is merged:

- At least *one team member must review and approve* the Pull Request.
- The reviewer should check the changes and confirm that they do not introduce unnecessary problems.
- If changes are requested, the developer should make the necessary changes and push them to the same branch.
- The Pull Request will automatically be updated with the new commits.

Direct pushes to the main branch are *not allowed*. All changes must go through a Pull Request and code review before being merged into main.

This process helps prevent unfinished or incorrect changes from being added directly to the main branch.

---

## 5. Merge strategy

We use a *regular merge* to integrate Pull Requests into the main branch.

The merge process follows these rules:

- A feature should be fully completed and tested before it is merged.
- Merge conflicts should be resolved on the developer's branch before the Pull Request is merged.
- When a conflict occurs, team members can check previous commits and merges to understand why the conflicting code was introduced.
- Each logical change is kept as a separate commit, making the project history easier to understand.
- After the Pull Request is approved and merged, the changes become part of the main branch.

Using regular merges allows us to keep the development history of each feature while maintaining a clear and understandable main branch.

---

## 6. Overall workflow


The following steps describe how each of our team member develops and integrates a feature or bug fix.

### Step 1: Pick a Task

Each team member selects or creates a GitHub Issue and assigns it to themselves.

For example:

#8 Add Enemy Hunter achievement

Tasks are organized based on the achievement development order. Basic components that other features depend on are developed first, followed by the actual achievement system and additional achievement types.

### Step 2: Update main

Before creating a new branch, the developer updates their local main branch so that they start with the latest version of the project.

git switch main; 
git pull origin main


### Step 3: Create a Branch

The developer creates a new branch based on the type of work.

For example, a new feature uses the feature/ prefix:

git switch -c feature/enemy-hunter

A bug fix would use the fix/ prefix:

git switch -c fix/achievement-display

The branch name should clearly describe the task or achievement being developed.

### Step 4: Develop and Commit

The developer implements the assigned task and creates commits for each logical change.

For example:

git add src/achievements/Achievement.java; 
git commit -m "feat(achievements): add achievement domain model"

git add src/records/RecordManager.java; 
git commit -m "feat(records): persist achievement progress and total kills"

git add src/achievements/EnemyHunter.java; 
git commit -m "feat(achievements): add Enemy Hunter unlock logic"

git add src/ui/AchievementView.java; 
git commit -m "feat(achievements): display Enemy Hunter progress"

Each commit should contain one clear logical change.

### Step 5: Sync with main

Other team members may have merged their work while the developer was working. Therefore, the developer should update their branch with the latest changes from main.

git fetch origin; 
git merge origin/main


If a merge conflict occurs, the developer should check the history of the conflicting file to understand the changes made by both sides.

git log --merge -p <file>


After resolving the conflict, the developer should build and test the project again. A successful merge does not always guarantee that the project will work correctly.

Then the resolved file can be added and committed:

git add <file>; 
git commit


### Step 6: Push the Branch

After the feature has been developed and tested, the developer pushes the branch to GitHub.

git push -u origin feature/enemy-hunter

The first push creates the branch on GitHub.

### Step 7: Open a Pull Request

The developer opens a Pull Request after the feature or bug fix has been completed and tested.

The Pull Request should explain:

- What was changed.
- Why the change was made.
- How the changes can be tested.
- Which GitHub Issue is related to the change.

For example:

Closes #8

This automatically closes the related issue when the Pull Request is merged.

### Step 8: Review

At least one team member reviews the Pull Request.

If changes are requested, the developer makes the necessary changes and creates additional commits on the same branch.

For example:

git commit -m "fix(achievements): stop kill count resetting on game over"; 
git push

The Pull Request is automatically updated with the new changes.

### Step 9: Merge

After the Pull Request has been approved, it can be merged into main.

We use a regular merge so that the commits from the feature branch remain visible in the project history. This allows the team to see how each feature was developed.

The merge should be completed after approval so that the next developer can start from the latest version of main.

### Step 10: Clean Up

After the Pull Request has been merged, the developer deletes the completed branch from GitHub.

The developer then updates their local main branch:

git switch main;
git pull origin main

Finally, the local feature branch can be deleted:

git branch -d feature/enemy-hunter

The developer can then return to **Step 1** and start the next task.

### Diagram

```mermaid
flowchart TD
    A[Pick or create an issue] --> B[Pull latest main]
    B --> C[Create branch<br/>feature/ · fix/ · refactor/ · docs/]
    C --> D[Write code &<br/>make small commits]
    D --> E[Rebase onto latest main]
    E --> F{Conflicts?}
    F -- Yes --> G[Author resolves,<br/>builds & tests]
    G --> H
    F -- No --> H[Push branch]
    H --> I[Open Pull Request]
    I --> J{Review:<br/>1 approval + build passes?}
    J -- Changes requested --> K[Address feedback<br/>push new commits]
    K --> J
    J -- Approved --> L[Regular merge into main]
    L --> M[Delete branch]
    M --> B
