# Windows Pro Setup

This guide covers working with Windows Pro, Intellij (Community only), WSL 2.0 and the default Ubuntu distro.  Debugging with this
method has not been tested.  For full debug with WSL see the [WindowsHomeSetup](IntellijUltimate.md) docs if you're
using Intellij Pro.

With the Windows Pro setup we are going to have our development directory directly in the WSL file space.

## Install WSL

For setting up WSL initially the [regular instructions](https://learn.microsoft.com/en-us/windows/wsl/install) should work fine.
At this point you should be able to open "\\wsl$" or "\\wsl.localhost\Ubuntu" in windows to get access to the
WSL file system.  After the WSL install you can just use the `wsl` command at the command prompt to get into your Linux
shell.  Be sure to update your OS packages.  In Ubuntu it's `sudo apt update && sudo apt upgrade`.  

## Setup Ubuntu

Now we can install Homebrew.  The homebrew site has all of these details for Linux distros but you just 
install the script as normal (besides a couple extra post-install steps) and it handles the details.

Do a 'brew install java' and 'brew install mvn'.

We'll 'git clone' this code base into the Ubuntu filesystem.  Intellij doesn't play well with the Linux filesystem 
(issues with Virtualization Based Security and Windows Defender
[Credential Guard](https://learn.microsoft.com/en-us/windows/security/identity-protection/credential-guard/credential-guard-manage))
but we have some workarounds.

## Setup Intellij

Open Intellij and our project in the WSL filesystem.  You should get a popup to add exclusions for a couple of development
directories.  You'll also want to add the whole Linux directory as an exclusion as well.  In Windows 11 you go to Windows Security -> Virus &
Threat Protection -> Virus & Threat Protection Settings -> Manage Settings -> Exclusions -> Add or Remove Exclusions ->
Add an Exclusion -> Folder, and finally you can enter `\\wsl$\Ubuntu` as an exclusion.

In the Intellij Maven settings you can point to the WSL Maven at
`\\wsl$\Ubuntu\home\linuxbrew\.linuxbrew\Cellar\maven\<version>\libexec`.  Now you should be able to run commands from
the Maven tool window using wsl maven.  Run `mvn build` now to generate some source files in the project's `\target`
directory.  Mark your directories as sources, resources and generated sources.  In particular `\src\main\antlr4` as a
sources root, `\target\generated-sources\annotations` and `\target\generated-sources\antlr4` as generated sources roots
and `\target\generated-test-sources\test-annotations` as a generated sources root.  Make sure the sub directories are
NOT marked as excluded.