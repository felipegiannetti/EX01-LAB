#!/usr/bin/env bash
# Compila e executa, em sequencia, todas as partes do roteiro (A-E) e os
# extras (ciclo de vida e comparativo). Util para conferir tudo de uma vez
# antes da entrega.
set -e
cd "$(dirname "$0")/.."

./scripts/compilar.sh

executar() {
    echo
    echo "=== $1 ==="
    java -cp out "$1" "${@:2}"
}

executar parteA.Main
executar parteB.Main
executar parteC.Main
executar parteD.Main
executar parteD.MainCached
executar parteE.Main
executar extra.CicloDeVidaDemo
executar extra.Comparativo
