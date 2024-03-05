FROM debian
LABEL authors="Michael Amiethyst"

# test with while Docker Desktop is running in the background
# docker build -t "debian-bashpile" .
# or
# docker build -t "debian-bashpile" src/test/resources/docker/debian
# docker run --rm -it debian-bashpile


# Setup system and user
RUN apt update && apt upgrade && apt-get -y install curl git build-essential procps
RUN useradd -ms /bin/bash dockeruser

# install brew
RUN /bin/bash -c "NONINTERACTIVE=1 $(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# install Bashpile
RUN /bin/bash -c "eval \"$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)\" && brew install gcc michael-amiethyst/bashpile/bashpile"

# finish setting up user and start
RUN chown -R dockeruser /home/linuxbrew/.linuxbrew
USER dockeruser
WORKDIR /home/dockeruser
ENTRYPOINT ["/bin/bash"]