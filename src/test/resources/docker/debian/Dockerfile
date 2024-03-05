FROM debian
LABEL authors="Michael Amiethyst"

# test with
# docker build -t "debian-bashpile" .
# docker run --rm -it debian-bashpile


# Setup system and user
RUN apt update && apt upgrade && apt-get -y install curl git build-essential procps
RUN useradd -ms /bin/bash dockeruserech
# install brew
RUN /bin/bash -c "NONINTERACTIVE=1 $(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
RUN (echo; echo 'eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"') >> /home/dockeruser/.bash_profile
RUN (echo; echo 'eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"') >> /home/dockeruser/.bashrc
RUN chown -R dockeruser /home/linuxbrew/.linuxbrew
USER dockeruser
WORKDIR /home/dockeruser
RUN eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"

# install Bashpile
RUN /bin/bash -c "eval \"$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)\" && brew install gcc"
# TODO uncomment for full install of bashpile
#RUN /bin/bash -c "eval \"$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)\" && export PATH=/home/linuxbrew/.linuxbrew/bin:$PATH && brew install gcc michael-amiethyst/bashpile/bashpile"
ENTRYPOINT ["/bin/bash"]