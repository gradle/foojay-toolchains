# Foojay Toolchains Plugin (FTP) Release Process

## 1. Bump the plugin version number

This should be the next version.
Verify that it's higher than the version reported [in the portal](https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver).

This lives in [`foojay-resolver/gradle.properties`](https://github.com/gradle/foojay-toolchains/blob/main/foojay-resolver/gradle.properties).


## 2. Add some release notes

You'll need to add a plain text file with name [`foojay-resolver/release-notes-${plugin-version}.txt`](https://github.com/gradle/foojay-toolchains/tree/main/foojay-resolver).
Whatever you put here is what will appear in the portal for that version.
E.g. for [0.2](https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver/0.2) it was "- Make the plugin compatible with Java 8".

*NOTE:* Only include new changes relative to the previously published version.

Remove any old release notes files.


## 3. Update the CHANGELOG.md file

On one hand it needs some content describing what is in the new release, can be the same as the release notes above.
On the other hand it needs an entry towards the end of the file to specify which tag belongs to this new release. 
See previous content for reference.


## 4. Deploy to Dev Portal

Use [this job](https://builds.gradle.org/buildConfiguration/Dotcom_PluginsPortal_DeployFoojayToolchainsPluginDevelopment?branch=%3Cdefault%3E&buildTypeTab=overview&mode=builds#all-projects) to deploy the plugin to the dev portal.

Make sure it [looks ok](https://plugins.grdev.net/plugin/org.gradle.toolchains.foojay-resolver) in the portal and the release notes are formatted as you want them to be.
Do the same check for the [convention version](https://plugins.grdev.net/plugin/org.gradle.toolchains.foojay-resolver-convention) too.

If you need to make changes, you'll need to delete the published version from the portal before you try again.
Just use the big red "Delete version X" button of the [portal page](https://plugins.grdev.net/plugin/org.gradle.toolchains.foojay-resolver).
Don't forget about the [convention version](https://plugins.grdev.net/plugin/org.gradle.toolchains.foojay-resolver-convention) either.


## 5. Deploy to Prod Portal

Use [this job](https://builds.gradle.org/buildConfiguration/Dotcom_PluginsPortal_DeployFoojayToolchainsPluginProduction?branch=%3Cdefault%3E&buildTypeTab=overview&mode=builds#all-projects) to deploy the plugin to the production portal.

Make sure it [looks ok](https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver) in the portal and the release notes are formatted as you want them to be.
Check the [convention version](https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention) too.


## 6. Tag the release

We don't have any automated tagging, but please manually tag the current master and push it in, e.g.:

    git tag foojay-toolchains-plugin-1.0
    git push origin main --tags


## 7. Notify anyone waiting for a fix


## 8. Bump the development version of the plugin

- Increment the version in [`foojay-resolver/gradle.properties`](https://github.com/gradle/foojay-toolchains/blob/main/foojay-resolver/gradle.properties).
- Update all usages of the previous version in [`the project`](https://github.com/gradle/foojay-toolchains/)
