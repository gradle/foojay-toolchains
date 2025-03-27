# Changelog

## [Unreleased]

### Added

- Support the nativeImageCapable criteria from JavaToolchainSpec in Gradle 8.14+

### Changed

### Deprecated

### Removed

### Fixed

### Security

## [0.9.0] - 2024-11-26

### Added

- Provide an x86-64 based Java version when ARM64 is requested from macOS and none is available

## [0.8.0] - 2024-01-12

### Changed

- Bump Gson dependency version

### Fixed

- Fix issues related to Java version parameter

## [0.7.0] - 2023-08-17

### Added

- Provide meaningful error message when plugin is applied in a build script (instead of the settings script)

## [0.6.0] - 2023-07-10

### Added

- Add support for new GraalVM Community distributions

## [0.5.0] - 2023-04-24

### Added

- Generate useful error message if used with unsupported Gradle versions

## [0.4.0] - 2022-12-22

### Added

- Try distributions one-by-one if no vendor is specified

## [0.3.0] - 2022-12-22

### Fixed

- Make sure vendors IBM and IBM_SEMERU are handled identically

## [0.2] - 2022-11-29

### Added

- Make the plugin compatible with Java 8

## [0.1] - 2022-11-28

Toolchains resolver using the Foojay Disco API for resolving Java runtimes. Automatically configures toolchain management.



[Unreleased]: https://github.com/gradle/foojay-toolchains/compare/foojay-toolchains-plugin-0.9.0...HEAD
[0.9.0]: https://github.com/gradle/foojay-toolchains/releases/tag/foojay-toolchains-plugin-0.9.0
[0.8.0]: https://github.com/gradle/foojay-toolchains/releases/tag/foojay-toolchains-plugin-0.8.0
[0.7.0]: https://github.com/gradle/foojay-toolchains/releases/tag/foojay-toolchains-plugin-0.7.0
[0.6.0]: https://github.com/gradle/foojay-toolchains/releases/tag/foojay-toolchains-plugin-0.6.0
[0.5.0]: https://github.com/gradle/foojay-toolchains/releases/tag/foojay-toolchains-plugin-0.5.0
[0.4.0]: https://github.com/gradle/foojay-toolchains/releases/tag/foojay-toolchains-plugin-0.4.0
[0.3.0]: https://github.com/gradle/foojay-toolchains/releases/tag/foojay-toolchains-plugin-0.3.0
[0.2]: https://github.com/gradle/foojay-toolchains/releases/tag/foojay-toolchain-plugin-0.2
[0.1]: https://github.com/gradle/foojay-toolchains/releases/tag/foojay-toolchain-plugin-0.1
