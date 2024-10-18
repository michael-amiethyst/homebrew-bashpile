FROM debian
LABEL authors="Michael Amiethyst"

# test with while Docker Desktop is running in the background
# docker build -t "michaelamiethystbashpile/debian-safe" src/test/resources/docker/debian/safe
# or
# docker build --progress=plain --no-cache -t "michaelamiethystbashpile/debian-safe" src/test/resources/docker/debian/safe
# docker run --rm -it michaelamiethystbashpile/debian-safe


# Setup system and user
RUN apt update && apt upgrade && apt-get -y install curl git build-essential procps
# skip user-add for simple and safe mode

# install brew
RUN /bin/bash -c "NONINTERACTIVE=1 $(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# install Bashpile
RUN /bin/bash -c "eval \"$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)\" \
    && brew install gcc && brew install --HEAD michael-amiethyst/bashpile/bashpile || \
    cat /root/.cache/Homebrew/Logs/bashpile/01.mvn"

ENTRYPOINT ["/bin/bash"]