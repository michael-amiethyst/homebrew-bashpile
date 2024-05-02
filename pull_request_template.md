## Describe your changes

## Issue ticket number and link

## Merge to Development - Checklist before requesting a review
- [ ] I have performed a self-review of my code.  It runs with `mvn clean verify`
- [ ] I have added thorough tests.

## Merge to Main - Checklist before requesting a review
- [ ] I have installed this version locally with `bin/deploy-head`
- [ ] I have tested on Debian with `docker build -t "debian-bashpile" src/test/resources/docker/debian`
- [ ] I have regenerated the docutils documentation with `bin/generate-docs`