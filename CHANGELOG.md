# Changelog

## [Unreleased]

## [1.2.0]

### Added

- Added a hint to the commit tool window textbox showing Kudos addition
    - You now see all collaborators before you commit, incase you forgot you enabled Kudos!
    - This only shows if Kudos is enabled and if the input has text (does not replace the default hint: 'Commit
      Message')
- Added icon to .idea directory
    - Cloning this repo now shows a unique icon for the repository

### Fixed

- Attempt to address bug; update requires restart when it shouldn't (003)
    - Register additional Disposable objects
    - Modify bus subscriptions to allow easier unloading
- Dynamically find plugin ID incase of using hardcoded "Kudos"

## [1.1.3]

### Fixed

- Attempt to address bug: update requires restart when it shouldn't (002)
    - Add PluginListener to clear icon cache during unload

## [1.1.2]

### Fixed

- Address Deprecated API Usage warning
  - Remove redundant Refreshable.refresh() entirely in KudosCommitOptionsPanel
- Improve icons
  - fill in the back of the hand with an opaque colour - improve visibility in the IDE

## [1.1.1]

### Fixed

- Attempt to address bug: update requires restart when it shouldn't (001)
    - Add Disposable to KudosSettingsState

## [1.1.0]

### Added

- Plugin now supports selecting multiple collaborators
    - Useful if pair-programming with someone, and also use AI - give credit to all 3 (yourself, colleague, AI)

## [1.0.0]

### Added

- Initial plugin creation
    - Tool Window to enable Kudos and define Collaborators
    - Integrated into Commit Tool Window to Give Kudos to Collaborators
    - Settings remembered globally (2+ IDE instances will have same collaborators)

[Unreleased]: https://github.com/alexBlakeGoudemond/jetbrains-window-accent/compare/1.1.3...HEAD
[1.1.3]: https://github.com/alexBlakeGoudemond/jetbrains-window-accent/compare/1.1.2...1.1.3
[1.1.2]: https://github.com/alexBlakeGoudemond/jetbrains-window-accent/compare/1.1.1...1.1.2
[1.1.1]: https://github.com/alexBlakeGoudemond/jetbrains-window-accent/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/alexBlakeGoudemond/jetbrains-window-accent/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/alexBlakeGoudemond/jetbrains-window-accent/commits/1.0.0
