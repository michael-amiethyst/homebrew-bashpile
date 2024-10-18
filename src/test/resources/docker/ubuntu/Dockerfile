FROM ubuntu:24.10
LABEL authors="Michael Amiethyst"

# test with while Docker Desktop is running in the background
# docker build -t "michaelamiethystbashpile/bashpile" src/test/resources/docker/ubuntu
# or
# docker build --progress=plain --no-cache -t "michaelamiethystbashpile/bashpile" src/test/resources/docker/ubuntu
# docker run --rm -it michaelamiethystbashpile/bashpile

# Use bash as default shell for RUN commands
SHELL ["/bin/bash", "-c"]

# Setup system and user
RUN apt update -y && apt upgrade -y && apt-get -y install curl git build-essential procps
RUN useradd -ms /bin/bash dockeruser
RUN mkdir /home/linuxbrew
RUN chown -R dockeruser /home/linuxbrew
USER dockeruser
WORKDIR /home/dockeruser

# install brew, don't update .bashrc or .bash_profile (not picked up by subsequent RUN commands)
RUN /bin/bash -c "NONINTERACTIVE=1 $(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# install gcc, Bashpile dependencies and Bashpile
RUN eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)" \
    && brew install gcc openjdk bash maven shfmt shellcheck bc gnu-getopt gnu-sed
RUN eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)" \
    && brew install --HEAD michael-amiethyst/bashpile/bashpile

ENTRYPOINT ["/bin/bash"]