# bashpile
The Bash transpiler - Write in a modern language and run in any Bash shell

## Setup

### Linux / Unix / OSX

Standard setup.

### Windows with Windows Subsystem for Linux (WSL)

#### Enable Hyper-V

If you're on Windows Home edition you need to enable Hyper-V.  Other versions have it installed by default.
Ensure virtualization is enabled in your BIOS (see their docs on how to do that).  Use 
[these instructions](https://www.makeuseof.com/install-hyper-v-windows-11-home/) to enable it on the OS level.

#### Install from Admin Command Prompt

With Hyper-V enabled the [regular instructions](https://learn.microsoft.com/en-us/windows/wsl/install) should work fine.
At this point you should be able to open "\\wsl$" or "\\wsl.localhost" and your distro in windows to get access to the
WSL file system.  At your favorite command prompt in the Linux shell update your OS packages.  In Ubuntu it's 
`sudo apt update && sudo apt upgrade`.  Then we can install Homebrew.  The site has all of these details for Linux 
distros but you just install the script as normal (besides some extra post-install steps) and it handles the details.

We'll 'git clone' this code base there and open with our IDE.

## Legal

This codebase is a complete re-write of a similar project at eBay.  Their legal team passed on a patent so I can re-implement the idea.  The original code however, is theirs.
