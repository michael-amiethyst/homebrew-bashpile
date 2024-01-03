# Temporary notes - to go to wiki

## Setup FileTypes - Intellij
Java syntax highlighting for now?

## Setup Run Configuration - Intellij
`clean verify -f pom.xml` on WSL
Maven Home on WSL
Java JRE on WSL, Modify Settings to get Environment variables, set JAVA_HOME=Linux style

## Deploy with Brew

Just run bin/deploy to redeploy from the release.

Install local formula with `brew unlink bashpile; brew install Formula/bashpile.rb`.

Fresh pull: `brew cleanup --prune=all && brew reinstall Formula/bashpile.rb`
Audit: `brew audit --new bashpile`

Interactive: `brew install --interactive Formula/homebrew-bashpile.rb`
Debug: `HOMEBREW_NO_INSTALL_FROM_API=1 brew install --build-from-source --verbose --debug --HEAD Formula/homebrew-bashpile.rb`