FROM debian
LABEL authors="Michael Amiethyst"

# test with while Docker Desktop is running in the background
# docker build -t "debian-bashpile" src/test/resources/docker/debian
# or
# docker build --progress=plain --no-cache -t "debian-bashpile" src/test/resources/docker/debian
# docker run --rm -it debian-bashpile


# Setup system and user
RUN apt update && apt upgrade && apt-get -y install curl git build-essential procps
RUN useradd -ms /bin/bash dockeruser
RUN mkdir /home/linuxbrew
RUN chown -R dockeruser /home/linuxbrew
USER dockeruser
WORKDIR /home/dockeruser

# install brew
RUN /bin/bash -c "NONINTERACTIVE=1 $(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# install gcc, Bashpile dependencies and Bashpile
RUN /bin/bash -c "eval \"$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)\" \
    && brew install gcc openjdk bash maven shfmt shellcheck bc gnu-getopt gnu-sed" \
# shfmt not in PATH for embedded Shell
RUN /bin/bash -c "eval \"$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)\" && export PATH=/home/linuxbrew/.linuxbrew/bin:$PATH \
    && brew install --HEAD michael-amiethyst/bashpile/bashpile || cat /home/dockeruser/.cache/Homebrew/Logs/bashpile/01.mvn"
RUN /bin/bash -c "eval \"$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)\" && bpr --help"

ENTRYPOINT ["/bin/bash"]