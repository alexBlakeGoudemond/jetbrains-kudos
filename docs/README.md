# Kudos

This document describes what Kudos is trying to solve and how it differs from other approaches to attribution.

## How is this Plugin different from other solutions?

### Git AI

Tools like [Git AI](https://usegitai.com/) work by recording metadata about AI-generated changes while an AI Agent is
actively making changes in a codebase.

Combined with Git Hooks, this metadata can be used to automatically add AI attribution to commits.

#### Pros

- Attribution can happen automatically
- If you forget to give credit, the Git Hook can add it for you
- The attribution can be based on actual AI activity recorded by Git AI

#### Cons

- Requires three separate components: Git AI, Git Hooks, and the AI Agent
- If Git AI is not running, the automatic attribution may fail
- Metadata support depends on the AI Agent and tooling being used
- It does not solve attribution for code copied from tools such as ChatGPT or Claude

### Kudos

Kudos takes a deliberately different approach:

**The developer decides.**

Kudos does not attempt to detect whether AI was used or determine who contributed to a change.

Instead, it provides a simple option in the JetBrains commit UI that allows the developer to give credit to whoever or
whatever helped with the change.

For example:

- A teammate
- GitHub Copilot
- Claude
- ChatGPT
- Stack Overflow
- An open-source project
- Documentation
- Anyone else the developer wants to credit

The selected attribution is added directly to the commit message in a standardized format.

#### Pros

- Simple setup
- No Git Hooks required
- No external metadata required
- No AI detection
- Works regardless of how the contribution was made
- Works for AI Agents, humans, websites, documentation, or anything else
- Attribution is controlled by the developer
- The attribution preference can be remembered per repository

#### Cons

- Attribution is not automatic.
- The developer must remember to give Kudos when appropriate.
- The developer is responsible for accurately declaring attribution.

## The principle

Kudos deliberately chooses a low-tech approach:

> **Don't try to determine who contributed. Ask the person making the commit.**

The developer is already responsible for reviewing and accepting the changes. They are therefore also the person best
placed to decide who deserves credit for them.
