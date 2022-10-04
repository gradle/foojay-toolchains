# Disco Toolchains Plugin

The `org.gradle.disco-toolchains` plugin is a [Java toolchain resolver plugin for Gradle](https://docs.gradle.org/current/userguide/toolchain_plugins.html#toolchain_plugins).
It provides download URLs for JVMs and it's based on the [foojay DiscoAPI](https://github.com/foojayio/discoapi).

# Matching Toolchain Specifications

The main thing the plugin does is to match [Gradle's toolchain specifications](https://docs.gradle.org/current/javadoc/org/gradle/jvm/toolchain/JavaToolchainSpec.html) to foojay DiscoAPI distributions and packages. 

## Vendors

There is mostly a 1-to-1 relationship between the DiscoAPI's distributions and Gradle vendors.
The plugin works with the following mapping:

| Vendor                  | Distribution   |
|-------------------------|----------------|
| \<no vendor specified\> | Temurin        |
| ADOPTIUM                | Temurin        |
| ADOPTONEJDK             | AOJ            |
| AMAZON                  | Corretto       |
| APPLE                   | -              |
| AZUL                    | Zulu           |
| BELLSOFT                | Liberica       |
| GRAAL_VM                | -              |
| HEWLETT_PACKARD         | -              |
| IBM                     | -              |
| IBM_SEMERU              | Semeru         |
| MICROSOFT               | Microsoft      |
| ORACLE                  | Oracle OpenJDK |
| SAP                     | SAP Machine    |

To note:

* If no vendor is specified, then the `Temurin` distribution is picked (due to the history of the auto-provisioning feature in Gradle, specifically that AdoptOpenJDK/Adoptium have been the default sources for downloading JVMs).
* Not all Gradle vendors have an equivalent DiscoAPI distribution.
* GraalVM distributions just don't seem to work in the current version of the DiscoAPI, so this version of the plugin doesn't map to them.

## Implementations

When specifying toolchains Gradle distinguishes between `J9` JVMs and `VENDOR_SPECIFIC` ones (ie. any other).
What this criteria does in the plugin is to influence the Vendor-to-Distribution matching table.
`VENDOR_SPECIFICATION` doesn't change it at all, while `J9` alter it like this:

| Vendor                  | Distribution |
|-------------------------|--------------|
| \<no vendor specified\> | Semeru       |
| ADOPTIUM                | -            |
| ADOPTONEJDK             | AOJ OpenJ9   |
| AMAZON                  | -            |
| APPLE                   | -            |
| AZUL                    | -            |
| BELLSOFT                | -            |
| GRAAL_VM                | -            |
| HEWLETT_PACKARD         | -            |
| IBM                     | -            |
| IBM_SEMERU              | Semeru       |
| MICROSOFT               | -            |
| ORACLE                  | -            |
| SAP                     | -            |

## Versions

Once the vendor and the implementation values of the toolchain spec have been used to select a DiscoAPI distribution, a specific package of that distribution needs to be picked by the plugin, in order for it to obtain a download link. 
The inputs it uses to do this are:
* the major **Java version** number for the spec
* the **operating system** running the build that made the request
* the **CPU architecture** of the system running the build that made the request

Additional criteria used for selection:
* for each major version number only packages having the latest minor version will be considered 
* only packages containing an archive of a format known to Gradle will be considered (zip, tar, tgz)
* JDKs have priority over JREs