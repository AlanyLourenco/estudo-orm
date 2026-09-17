#!/usr/bin/env sh
# Compila e abre a demonstração de ORM (Matrícula escolar) no Linux/macOS.
# Uso:  ./executar.sh           abre a janela
#       ./executar.sh --teste   roda o roteiro no console, usando H2
cd "$(dirname "$0")" || exit 1

rm -rf out
mkdir -p out
javac -encoding UTF-8 -cp "lib/*" -d out \
    src/*.java src/modelo/*.java src/persistencia/*.java src/negocio/*.java src/visao/*.java || exit 1
cp -r src/META-INF out/

java -Dfile.encoding=UTF-8 -cp "out:lib/*" Principal "$@"
