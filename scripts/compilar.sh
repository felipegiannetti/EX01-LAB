#!/usr/bin/env bash
# Compila todo o codigo-fonte em ./out
set -e
cd "$(dirname "$0")/.."
rm -rf out
mkdir out
javac -d out $(find src -name "*.java")
echo "Compilado com sucesso em ./out"
