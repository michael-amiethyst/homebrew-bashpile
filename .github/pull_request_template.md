## Describe your changes
[Link closed bugs with closing keywords]

## Merge to Development - Checklist before requesting a review
- [ ] I have performed a self-review of my code.  
- [ ] It runs with `mvn clean verify`
- [ ] I have installed this version locally with `bin/deploy-head`
- [ ] I have added thorough tests.

## Merge to Main - Checklist before requesting a review
- [ ] I have installed this version locally with `bin/deploy-head`
- [ ] I have tested on Debian with `docker build --no-cache -t "debian-bashpile" src/test/resources/docker/debian`
- [ ] I have regenerated the docutils documentation with `bin/generate-docs`