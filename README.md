# jetbrains-kudos

A simple JetBrains plugin for giving credit where credit is due!

## What does this Plugin do?

Kudos makes it easy to give credit to contributors when committing changes through a JetBrains IDE. Whether the
contributor is a teammate, an open-source project, a Stack Overflow answer, or an AI Agent, Kudos lets you select who
helped and automatically adds the attribution to your commit message. The attribution is controlled entirely by the
developer using the IDE's commit UI.

## Why is this Plugin needed?

Giving attribution to contributors helps maintain a healthy and transparent development environment. It ensures that
people and tools that contributed to a change receive appropriate recognition.

This is particularly useful when working with AI Agents. AI-generated code still needs to be reviewed, understood, and
accepted by a developer. Giving credit to the AI Agent makes its involvement explicit without attempting to determine
automatically whether AI was used. Transparency is key to maintaining trust in the development process - attribution
forms a keen part of that trust.

## Why is it called Kudos?

*Kudos* comes from the Greek word *kydos*, meaning glory, praise, or renown.

In modern usage, to give someone "kudos" is to give them credit or recognition for something they did well.

That's exactly what this plugin is about:

> **Give credit where credit is due.**

Whether that credit goes to a teammate, an open-source project, Stack Overflow, or an AI Agent — Kudos makes it easy to
say, "Hey, you helped with this."

## How does this Plugin work?

Kudos takes a deliberately simple approach:

**Let the developer decide.**

Rather than relying on AI detection, commit metadata, Git Hooks, code analysis, etc to determine whether an AI Agent or
other contributor was involved, `Kudos` provides a simple option in the JetBrains commit UI.

The developer selects the contributor they would like to credit, and Kudos adds the appropriate attribution to the
commit message in a standardized format.

The selected attribution can be remembered for future commits and easily changed whenever needed.

No AI detection.  

No Git Hooks.  

No additional metadata.  

Just a simple way to give credit.

## Extra Reading

See [docs/README.md](docs/README.md) for more information about the problem Kudos is trying to solve and how it differs
from other solutions.