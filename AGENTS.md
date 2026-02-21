# Repository Agent Rules

## Git Push Policy
- In this repository (`/Users/ktoyo/Documents/Web-under-the-hood`), do not ask for user confirmation before running `git push`.
- When commits are ready, push directly to the current branch unless the user explicitly requests a different branch/remote.

## Git Commit/Push Execution Policy
- In this repository, do not ask for user confirmation before running `git commit`.
- If sandbox restrictions block `git commit` or `git push`, immediately rerun with escalated permissions.
- For commit/push workflows, proceed end-to-end (stage, commit, push) unless the user explicitly asks to stop before push.
