# Windows Home Setup

This guide is for Intellij Ultimate, WSL 2.0 and Ubuntu.

## Enable Hyper-V

You need to enable Hyper-V for Windows Home.  Ensure virtualization is enabled in your BIOS (see their docs on how to do that).  Use
[these instructions](https://www.makeuseof.com/install-hyper-v-windows-11-home/) to enable it on the OS level.

## Install WSL

For setting up WSL initially the [regular instructions](https://learn.microsoft.com/en-us/windows/wsl/install) should work fine.
At this point you should be able to open `\\wsl$` or `\\wsl.localhost\Ubuntu` in windows to get access to the
WSL file system.  After the WSL install you can just use the `wsl` command at the command prompt to get into your Linux
shell.  Be sure to update your OS packages.  In Ubuntu it's `sudo apt update && sudo apt upgrade`.

## Setup Ubuntu

Now we can install Homebrew.  The homebrew site has all of these details for Linux distros but you just
install the script as normal (besides a couple extra post-install steps) and it handles the details.

Do a 'brew install java' and 'brew install mvn'.

We'll 'git clone' this code base into the Windows filesystem (e.g. `C:\Users\username\dev\Bashpile`).

## Setup Intellij

Open Intellij and our project in the Windows (not a subdirectory of `\\wsl$`) filesystem.  You should get a popup to add exclusions for a couple of development
directories.  When you have a Maven command or run something you want to use a (run target)[https://www.jetbrains.com/help/idea/how-to-use-wsl-development-environment-in-product.html#local_project].
Also enable environment variables and set your JAVA_HOME to the Linux location (something like `JAVA_HOME=/home/linuxbrew/.linuxbrew/Cellar/openjdk/19.0.2/libexec`).